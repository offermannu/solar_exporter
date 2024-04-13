/*
 * Copyright (c) Papierfabrik Louisenthal GmbH
 *
 * All rights reserved.
 */
package de.atrium.solarlogger.impl;

import static jssc.SerialPort.MASK_BREAK;
import static jssc.SerialPort.MASK_CTS;
import static jssc.SerialPort.MASK_DSR;
import static jssc.SerialPort.MASK_ERR;
import static jssc.SerialPort.MASK_RING;
import static jssc.SerialPort.MASK_RLSD;
import static jssc.SerialPort.MASK_RXCHAR;
import static jssc.SerialPort.MASK_RXFLAG;
import static jssc.SerialPort.MASK_TXEMPTY;

/**
 * JSSC Event Types as enum
 */
enum SscEventType {
    RXCHAR(MASK_RXCHAR),
    RXFLAG(MASK_RXFLAG),
    TXEMPTY(MASK_TXEMPTY),
    CTS(MASK_CTS),
    DSR(MASK_DSR),
    RLSD(MASK_RLSD),
    BREAK(MASK_BREAK),
    ERR(MASK_ERR),
    RING(MASK_RING),
    UNKNOWN(0);

    private final int mask;

    SscEventType(int mask) {
        this.mask = mask;
    }

    static SscEventType of(int mask) {
        for (SscEventType eventType : values()) {
            if (eventType.mask == mask) {
                return eventType;
            }
        }
        return UNKNOWN;
    }
}
