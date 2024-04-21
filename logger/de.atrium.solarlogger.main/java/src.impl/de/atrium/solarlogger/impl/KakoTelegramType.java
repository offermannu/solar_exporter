package de.atrium.solarlogger.impl;/*
 * Copyright (c) Papierfabrik Louisenthal GmbH
 *
 * All rights reserved.
 */

import java.util.function.Predicate;
public enum KakoTelegramType {

    INVALID(
            t -> !t.isValid(),
            InvalidMetricsExporter.INSTANCE
    ),
    INVERTER_STATUS(
            t->t.isValid() && t.getCmd()==KakoCommand.STATUS && t.getNoe()==20,
            InverterStatusMetricsExporter.INSTANCE
    ),
    INVERTER_TOTAL_YIELD(
            t->t.isValid() && t.getCmd()==KakoCommand.TOTAL_YIELD && t.getNoe()==7,
            TotalYieldMetricsExporter.INSTANCE
    ),
    POWADOR_PROTECT_STATUS(
            t->t.isValid() && t.getCmd()==KakoCommand.STATUS && t.getNoe()==11,
            PowadorProtectMetricsExporter.INSTANCE
    );

    private final Predicate<KakoTelegram> telegramPredicate;

    public MetricsExporter getMetricsExporter() {
        return metricsExporter;
    }

    private final MetricsExporter metricsExporter;

    KakoTelegramType(Predicate<KakoTelegram> telegramPredicate, MetricsExporter metricsExporter) {
        this.telegramPredicate = telegramPredicate;
        this.metricsExporter   = metricsExporter;
    }

    public boolean matches(KakoTelegram t) {
        return telegramPredicate.test(t);
    }
}
