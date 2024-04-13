/*
 * Copyright (c) Papierfabrik Louisenthal GmbH
 *
 * All rights reserved.
 */
package de.atrium.solarlogger.impl;

import java.util.Set;
/**
 * Our Kako device pool.
 * The inverters are listed from left to right as they are arranged in the basement.
 */
public enum KakoDevice {
    POWADOR_PROTECT("01", Set.of(KakoCommand.STATUS)),
    WR_1("05", Set.of(KakoCommand.STATUS, KakoCommand.TOTAL_YIELD)),
    WR_2("02", Set.of(KakoCommand.STATUS, KakoCommand.TOTAL_YIELD)),
    WR_3("03", Set.of(KakoCommand.STATUS, KakoCommand.TOTAL_YIELD)),
    WR_4("04", Set.of(KakoCommand.STATUS, KakoCommand.TOTAL_YIELD));

    private final String           address;
    private final Set<KakoCommand> supportedCommands;

    KakoDevice(String address, Set<KakoCommand> commands) {
        this.address           = address;
        this.supportedCommands = commands;
    }

    public String getAddress() {
        return address;
    }

    public boolean supports(KakoCommand kakoCommand) {
        return supportedCommands.contains(kakoCommand);
    }
}
