package de.atrium.solarlogger.impl;/*
 * Copyright (c) Papierfabrik Louisenthal GmbH
 *
 * All rights reserved.
 */

import org.apache.commons.lang3.StringUtils;
/**
 * See chapter 3.4 page 15f in
 * APL_RS485protocol_KACO_Communication between inverter and data logger_ALL_en.pdf
 */
public enum KakoTotalYieldTelegramPayload {

    AC_DAILY_PEAK(1),
    DAILY_YIELD(2),
    TOTAL_YIELD(4),
    DAILY_YIELD_HOURS(5),
    TOTAL_YIELD_HOURS(7);

    private final int fieldNo;

    KakoTotalYieldTelegramPayload(int fieldNo) {
        this.fieldNo = fieldNo;
    }

    public double getValue(KakoTelegram telegram) {
        // Payload field enumeration start with 1
        return Double.parseDouble(telegram.getPayload(fieldNo-1));
    }

    public double getHours(KakoTelegram telegram) {
        String hours = telegram.getPayload(fieldNo - 1);
        String[] hoursAndMinutes = StringUtils.split(hours, ':');
        if (hoursAndMinutes.length == 2) {
            double h = Double.parseDouble(hoursAndMinutes[0]);
            double m = Double.parseDouble(hoursAndMinutes[1]);
            return h + m / 60;
        } else {
            throw new RuntimeException("Field " + this + "(" + fieldNo + ") is not an hour field and cannot be applied to value '" + hours + "'!");
        }
    }
}
