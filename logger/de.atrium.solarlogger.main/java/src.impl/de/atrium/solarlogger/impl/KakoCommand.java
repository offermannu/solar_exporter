package de.atrium.solarlogger.impl;/*
 * Copyright (c) Papierfabrik Louisenthal GmbH
 *
 * All rights reserved.
 */

public enum KakoCommand {
    STATUS('0'),

    TOTAL_YIELD('3');

    private final char remoteCommand;

    KakoCommand(char remoteCommand) {
        this.remoteCommand = remoteCommand;
    }

    public static KakoCommand valueOf(char c) {
        switch (c) {
            case '0':
            case 'n':
                return STATUS;
            case '3':
                return TOTAL_YIELD;
            default:
                throw new IllegalArgumentException("Undefined command '" + c + "'");
        }
    }

    public String createFor(KakoDevice d) {
        return "#" + d.getAddress() + remoteCommand + '\r';
    }
}
