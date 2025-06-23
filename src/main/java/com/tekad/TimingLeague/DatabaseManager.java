package com.tekad.TimingLeague;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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



        PreparedStatement calendarStmt = connection.prepareStatement(
                "INSERT INTO calendar (leagueName, eventName) VALUES (?, ?)"
        );
        for (String event : league.getCalendar()){
            calendarStmt.setString(1, league.getName());
            calendarStmt.setString(2, event);
            calendarStmt.addBatch();
        }
        calendarStmt.executeBatch();
        calendarStmt.close();
    }

    public void saveAllLeagues(Collection<League> leagues) throws SQLException {
        for (League league : leagues){
            saveLeague(league);
        }
    }

    // i made chat gpt write this one because i am hopeless when it comes to sql keep an eye on this just in case
    public League loadLeague(String name) throws SQLException {
        // Load base league info
        PreparedStatement leagueStmt = connection.prepareStatement(
                "SELECT * FROM leagues WHERE name = ?");
        leagueStmt.setString(1, name);
        ResultSet leagueResult = leagueStmt.executeQuery();

        if (!leagueResult.next()) {
            leagueStmt.close();
            return null; // League not found
        }

        int predictedDrivers = leagueResult.getInt("predictedDrivers");
        League league = new League(name, predictedDrivers);
        leagueStmt.close();

        // Load drivers and teams
        PreparedStatement driverStmt = connection.prepareStatement(
                "SELECT * FROM drivers WHERE leagueName = ?");
        driverStmt.setString(1, name);
        ResultSet driverResult = driverStmt.executeQuery();

        while (driverResult.next()) {
            String uuid = driverResult.getString("uuid");
            String teamName = driverResult.getString("team");

            Team team = league.getTeams().stream()
                    .filter(t -> t.getName().equalsIgnoreCase(teamName))
                    .findFirst()
                    .orElseGet(() -> {
                        Team newTeam = new Team(teamName, "ffffff", league); // default color
                        league.addTeam(newTeam);
                        return newTeam;
                    });

            league.addDriver(uuid, team);
        }
        driverStmt.close();

        // Load calendar
        PreparedStatement calendarStmt = connection.prepareStatement(
                "SELECT * FROM calendar WHERE leagueName = ?");
        calendarStmt.setString(1, name);
        ResultSet calendarResult = calendarStmt.executeQuery();

        while (calendarResult.next()) {
            String eventName = calendarResult.getString("eventName");
            league.addEvent(eventName);
        }
        calendarStmt.close();

        return league;
    }

    public Map<String, League> loadAllLeagues() throws SQLException {
        Map<String, League> leagues = new HashMap<>();

        PreparedStatement stmt = connection.prepareStatement("SELECT name FROM leagues");
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            String name = rs.getString("name");
            League league = loadLeague(name);
            if (league != null) {
                leagues.put(name, league);
            }
        }

        stmt.close();
        return leagues;
    }

}
