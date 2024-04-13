/*
 * Copyright (c) Papierfabrik Louisenthal GmbH
 *
 * All rights reserved.
 */
package de.atrium.solarlogger.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.prometheus.metrics.core.metrics.Gauge;

import static de.atrium.solarlogger.impl.KakoTotalYieldTelegramPayload.AC_DAILY_PEAK;
import static de.atrium.solarlogger.impl.KakoTotalYieldTelegramPayload.DAILY_YIELD;
import static de.atrium.solarlogger.impl.KakoTotalYieldTelegramPayload.DAILY_YIELD_HOURS;
import static de.atrium.solarlogger.impl.KakoTotalYieldTelegramPayload.TOTAL_YIELD;
import static de.atrium.solarlogger.impl.KakoTotalYieldTelegramPayload.TOTAL_YIELD_HOURS;
import static java.nio.file.StandardOpenOption.*;

public class TotalYieldMetricsExporter implements MetricsExporter {

    private static final Logger LOGGER = Logger.getLogger(TotalYieldMetricsExporter.class.getName());

    public static final  String        HOME         = System.getProperty("user.home");
    private static final Path          YIELD_LOG    = Path.of(HOME, "ertrag.log");
    private static final MessageFormat YIELD_FORMAT = new MessageFormat(
            "{0,date,short} {1} {2,number,0.000} {3,number,0.000}",
            Locale.GERMAN
    ); // [date] [addr] [yield] [accumulated yield]

    private static final Gauge METRIC_AC_DAILY_PEAK     =
            Gauge.builder()
                 .name("solarlogger_daily_ac_peak")
                 .labelNames("address")
                 .help("AC-daily yield (peak) [W]")
                 .register();
    private static final Gauge METRIC_ACCUMULATED_YIELD =
            Gauge.builder()
                 .name("solarlogger_accumulated_yield")
                 .help("Overall accumulated yield [Wh]")
                 .labelNames("address")
                 .register();

    private static final Gauge METRIC_DAILY_YIELD_HOURS =
            Gauge.builder()
                 .name("solarlogger_daily_yield_hours")
                 .help("daily yield hours [hours]")
                 .labelNames("address")
                 .register();

    private static final Gauge METRIC_ACCUMULATED_YIELD_HOURS =
            Gauge.builder()
                 .name("solarlogger_accumulated_yield_hours")
                 .help("Accumulated yield hours [hours]")
                 .labelNames("address")
                 .register();

    private LocalDateTime nextLogTime = LocalDate.now().atStartOfDay().plusDays(1);

    public TotalYieldMetricsExporter() {
        LOGGER.info("Daily yield will be written into " + YIELD_LOG + ". Next log is scheduled for " + nextLogTime);
    }

    @Override
    public void exportMetrics(KakoTelegram telegram) {
        String address = telegram.getAddress();

        METRIC_AC_DAILY_PEAK.labelValues(address).set(AC_DAILY_PEAK.getValue(telegram));
        METRIC_ACCUMULATED_YIELD.labelValues(address).set(TOTAL_YIELD.getValue(telegram));
        METRIC_DAILY_YIELD_HOURS.labelValues(address).set(DAILY_YIELD_HOURS.getHours(telegram));
        METRIC_ACCUMULATED_YIELD_HOURS.labelValues(address).set(TOTAL_YIELD_HOURS.getHours(telegram));

        // log the total yield once per day
        if (LocalDateTime.now().isAfter(nextLogTime)) {
            // format is [DATE] [INVERTER-ADDR] [DAILY-YIELD] [ACCUMULATED YIELD]
            try {
                Files.writeString(
                        YIELD_LOG,
                        YIELD_FORMAT.format(new Object[]{
                                new Date(),
                                address,
                                DAILY_YIELD.getValue(telegram),
                                TOTAL_YIELD.getValue(telegram)
                        }),
                        CREATE, APPEND
                );
                nextLogTime = nextLogTime.plusDays(1);
                LOGGER.info("Daily yield was logged into " + YIELD_LOG + ". Next log is scheduled for " + nextLogTime);

            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to log daily yield into log file " + YIELD_LOG, e);
            }
        }
    }
}
