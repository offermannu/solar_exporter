package de.atrium.solarlogger.impl;/*
 * Copyright (c) Papierfabrik Louisenthal GmbH
 *
 * All rights reserved.
 */

/**
 *  See chapter 3.3.1 page 12f in
 *  APL_RS485protocol_KACO_Communication between inverter and data logger_ALL_en.pdf
 */
public enum KakoStatusTelegramPayload {

    MPPT_1_DC_VOLTAGE(4),
    MPPT_1_DC_CURRENT(5),
    MPPT_1_DC_POWER(6),
    MPPT_2_DC_VOLTAGE(7),
    MPPT_2_DC_CURRENT(8),
    MPPT_2_DC_POWER(9),
    PHASE_1_AC_VOLTAGE(10),
    PHASE_1_AC_CURRENT(11),
    PHASE_2_AC_VOLTAGE(12),
    PHASE_2_AC_CURRENT(13),
    PHASE_3_AC_VOLTAGE(14),
    PHASE_3_AC_CURRENT(15),
    DC_POWER_TOTAL(16),
    AC_POWER_TOTAL(17),
    COS_PHI(18),
    BOARD_TEMPERATURE(19),
    DAILY_YIELD(20);

    private final int fieldNo;

    KakoStatusTelegramPayload(int fieldNo) {
        this.fieldNo = fieldNo;
    }

    public double getValue(KakoTelegram telegram) {
        // Payload field enumeration start with 4!
        return Double.parseDouble(telegram.getPayload(fieldNo-4));
    }

    public String getStrValue(KakoTelegram telegram) {
        // Payload field enumeration start with 4!
        return telegram.getPayload(fieldNo-4);
    }
}
