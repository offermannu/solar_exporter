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
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.atrium.solarlogger.impl.KakoTotalYieldTelegramPayload.AC_DAILY_PEAK;
import static de.atrium.solarlogger.impl.KakoTotalYieldTelegramPayload.DAILY_YIELD;
import static de.atrium.solarlogger.impl.KakoTotalYieldTelegramPayload.DAILY_YIELD_HOURS;
import static de.atrium.solarlogger.impl.KakoTotalYieldTelegramPayload.TOTAL_YIELD;
import static de.atrium.solarlogger.impl.KakoTotalYieldTelegramPayload.TOTAL_YIELD_HOURS;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class TotalYieldMetricsExporter implements MetricsExporter {

    private static final Logger LOGGER = Logger.getLogger(TotalYieldMetricsExporter.class.getName());

    public static final  String        HOME         = System.getProperty("user.home");
    private static final Path          YIELD_LOG    = Path.of(HOME, "ertrag.log");
    private static final MessageFormat YIELD_FORMAT = new MessageFormat(
            "{0}.{1}.{2} {3} {4,number,0.000} {5,number,0.000}\n",
            Locale.GERMAN
    ); // [date] [addr] [yield] [accumulated yield]

    public TotalYieldMetricsExporter() {
        LOGGER.info("Daily yield will be written into " + YIELD_LOG);
    }

    @Override
    public void exportMetrics(KakoTelegram telegram) {
        String address = telegram.getAddress();

        LocalDateTime now = LocalDateTime.now();
        if (now.getHour() == 0) {
            // after midnight
            now = now.minusDays(1);
        }

        String day   = String.format("%02d", now.getDayOfMonth());
        String month = String.format("%02d", now.getMonth().getValue());
        String year  = String.format("%04d", now.getYear());

        if (DAILY_YIELD.getValue(telegram) == 0) {
            // End of the day reached - record daily gain

            // get the daily gain from the gauge and publish it as once-per-day sample
            double dailyGain = METRIC_DAILY_GAIN.labelValues(address, month, year).get();
            METRIC_DAY_ENERGIE.labelValues(address, day, month, year).set(dailyGain);

            // log daily gain into file
            // format is [DATE] [INVERTER-ADDR] [DAILY-GAIN] [TOTAL_YIELD]
            try {
                Files.writeString(
                        YIELD_LOG,
                        YIELD_FORMAT.format(new Object[] {
                                day, month, year,
                                address,
                                dailyGain,
                                TOTAL_YIELD.getValue(telegram)
                        }),
                        CREATE, APPEND
                );
                LOGGER.info("Logged daily gain into " + YIELD_LOG);

            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to log daily yield into log file " + YIELD_LOG, e);
            }

        } else {
            // make sure there is only one sample > 0 per day!
            METRIC_DAY_ENERGIE.labelValues(address, day, month, year).set(0);
        }

        METRIC_AC_DAILY_PEAK.labelValues(address).set(AC_DAILY_PEAK.getValue(telegram));
        METRIC_ACCUMULATED_YIELD.labelValues(address).set(TOTAL_YIELD.getValue(telegram));
        METRIC_DAILY_YIELD_HOURS.labelValues(address).set(DAILY_YIELD_HOURS.getHours(telegram));
        METRIC_ACCUMULATED_YIELD_HOURS.labelValues(address).set(TOTAL_YIELD_HOURS.getHours(telegram));
        METRIC_DAILY_GAIN.labelValues(address, month, year).set(DAILY_YIELD.getValue(telegram));
    }
}
