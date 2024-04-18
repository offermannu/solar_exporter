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
public interface MetricsExporter {

    Gauge KAKO_MPPT_VOLTAGE_GAUGE =
            Gauge.builder()
                 .name("solarlogger_mppt_voltage")
                 .help("Voltages reported from the Maximum Power Point Tracker (MPPT) [Volt]")
                 .unit(Unit.VOLTS)
                 .labelNames("address", "mppt")
                 .register();

    Gauge KAKO_MPPT_CURRENT_GAUGE =
            Gauge.builder()
                 .name("solarlogger_mppt_current")
                 .help("Currents reported from the Maximum Power Point Tracker (MPPT) [Ampere]")
                 .unit(Unit.AMPERES)
                 .labelNames("address", "mppt")
                 .register();

    Gauge KAKO_MPPT_POWER_GAUGE =
            Gauge.builder()
                 .name("solarlogger_mppt_power")
                 .help("Power reported from the Maximum Power Point Tracker (MPPT) [Watt]")
                 .labelNames("address", "mppt")
                 .register();

    Gauge KAKO_AC_VOLATGE_GAUGE =
            Gauge.builder()
                 .name("solarlogger_ac_voltage")
                 .help("AC-Voltage of phase p [Volt]")
                 .unit(Unit.VOLTS)
                 .labelNames("address", "phase")
                 .register();

    Gauge KAKO_AC_CURRENT_GAUGE =
            Gauge.builder()
                 .name("solarlogger_ac_current")
                 .help("AC-Current of phase p [Ampere]")
                 .unit(Unit.AMPERES)
                 .labelNames("address", "phase")
                 .register();

    Gauge KAKO_TOTAL_DC_POWER_GAUGE =
            Gauge.builder()
                 .name("solarlogger_total_dc_power")
                 .help("DC-Power total [Watt]")
                 .labelNames("address")
                 .register();

    Gauge KAKO_TOTAL_AC_POWER_GAUGE =
            Gauge.builder()
                 .name("solarlogger_total_ac_power")
                 .help("AC-Power total [Watt]")
                 .labelNames("address")
                 .register();

    Gauge KAKO_COS_PHI_GAUGE =
            Gauge.builder()
                 .name("solarlogger_cos_phi")
                 .help("cos(phi)")
                 .labelNames("address", "nature")
                 .register();

    Gauge KAKO_BOARD_TEMPERATURE_GAUGE =
            Gauge.builder()
                 .name("solarlogger_board_temperature")
                 .help("Circuit board temperature (°C)")
                 .unit(Unit.CELSIUS)
                 .labelNames("address")
                 .register();

    Gauge KAKO_DAILY_YIELD_GAUGE =
            Gauge.builder()
                 .name("solarlogger_daily_yield")
                 .help("Daily yield [Wh]")
                 .labelNames("address")
                 .register();

    Gauge METRIC_AC_DAILY_PEAK =
            Gauge.builder()
                 .name("solarlogger_daily_ac_peak")
                 .labelNames("address")
                 .help("AC-daily yield (peak) [W]")
                 .register();

    Gauge METRIC_ACCUMULATED_YIELD =
            Gauge.builder()
                 .name("solarlogger_accumulated_yield")
                 .help("Overall accumulated yield [Wh]")
                 .labelNames("address")
                 .register();

    Gauge METRIC_DAILY_YIELD_HOURS =
            Gauge.builder()
                 .name("solarlogger_daily_yield_hours")
                 .help("daily yield hours [hours]")
                 .labelNames("address")
                 .register();

    Gauge METRIC_ACCUMULATED_YIELD_HOURS =
            Gauge.builder()
                 .name("solarlogger_accumulated_yield_hours")
                 .help("Accumulated yield hours [hours]")
                 .labelNames("address")
                 .register();

    Gauge METRIC_DAILY_GAIN =
            Gauge.builder()
                 .name("solarlogger_daily_gain")
                 .help("In contrast to solarlogger_daily_yield, solarlogger_daily_gain is logged once at the end of the day")
                 .labelNames("address", "month", "year")
                 .register();

    Gauge METRIC_DAY_ENERGIE =
            Gauge.builder()
                 .name("solarlogger_day_energie")
                 .help("In contrast to solarlogger_daily_yield, solarlogger_daily_gain is logged once at the end of the day")
                 .labelNames("address", "day", "month", "year")
                 .register();

    Counter SSC_EVT_COUNTER = Counter.builder()
                                     .name("solarlogger_serial_port_event")
                                     .help("Number of serial port events")
                                     .labelNames("event")
                                     .register();

    Gauge SSC_SPEED_GAUGE = Gauge.builder()
                                 .name("solarlogger_serial_port_speed")
                                 .help("Duration of a Kako query [sec]")
                                 .unit(Unit.SECONDS)
                                 .labelNames("address")
                                 .register();

    Gauge KAKO_STATUS_GAUGE =
            Gauge.builder()
                 .name("solarlogger_status")
                 .help("Status of the power inverter")
                 .labelNames("address", "type")
                 .register();

    Counter ERROR_COUNTER =
            Counter.builder()
                   .name("solarlogger_error")
                   .help("Number of errors that occurred during the measurements")
                   .register();

    static void countSSCDuration(String deviceAddr, Duration duration) {
        SSC_SPEED_GAUGE.labelValues(deviceAddr).set(duration.toMillis() / 1000.0d);
    }

    static void countSSCEvent(String evtType) {
        SSC_EVT_COUNTER.labelValues(evtType).inc();
    }

    static void countError() {
        ERROR_COUNTER.inc();
    }

    void exportMetrics(KakoTelegram telegram);
}
