/*
 * Copyright (c) Papierfabrik Louisenthal GmbH
 *
 * All rights reserved.
 */
package de.atrium.solarlogger.impl;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

    public void save(LocalDate date, int address, double yield) {
        upsertYield(date, address, yield);
//        if (yield > 0) {
//            updateYield(date, address, yield);
//        } else {
//            insertNewDay(date, address);
//        }
    }

    private void insertNewDay(LocalDate date, int address) {
        try (Connection con = DriverManager.getConnection(JDBC_URL, DB_PROPS)) {
            try (PreparedStatement insert = con.prepareStatement(
                    // Insert first entry of new day:
                    //  if this happens before midnight, (day, inverter) already exists, and we do nothing
                    //  if this happens after midnight, a new entry for the new day is stored with yield = 0
                    "INSERT INTO daily_yield (day, inverter, yield) " +
                    "                 VALUES (?,   ?,        0) " +
                    "                 ON CONFLICT (day, inverter) DO NOTHING")) {

                insert.setObject(1, date);
                insert.setInt(2, address);

                if (insert.executeUpdate() > 0) {
                    // first insert of new day
                    LOGGER.info("Insert (" + date + ", " + address + ", " + 0 + ") into daily_yield table");
                } else {
                    // second insert of new day or inverter was reset before midnight
                    LOGGER.info("Skipped Insert (" + date + ", " + address + ", " + 0 + ") into daily_yield table");
                }
            } catch (SQLException e) {
                LOGGER.warning("IGNORE: Failed to insert yield because " + e);
            }
        } catch (SQLException e) {
            LOGGER.warning("IGNORE: Failed to access DB " + JDBC_URL + " because " + e);
        }
    }

    private void updateYield(LocalDate date, int address, double yield) {
        try (Connection con = DriverManager.getConnection(JDBC_URL, DB_PROPS)) {
            try (PreparedStatement insert = con.prepareStatement(
                    // yield must increase monotonically!
                    // If this happens before midnight, the yield is updated in a monotonically increasing manner
                    // If this happens after midnight, the update will not find a matching row and we do nothing
                    "UPDATE daily_yield SET yield = ?, updated_at = now() WHERE day=? AND inverter=? and ? > yield")) {

                insert.setDouble(1, yield);
                insert.setObject(2, date);
                insert.setInt(3, address);
                insert.setDouble(4, yield);

                if (insert.executeUpdate() > 0) {
                    // new yield > old yield
                    LOGGER.info("Update (" + date + ", " + address + ", " + yield + ") in daily_yield table");
                } else {
                    // new yield <= old yield or new day
                    LOGGER.info("Skipped Update (" + date + ", " + address + ", " + yield + ") in daily_yield table");
                }
            } catch (SQLException e) {
                LOGGER.warning("IGNORE: Failed to update yield because " + e);
            }
        } catch (SQLException e) {
            LOGGER.warning("IGNORE: Failed to access DB " + JDBC_URL + " because " + e);
        }
    }

    private void upsertYield(LocalDate date, int address, double yield) {
        try (Connection con = DriverManager.getConnection(JDBC_URL, DB_PROPS)) {
            try (PreparedStatement insert = con.prepareStatement(
                    // yield must always increase monotonically!
                    // If this happens before midnight, the yield is updated in a monotonically increasing manner
                    // If this happens after midnight, the update will not find a matching row and inserts a new row
                    "INSERT INTO daily_yield (day, inverter, yield)" +
                    "                 VALUES (?,   ?,        ?) " +
                    "                 ON CONFLICT (day, inverter) " +
                    "                 DO UPDATE SET yield=GREATEST(" +
                    "                       EXCLUDED.yield, " +
                    "                       (SELECT yield FROM daily_yield WHERE day=EXCLUDED.day AND inverter=EXCLUDED.inverter)" +
                    "                 ), updated_at = now()")) {

                insert.setObject(1, date);
                insert.setInt(2, address);
                insert.setDouble(3, yield);

                if (insert.executeUpdate() > 0) {
                    // new day or new yield > old yield
                    LOGGER.info("Upsert (" + date + ", " + address + ", " + yield + ") in daily_yield table");
                } else {
                    // new yield <= old yield
                    LOGGER.info("Skipped Upsert (" + date + ", " + address + ", " + yield + ") in daily_yield table");
                }
            } catch (SQLException e) {
                LOGGER.warning("IGNORE: Failed to upsert yield because " + e);
            }
        } catch (SQLException e) {
            LOGGER.warning("IGNORE: Failed to access DB " + JDBC_URL + " because " + e);
        }
    }

    public void dump(Path dest) {
        LOGGER.info("Dumping table daily_yields into folder " + dest);
        try (Connection con = DriverManager.getConnection(JDBC_URL, DB_PROPS);
             Writer w = Files.newBufferedWriter(dest.resolve("daily_yield.csv"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

            //new CopyManager((BaseConnection) con).copyOut("COPY daily_yield TO STDOUT CSV HEADER", w);
            new CopyManager((BaseConnection) con).copyOut(
                    "COPY (" +
                    "  SELECT\n" +
                    "      day as time,\n" +
                    "      sum(yield) filter (where inverter=5) as \"WR 1\",\n" +
                    "      sum(yield) filter (where inverter=2) as \"WR 2\",\n" +
                    "      sum(yield) filter (where inverter=3) as \"WR 3\",\n" +
                    "      sum(yield) filter (where inverter=4) as \"WR 4\",\n" +
                    "      sum(yield)                           as \"ALL\"\n" +
                    "  FROM daily_yield\n" +
                    "  GROUP BY day\n" +
                    "  ORDER BY day" +
                    ") TO STDOUT CSV HEADER", w);

            LOGGER.info("Done dumping table daily_yields into folder " + dest);
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.WARNING, "IGNORE: Failed to dump yield to " + dest, e);
        }
    }
}
