/*
 * Copyright (c) Papierfabrik Louisenthal GmbH
 *
 * All rights reserved.
 */
package de.atrium.solarlogger.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DailyYieldDAO {

    private static final Logger LOGGER = Logger.getLogger(DailyYieldDAO.class.getName());

    private static final String     JDBC_URL = "jdbc:postgresql://192.168.60.13:5432/solarlogger";
    private static final Properties DB_PROPS = new Properties();

    static {
        try {
            Class.forName("org.postgresql.Driver");
            DB_PROPS.setProperty("user", "solarlogger");
            DB_PROPS.setProperty("password", "Log and roll;-)");
            DB_PROPS.setProperty("ssl", "false");
            DriverManager.getConnection(JDBC_URL, DB_PROPS);
            LOGGER.info("PostgreSQL Driver loaded, connection test was successful");

        } catch (ClassNotFoundException e) {
            LOGGER.warning("Failed to load PostgreSQL JDBC driver");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to connect to PostgreSQL " + JDBC_URL, e);
        }
    }

    public void save(LocalDate date, int address, Double yield) {
        try (Connection con = DriverManager.getConnection(JDBC_URL, DB_PROPS)) {
            try (PreparedStatement insert = con.prepareStatement(
                    // UPSERT yield
                    "INSERT INTO daily_yield (day, inverter, yield) VALUES (?, ?, ?) " +
                    "ON CONFLICT (day, inverter) DO UPDATE SET yield=EXCLUDED.yield, updated_at=now()")) {

                insert.setObject(1, date);
                insert.setInt(2, address);
                insert.setDouble(3, yield);

                insert.execute();
                LOGGER.info("Upsert (" + date + ", " + address + ", " + yield + ") into daily_yield table");
            } catch (SQLException e) {
                LOGGER.warning("IGNORE: Failed to insert yield because " + e);
            }
        } catch (SQLException e) {
            LOGGER.warning("IGNORE: Failed to access DB " + JDBC_URL + " because " + e);
        }
    }

}
