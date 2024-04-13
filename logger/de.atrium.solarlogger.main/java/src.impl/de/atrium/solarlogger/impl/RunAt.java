/*
 * Copyright (c) Papierfabrik Louisenthal GmbH
 *
 * All rights reserved.
 */
package de.atrium.solarlogger.impl;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.stream.Collectors;

import jssc.SerialPortException;
/**
 * Runs a given command at a given Instant
 */
public class RunAt {

    public RunAt(Instant at, Runnable runnable) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
                           @Override
                           public void run() {
                               runnable.run();
                           }
                       }
                , Date.from(at));
    }
}
