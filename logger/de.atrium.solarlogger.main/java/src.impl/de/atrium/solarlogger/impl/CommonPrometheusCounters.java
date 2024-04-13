/*
 * Copyright (c) Papierfabrik Louisenthal GmbH
 *
 * All rights reserved.
 */
package de.atrium.solarlogger.impl;

import java.time.Duration;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.model.snapshots.Unit;
public class CommonPrometheusCounters {

    public static final Counter SSC_EVT_COUNTER = Counter.builder()
                                                         .name("solarlogger_serial_port_event")
                                                         .help("Number of serial port events")
                                                         .labelNames("event")
                                                         .register();
    public static final Gauge   SSC_SPEED_GAUGE = Gauge.builder()
                                                       .name("solarlogger_serial_port_speed")
                                                       .help("Duration of a Kako query [sec]")
                                                       .unit(Unit.SECONDS)
                                                       .labelNames("address")
                                                       .register();

    public static final Gauge KAKO_STATUS_GAUGE =
            Gauge.builder()
                 .name("solarlogger_status")
                 .help("Status of the power inverter")
                 .labelNames("address", "type")
                 .register();

    public static final Counter ERROR_COUNTER =
            Counter.builder()
                   .name("solarlogger_error")
                   .help("Number of errors that occurred during the measurements")
                   .register();

    public static void countSSCDuration(String deviceAddr, Duration duration) {
        SSC_SPEED_GAUGE.labelValues(deviceAddr).set(duration.toMillis() / 1000.0d);
    }

    public static void countSSCEvent(String evtType) {
        SSC_EVT_COUNTER.labelValues(evtType).inc();
    }

    public static void countError() {
        ERROR_COUNTER.inc();
    }
}
