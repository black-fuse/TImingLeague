package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;

import java.sql.SQLException;

public class Version12 {
    
    public static void updateMySQL() throws SQLException {
        try {
            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_team_heat_entries` (
                      `id` int(11) NOT NULL AUTO_INCREMENT,
                      `heatId` int(11) NOT NULL,
                      `teamId` int(11) NOT NULL,
                      `activeDriverUUID` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                      `currentLap` int(11) NOT NULL DEFAULT 0,
                      `currentCheckpoint` int(11) NOT NULL DEFAULT 0,
                      `startTime` bigint(30) DEFAULT NULL,
                      `endTime` bigint(30) DEFAULT NULL,
                      `position` int(11) DEFAULT NULL,
                      `startPosition` int(11) NOT NULL,
                      `pits` int(11) NOT NULL DEFAULT 0,
                      `finished` tinyint(1) NOT NULL DEFAULT 0,
                      PRIMARY KEY (`id`),
                      FOREIGN KEY (`heatId`) REFERENCES `ts_heats`(`id`) ON DELETE CASCADE,
                      FOREIGN KEY (`teamId`) REFERENCES `ts_teams`(`id`) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                    """);
        } catch (SQLException e) {
            if (e.getErrorCode() != 1050) {
                throw e;
            }
        }
        
        try {
            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_team_laps` (
                      `id` int(11) NOT NULL AUTO_INCREMENT,
                      `teamHeatEntryId` int(11) NOT NULL,
                      `lapNumber` int(11) NOT NULL,
                      `lapStart` bigint(30) NOT NULL,
                      `lapEnd` bigint(30) DEFAULT NULL,
                      `driverUUID` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                      `pitted` tinyint(1) NOT NULL DEFAULT 0,
                      PRIMARY KEY (`id`),
                      FOREIGN KEY (`teamHeatEntryId`) REFERENCES `ts_team_heat_entries`(`id`) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                    """);
        } catch (SQLException e) {
            if (e.getErrorCode() != 1050) {
                throw e;
            }
        }
    }

    public static void updateSQLite() throws SQLException {
        try {
            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_team_heat_entries` (
                      `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                      `heatId` INTEGER NOT NULL,
                      `teamId` INTEGER NOT NULL,
                      `activeDriverUUID` TEXT DEFAULT NULL,
                      `currentLap` INTEGER NOT NULL DEFAULT 0,
                      `currentCheckpoint` INTEGER NOT NULL DEFAULT 0,
                      `startTime` INTEGER DEFAULT NULL,
                      `endTime` INTEGER DEFAULT NULL,
                      `position` INTEGER DEFAULT NULL,
                      `startPosition` INTEGER NOT NULL,
                      `pits` INTEGER NOT NULL DEFAULT 0,
                      `finished` INTEGER NOT NULL DEFAULT 0,
                      FOREIGN KEY (heatId) REFERENCES ts_heats(id) ON DELETE CASCADE,
                      FOREIGN KEY (teamId) REFERENCES ts_teams(id) ON DELETE CASCADE
                    );""");
        } catch (SQLException e) {
            if (!e.getMessage().toLowerCase().contains("already exists")) {
                throw e;
            }
        }
        
        try {
            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_team_laps` (
                      `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                      `teamHeatEntryId` INTEGER NOT NULL,
                      `lapNumber` INTEGER NOT NULL,
                      `lapStart` INTEGER NOT NULL,
                      `lapEnd` INTEGER DEFAULT NULL,
                      `driverUUID` TEXT NOT NULL,
                      `pitted` INTEGER NOT NULL DEFAULT 0,
                      FOREIGN KEY (teamHeatEntryId) REFERENCES ts_team_heat_entries(id) ON DELETE CASCADE
                    );""");
        } catch (SQLException e) {
            if (!e.getMessage().toLowerCase().contains("already exists")) {
                throw e;
            }
        }
    }
}
