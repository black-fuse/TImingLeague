package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;

import java.sql.SQLException;

public class Version11 {
    public static void updateMySQL() throws SQLException {
        // Create teams table
        try {
            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_teams` (
                      `id` int(11) NOT NULL AUTO_INCREMENT,
                      `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                      `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                      `dateCreated` bigint(30) NOT NULL,
                      `isRemoved` tinyint(1) NOT NULL DEFAULT '0',
                      PRIMARY KEY (`id`),
                      UNIQUE KEY `name` (`name`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                    """);
        } catch (SQLException e) {
            // Table might already exist, check if it's a duplicate table error
            if (e.getErrorCode() != 1050) { // Error 1050: Table already exists
                throw e;
            }
        }
        
        // Create team players table
        try {
            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_team_players` (
                      `id` int(11) NOT NULL AUTO_INCREMENT,
                      `teamId` int(11) NOT NULL,
                      `playerUuid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                      `position` int(11) NOT NULL,
                      `dateAdded` bigint(30) NOT NULL,
                      PRIMARY KEY (`id`),
                      FOREIGN KEY (`teamId`) REFERENCES `ts_teams`(`id`) ON DELETE CASCADE,
                      UNIQUE KEY `team_player` (`teamId`, `playerUuid`),
                      UNIQUE KEY `team_position` (`teamId`, `position`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                    """);
        } catch (SQLException e) {
            // Table might already exist, check if it's a duplicate table error
            if (e.getErrorCode() != 1050) { // Error 1050: Table already exists
                throw e;
            }
        }
    }

    public static void updateSQLite() throws SQLException {
        // Create teams table
        try {
            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_teams` (
                      `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                      `name` TEXT NOT NULL UNIQUE,
                      `creator` TEXT NOT NULL,
                      `dateCreated` INTEGER NOT NULL,
                      `isRemoved` INTEGER NOT NULL DEFAULT 0
                    );""");
        } catch (SQLException e) {
            // Table might already exist
            if (!e.getMessage().toLowerCase().contains("already exists")) {
                throw e;
            }
        }
        
        // Create team players table
        try {
            DB.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `ts_team_players` (
                      `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                      `teamId` INTEGER NOT NULL,
                      `playerUuid` TEXT NOT NULL,
                      `position` INTEGER NOT NULL,
                      `dateAdded` INTEGER NOT NULL,
                      FOREIGN KEY (teamId) REFERENCES ts_teams(id),
                      UNIQUE(teamId, playerUuid),
                      UNIQUE(teamId, position)
                    );""");
        } catch (SQLException e) {
            // Table might already exist
            if (!e.getMessage().toLowerCase().contains("already exists")) {
                throw e;
            }
        }
    }
}