/*
 * Copyright (c) Papierfabrik Louisenthal GmbH
 *
 * All rights reserved.
 */
package de.atrium.solarlogger.impl;

import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

import static com.zfabrik.util.html.Escaper.escapeToJS;
import static de.atrium.solarlogger.impl.SscEventType.RXCHAR;

/**
 * Connection abstraction for a specific {@link KakoDevice}
 * The connection allows to {@link #query(KakoCommand)} a series of {@link KakoCommand}s to the device.
 * The {@link #query(KakoCommand)} method blocks until the response telegram is received.
 */
public class KakoConnection implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(KakoConnection.class.getName());

    private static final int TIMEOUT_MSEC = 2000;

    private final KakoDevice kakoDevice;
    private boolean isOpen;

    public KakoConnection(KakoDevice kakoDevice) {
        this.kakoDevice = kakoDevice;
        this.isOpen = true;
    }

    @Override
    public void close() {
        this.isOpen = false;
    }

    /**
     * Sends the given command to the Kako device and waits for the response
     *
     * @param command a {@link KakoCommand}
     * @return a {@link KakoTelegram}
     * @throws IOException in case of communication problems, timeout or when the response could be parsed
     */
    public KakoTelegram query(KakoCommand command) throws IOException {
        if (!this.isOpen) {
            throw new IOException("Kako-Connection already closed");
        }

        Instant tStart = Instant.now();

        String queryString = command.createFor(kakoDevice);
        try (KakoRS485Port port = new KakoRS485Port()) {
            return port.query(queryString);

        } catch (ExecutionException e) {
            close();
            throw new IOException("Query '" + queryString + "' failed with exception", e.getCause());

        } catch (InterruptedException e) {
            close();
            throw new IOException("Query '" + queryString + "' was interrupted");

        } catch (TimeoutException e) {
            close();
            throw new IOException("Query '" + queryString + "' timed out after " + TIMEOUT_MSEC + "msec");

        } catch (SerialPortException e) {
            close();
            throw new IOException("RS485 communication problem", e);

        } finally {
            // log duration in Prometheus
            MetricsExporter.countSSCDuration(
                    kakoDevice.getAddress(),
                    Duration.between(tStart, Instant.now())
            );
        }
    }

    private static class KakoRS485Port extends SerialPort implements SerialPortEventListener, AutoCloseable {

        private static final String KAKO_PORT = "/dev/ttyUSB0";

        private final StringBuilder telegram = new StringBuilder();

        private final CompletableFuture<KakoTelegram> response = new CompletableFuture<>();

        /**
         * Construct the RS485 port to /dev/ttyUSB0
         */
        KakoRS485Port() throws SerialPortException {
            super(KAKO_PORT);

            openPort();

            // RS485 Connection settings
            setParams(BAUDRATE_9600, DATABITS_8, STOPBITS_1, PARITY_NONE);
            addEventListener(this);
        }

        /**
         * Sends the given command to the port and waits for the response
         */
        KakoTelegram query(String command) throws SerialPortException, ExecutionException, InterruptedException, TimeoutException {
            writeString(command);
            return response.get(TIMEOUT_MSEC, TimeUnit.MILLISECONDS);
        }

        /**
         * This class receives the response from the Kako devices -
         * it's implemented as a JSSC Callback which waits for the RXCHAR events containing the telegram.
         *
         * @param event <code>SerialPortEvent</code> object containing port, event type and event data
         */
        @Override
        public void serialEvent(SerialPortEvent event) {
            try {
                SscEventType sscEventType = SscEventType.of(event.getEventType());
                MetricsExporter.countSSCEvent(sscEventType.name());

                if (sscEventType == RXCHAR) {
                    // this is the standard Kako RS485 event type
                    LOGGER.fine("Received event " + sscEventType + "(" + event.getEventValue() + ")");

                    String chunk = read(event);
                    if (chunk.charAt(0) == '\n') {
                        // start of telegram
                        telegram.setLength(0);
                    }
                    telegram.append(chunk);

                    // A datagram can consist of several event chunks and ends with '\r'
                    int length = telegram.length();
                    if (length > 0 && telegram.charAt(length - 1) == '\r') {
                        LOGGER.fine("Telegram reception completed");

                        // notify success
                        response.complete(new KakoTelegramParser(telegram.toString()).parse());
                    }

                } else {
                    // We do not expect any events other than RXCHAR;
                    // if this does happen, we simply empty the buffer and ignore the event.
                    LOGGER.warning("Received unexpected event " + sscEventType + "(" + event.getEventValue() + ")");
                    read(event);
                    response.completeExceptionally(
                            new SerialPortException(
                                    event.getPort(),
                                    "Receive Event", "Unsupported Event " + sscEventType
                            )
                    );
                }

            } catch (SerialPortTimeoutException | SerialPortException e) {
                // log the incident and return the exception to the caller
                LOGGER.severe("Exception occurred while receiving serial port event: " + e);
                response.completeExceptionally(e);

            } catch (ParseException e) {
                LOGGER.severe("Parser failed for Kako response telegram '" + escapeToJS(telegram.toString()) + "' at " + e.getErrorOffset());
                response.completeExceptionally(e);
            }
        }

        @Override
        public void close() throws SerialPortException {
            closePort();
        }

        private String read(SerialPortEvent event) throws SerialPortTimeoutException, SerialPortException {
            return event.getEventValue() > 0
                   ? event.getPort().readString(event.getEventValue(), 333)
                   : "";
        }
    }
}
