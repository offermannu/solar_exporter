/*
 * Copyright (c) Papierfabrik Louisenthal GmbH
 *
 * All rights reserved.
 */
package de.atrium.solarlogger.impl;

public interface MetricsExporter {
    void exportMetrics(KakoTelegram telegram);
}
