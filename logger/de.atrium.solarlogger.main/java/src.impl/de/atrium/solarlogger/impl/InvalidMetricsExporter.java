/*
 * Copyright (c) Papierfabrik Louisenthal GmbH
 *
 * All rights reserved.
 */
package de.atrium.solarlogger.impl;

public class InvalidMetricsExporter implements MetricsExporter {

    @Override
    public void exportMetrics(KakoTelegram telegram) {
        throw new UnsupportedOperationException("There is no suitable MetricsExporter available for " + telegram);
    }
}
