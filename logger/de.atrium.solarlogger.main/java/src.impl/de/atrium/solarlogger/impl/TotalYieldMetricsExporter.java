/*
 * Copyright (c) Papierfabrik Louisenthal GmbH
 *
 * All rights reserved.
 */
package de.atrium.solarlogger.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.prometheus.metrics.core.metrics.GaugeWithCallback;

import static de.atrium.solarlogger.impl.KakoTotalYieldTelegramPayload.AC_DAILY_PEAK;
import static de.atrium.solarlogger.impl.KakoTotalYieldTelegramPayload.DAILY_YIELD;
import static de.atrium.solarlogger.impl.KakoTotalYieldTelegramPayload.DAILY_YIELD_HOURS;
import static de.atrium.solarlogger.impl.KakoTotalYieldTelegramPayload.TOTAL_YIELD;
import static de.atrium.solarlogger.impl.KakoTotalYieldTelegramPayload.TOTAL_YIELD_HOURS;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class TotalYieldMetricsExporter implements MetricsExporter, Consumer<GaugeWithCallback.Callback> {

    private static final Logger LOGGER = Logger.getLogger(TotalYieldMetricsExporter.class.getName());

    public static final  String        HOME         = System.getProperty("user.home");
    private static final Path          YIELD_LOG    = Path.of(HOME, "data/ertrag.log");

    // log file format: [year].[month].[day] [addr] [yield]
    private static final MessageFormat YIELD_FORMAT = new MessageFormat("{0}-{1}-{2} {3} {4,number,#}\n");

    public static final TotalYieldMetricsExporter INSTANCE = new TotalYieldMetricsExporter();

    private final SortedMap<String, Double> dailyYieldMap = new TreeMap<>();

    private LocalDate yesterday = LocalDate.now();

    private TotalYieldMetricsExporter() {
        LOGGER.info("Daily yield will be written into " + YIELD_LOG);
    }

    @Override
    public void exportMetrics(KakoTelegram telegram) {
        String address = telegram.getAddress();

        // Keep the daily yield - the value can only grow until it is reset
        double dailyYield = DAILY_YIELD.getValue(telegram);
        dailyYieldMap.merge(address, dailyYield, Math::max);

        METRIC_AC_DAILY_PEAK.labelValues(address).set(AC_DAILY_PEAK.getValue(telegram));
        METRIC_ACCUMULATED_YIELD.labelValues(address).set(TOTAL_YIELD.getValue(telegram));
        METRIC_DAILY_YIELD_HOURS.labelValues(address).set(DAILY_YIELD_HOURS.getHours(telegram));
        METRIC_ACCUMULATED_YIELD_HOURS.labelValues(address).set(TOTAL_YIELD_HOURS.getHours(telegram));

        LocalDate today = LocalDate.now();
        String month = String.format("%02d", today.getMonth().getValue());
        String year  = String.format("%04d", today.getYear());

        METRIC_DAILY_GAIN.labelValues(address, month, year).set(dailyYield);

        // save to Postgres
        new DailyYieldDAO().save(LocalDate.now(), Integer.parseInt(address), dailyYield);
    }

    @Override
    public void accept(GaugeWithCallback.Callback callback) {
        LocalDate now = LocalDate.now();
        if (now.isAfter(yesterday)) {
            // after midnight - publish daily yield

            String day   = String.format("%02d", yesterday.getDayOfMonth());
            String month = String.format("%02d", yesterday.getMonth().getValue());
            String year  = String.format("%04d", yesterday.getYear());

            dailyYieldMap.forEach((address, yield) -> {
                // expose daily yield to Prometheus
                callback.call(yield, address, day, month, year);

                // log daily gain into file
                // format is [YYYY.MM.DD] [INVERTER-ADDR] [DAILY-GAIN]
                try {
                    Files.writeString(
                            YIELD_LOG,
                            YIELD_FORMAT.format(new Object[] {
                                    year, month, day,
                                    address,
                                    yield
                            }),
                            CREATE, APPEND
                    );
                    LOGGER.info("Logged daily gain into " + YIELD_LOG);

                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to log daily yield into log file " + YIELD_LOG, e);
                }

                // reset for next day
                dailyYieldMap.put(address, 0.0);
            });

            yesterday = now;
        }

    }
}
