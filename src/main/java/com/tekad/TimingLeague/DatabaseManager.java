package com.tekad.TimingLeague;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;

public class DatabaseManager {
    private final Plugin plugin;
    private Connection connection;

    public DatabaseManager(Plugin plugin){
        this.plugin = plugin;
    }

    public void connect() throws SQLException{
        File dbfile = new File(plugin.getDataFolder(), "leagues.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbfile.getAbsolutePath());
    }

    public void createTables()  throws  SQLException{
        String sql = """
            CREATE TABLE IF NOT EXISTS leagues (
                name TEXT PRIMARY KEY,
                predictedDrivers INTEGER
            );
            CREATE TABLE IF NOT EXISTS drivers (
                leagueName TEXT,
                uuid TEXT,
                team TEXT
            );
            CREATE TABLE IF NOT EXISTS calendar (
                leagueName TEXT,
                eventName TEXT
            );
        """;

        Statement stmt = connection.createStatement();
        stmt.executeUpdate(sql);
        stmt.close();
    }

    public void saveLeague(League league) throws SQLException {
        String thing = "INSERT OR REPLACE INTO leagues (name, predictedDrivers) VALUES (?, ?)";
        PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO leagues (name, predictedDrivers) VALUES (?, ?)");
        ps.setString(1, league.getName());
        ps.setInt(2, league.getPredictedDriverCount());
        ps.executeUpdate();
        ps.close();



        PreparedStatement driverStmt = connection.prepareStatement(
                "INSERT INTO drivers (leagueName, uuid, team) VALUES (?, ?, ?)");
        for (String uuid : league.getDrivers()) {
            driverStmt.setString(1, league.getName());
            driverStmt.setString(2, uuid);
            driverStmt.setString(3, league.getTeamByDriver(uuid).getName());
            driverStmt.addBatch();
        }
        driverStmt.executeBatch();
        driverStmt.close();
    }
}
