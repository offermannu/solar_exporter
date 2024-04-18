/*
 * Copyright (c) Papierfabrik Louisenthal GmbH
 *
 * All rights reserved.
 */
package de.atrium.solarlogger.impl;

import static de.atrium.solarlogger.impl.KakoStatusTelegramPayload.AC_POWER_TOTAL;
import static de.atrium.solarlogger.impl.KakoStatusTelegramPayload.BOARD_TEMPERATURE;
import static de.atrium.solarlogger.impl.KakoStatusTelegramPayload.COS_PHI;
import static de.atrium.solarlogger.impl.KakoStatusTelegramPayload.DAILY_YIELD;
import static de.atrium.solarlogger.impl.KakoStatusTelegramPayload.DC_POWER_TOTAL;
import static de.atrium.solarlogger.impl.KakoStatusTelegramPayload.MPPT_1_DC_CURRENT;
import static de.atrium.solarlogger.impl.KakoStatusTelegramPayload.MPPT_1_DC_POWER;
import static de.atrium.solarlogger.impl.KakoStatusTelegramPayload.MPPT_1_DC_VOLTAGE;
import static de.atrium.solarlogger.impl.KakoStatusTelegramPayload.MPPT_2_DC_CURRENT;
import static de.atrium.solarlogger.impl.KakoStatusTelegramPayload.MPPT_2_DC_POWER;
import static de.atrium.solarlogger.impl.KakoStatusTelegramPayload.MPPT_2_DC_VOLTAGE;
import static de.atrium.solarlogger.impl.KakoStatusTelegramPayload.PHASE_1_AC_CURRENT;
import static de.atrium.solarlogger.impl.KakoStatusTelegramPayload.PHASE_1_AC_VOLTAGE;
import static de.atrium.solarlogger.impl.KakoStatusTelegramPayload.PHASE_2_AC_CURRENT;
import static de.atrium.solarlogger.impl.KakoStatusTelegramPayload.PHASE_2_AC_VOLTAGE;
import static de.atrium.solarlogger.impl.KakoStatusTelegramPayload.PHASE_3_AC_CURRENT;
import static de.atrium.solarlogger.impl.KakoStatusTelegramPayload.PHASE_3_AC_VOLTAGE;
public class InverterStatusMetricsExporter implements MetricsExporter {

    @Override
    public void exportMetrics(KakoTelegram telegram) {
        String address = telegram.getAddress();
        KAKO_STATUS_GAUGE.labelValues(address, telegram.getType()).set(telegram.getStatus());

        MetricsExporter.KAKO_MPPT_VOLTAGE_GAUGE.labelValues(address, "1").set(MPPT_1_DC_VOLTAGE.getValue(telegram));
        MetricsExporter.KAKO_MPPT_VOLTAGE_GAUGE.labelValues(address, "2").set(MPPT_2_DC_VOLTAGE.getValue(telegram));

        MetricsExporter.KAKO_MPPT_CURRENT_GAUGE.labelValues(address, "1").set(MPPT_1_DC_CURRENT.getValue(telegram));
        MetricsExporter.KAKO_MPPT_CURRENT_GAUGE.labelValues(address, "2").set(MPPT_2_DC_CURRENT.getValue(telegram));

        MetricsExporter.KAKO_MPPT_POWER_GAUGE.labelValues(address, "1").set(MPPT_1_DC_POWER.getValue(telegram));
        MetricsExporter.KAKO_MPPT_POWER_GAUGE.labelValues(address, "2").set(MPPT_2_DC_POWER.getValue(telegram));

        MetricsExporter.KAKO_AC_VOLATGE_GAUGE.labelValues(address, "1").set(PHASE_1_AC_VOLTAGE.getValue(telegram));
        MetricsExporter.KAKO_AC_VOLATGE_GAUGE.labelValues(address, "2").set(PHASE_2_AC_VOLTAGE.getValue(telegram));
        MetricsExporter.KAKO_AC_VOLATGE_GAUGE.labelValues(address, "3").set(PHASE_3_AC_VOLTAGE.getValue(telegram));

        MetricsExporter.KAKO_AC_CURRENT_GAUGE.labelValues(address, "1").set(PHASE_1_AC_CURRENT.getValue(telegram));
        MetricsExporter.KAKO_AC_CURRENT_GAUGE.labelValues(address, "2").set(PHASE_2_AC_CURRENT.getValue(telegram));
        MetricsExporter.KAKO_AC_CURRENT_GAUGE.labelValues(address, "3").set(PHASE_3_AC_CURRENT.getValue(telegram));

        MetricsExporter.KAKO_TOTAL_DC_POWER_GAUGE.labelValues(address).set(DC_POWER_TOTAL.getValue(telegram));
        MetricsExporter.KAKO_TOTAL_AC_POWER_GAUGE.labelValues(address).set(AC_POWER_TOTAL.getValue(telegram));

        MetricsExporter.KAKO_BOARD_TEMPERATURE_GAUGE.labelValues(address).set(BOARD_TEMPERATURE.getValue(telegram));
        MetricsExporter.KAKO_DAILY_YIELD_GAUGE.labelValues(address).set(DAILY_YIELD.getValue(telegram));

        // cos-phi (a measure of the reactive power or power loss) - syntax is x.xxx[i|c|] - e.g. "0.998i", "0.995c", "1.000"
        // 'c': capacitive reactive power
        // 'i': Inductive reactive power
        // '':  no letter
        String cosPhiStrValue = COS_PHI.getStrValue(telegram);
        int    length         = cosPhiStrValue.length();
        char   cosPhiNature   = cosPhiStrValue.charAt(length - 1);
        if (Character.isDigit(cosPhiNature)) {
            // no nature
            // e.g. "1.000" => cosPhi=1.000, nature=""
            double cosPhi = Double.parseDouble(cosPhiStrValue);
            MetricsExporter.KAKO_COS_PHI_GAUGE.labelValues(address, "").set(cosPhi);

        } else {
            // reactive power nature
            // e.g. "0.998i" => cosPhi=0.998, nature="i"
            double cosPhi = Double.parseDouble(cosPhiStrValue.substring(0, length-1));
            MetricsExporter.KAKO_COS_PHI_GAUGE.labelValues(address, String.valueOf(cosPhiNature)).set(cosPhi);
        }
    }
}
