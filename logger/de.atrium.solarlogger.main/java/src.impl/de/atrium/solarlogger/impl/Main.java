/*
 * Copyright (c) Papierfabrik Louisenthal GmbH
 *
 * All rights reserved.
 */
package de.atrium.solarlogger.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.prometheus.metrics.core.metrics.CounterWithCallback;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    /* Prometheus Export Port */
    private static final int EXPORTER_PORT = 9494;

    public static void main(String[] args) throws Exception {

        CounterWithCallback.builder()
                           .name("solarlogger_scrapes")
                           .callback(callback ->
                                   // Schedule the next kako status request in 55 seconds so that fresh data is available
                                   // when the next scrape request from Prometheus arrives.
                                   new RunAt(
                                           Instant.now().plusSeconds(55),
                                           Main::requestKakoStatus
                                   )
                           )
                           .register();

        try (HTTPServer ignored = HTTPServer.builder()
                                            .port(EXPORTER_PORT)
                                            .buildAndStart()) {

            LOGGER.info("HTTP-Server started and listens on port " + EXPORTER_PORT);

            // request first status
            requestKakoStatus();

            // let user main thread fall asleep
            Thread.currentThread().join();
        }
    }

    private static void requestKakoStatus() {

        Instant tStart = Instant.now();
        int     n      = 0;

        for (KakoDevice device : KakoDevice.values()) {
            LOGGER.info("Requesting Kako Metrics for Device " + device);
            try (KakoConnection connection = new KakoConnection(device)) {

                if (device.supports(KakoCommand.STATUS)) {
                    KakoTelegram statusTelegram = connection.query(KakoCommand.STATUS);
                    LOGGER.info("Received Status " + statusTelegram);
                    statusTelegram.getTelegramType()
                                  .getMetricsExporter()
                                  .exportMetrics(statusTelegram);
                    n++;
                }

                if (device.supports(KakoCommand.TOTAL_YIELD)) {
                    KakoTelegram totalYieldTelegram = connection.query(KakoCommand.TOTAL_YIELD);
                    LOGGER.info("Received Total Yield " + totalYieldTelegram);
                    totalYieldTelegram.getTelegramType()
                                      .getMetricsExporter()
                                      .exportMetrics(totalYieldTelegram);
                    n++;
                }

            } catch (Exception e) {
                CommonPrometheusCounters.countError();
                LOGGER.log(Level.WARNING, "Failed status request for device " + device + " is ignored", e);
            }
        }
        LOGGER.info("Processing of " + n + " Kako status telegrams took " + Duration.between(tStart, Instant.now()).toMillis() + " msec.");
    }
}
