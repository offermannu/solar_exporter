/*
 * Copyright (c) Papierfabrik Louisenthal GmbH
 *
 * All rights reserved.
 */
package de.atrium.solarlogger.impl;

import static de.atrium.solarlogger.impl.CommonPrometheusCounters.KAKO_STATUS_GAUGE;

public class PowadorProtectMetricsExporter implements MetricsExporter {

    @Override
    public void exportMetrics(KakoTelegram telegram) {
        KAKO_STATUS_GAUGE.labelValues(telegram.getAddress(), telegram.getType())
                         .set(telegram.getStatus());

    }
}
