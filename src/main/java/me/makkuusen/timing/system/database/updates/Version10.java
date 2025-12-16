package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;

import java.sql.SQLException;

public class Version10 {
    public static void updateMySQL() throws SQLException {
        // Add ghostingdelta column if it doesn't exist (it should exist from Version8, but just in case)
        try {
            DB.executeUpdate("ALTER TABLE `ts_heats` ADD `ghostingdelta` int(11) DEFAULT NULL AFTER `lapreset`;");
        } catch (SQLException e) {
            // Error 1060: Duplicate column name. We can ignore this.
            if (e.getErrorCode() != 1060) {
                throw e;
            }
        }
        
        // Add new columns for Version10
        try {
            DB.executeUpdate("ALTER TABLE `ts_heats` ADD `boatSwitching` tinyint(1) DEFAULT NULL AFTER `ghostingdelta`;");
        } catch (SQLException e) {
            // Error 1060: Duplicate column name. We can ignore this.
            if (e.getErrorCode() != 1060) {
                throw e;
            }
        }
        
        try {
            DB.executeUpdate("ALTER TABLE `ts_heats` ADD `collisionMode` VARCHAR(20) DEFAULT NULL AFTER `boatSwitching`;");
        } catch (SQLException e) {
            // Error 1060: Duplicate column name. We can ignore this.
            if (e.getErrorCode() != 1060) {
                throw e;
            }
        }
    }

    public static void updateSQLite() throws SQLException {
        // Add ghostingdelta column if it doesn't exist (it should exist from Version8, but just in case)
        try {
            DB.executeUpdate("ALTER TABLE `ts_heats` ADD `ghostingdelta` INTEGER DEFAULT NULL;");
        } catch (SQLException e) {
            // It's safer to check for the error message in SQLite
            if (!e.getMessage().toLowerCase().contains("duplicate column name")) {
                throw e;
            }
        }
        
        // Add new columns for Version10
        try {
            DB.executeUpdate("ALTER TABLE `ts_heats` ADD `boatSwitching` INTEGER DEFAULT NULL;");
        } catch (SQLException e) {
            // It's safer to check for the error message in SQLite
            if (!e.getMessage().toLowerCase().contains("duplicate column name")) {
                throw e;
            }
        }
        
        try {
            DB.executeUpdate("ALTER TABLE `ts_heats` ADD `collisionMode` TEXT DEFAULT NULL;");
        } catch (SQLException e) {
            // It's safer to check for the error message in SQLite
            if (!e.getMessage().toLowerCase().contains("duplicate column name")) {
                throw e;
            }
        }
    }
}
