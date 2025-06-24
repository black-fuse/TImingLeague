package com.tekad.TimingLeague;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;
import java.util.*;

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
                team TEXT,
                int points
            );
            CREATE TABLE IF NOT EXISTS calendar (
                leagueName TEXT,
                eventName TEXT
            );
            CREATE TABLE IF NOT EXISTS holograms (
                        id TEXT PRIMARY KEY,
                        leagueName TEXT,
                        world TEXT,
                        x REAL,
                        y REAL,
                        z REAL,
                        FOREIGN KEY (leagueName) REFERENCES leagues(name)
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
                "INSERT INTO drivers (leagueName, uuid, team, points) VALUES (?, ?, ?, ?)");
        for (String uuid : league.getDrivers()) {
            driverStmt.setString(1, league.getName());
            driverStmt.setString(2, uuid);
            driverStmt.setString(3, league.getTeamByDriver(uuid).getName());
            driverStmt.setInt(4,league.getDriverPoints(uuid));
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

    public void saveHolograms(List<String> holograms) throws SQLException {
        String sql = "INSERT OR REPLACE INTO holograms (id, leagueName, world, x, y, z) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement holoStmt = connection.prepareStatement(sql);

        for (String hologramName : holograms) {
            Hologram holo = DHAPI.getHologram(hologramName);
            if (holo == null) continue;

            Location location = holo.getLocation();

            // Extract league name from "league-holo-<leagueName><UUID>"
            // Example: "league-holo-myLeague8f4c62d0-0a2e-4fd2-8d3a-4cbf687aabc5"
            // So we strip "league-holo-" and then parse out the UUID
            String nameWithoutPrefix = hologramName.substring("league-holo-".length());
            int uuidStartIndex = nameWithoutPrefix.length() - 36; // UUID is 36 characters
            String leagueName = nameWithoutPrefix.substring(0, uuidStartIndex);

            holoStmt.setString(1, hologramName);
            holoStmt.setString(2, leagueName);
            holoStmt.setString(3, location.getWorld().getName());
            holoStmt.setDouble(4, location.getX());
            holoStmt.setDouble(5, location.getY());
            holoStmt.setDouble(6, location.getZ());

            holoStmt.addBatch();
        }

        holoStmt.executeBatch();
        holoStmt.close();
    }

    public List<String> loadHolograms() throws SQLException {
        String sql = "SELECT id, leagueName, world, x, y, z FROM holograms";
        PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();
        List<String> holoNames = new ArrayList<>();

        while (rs.next()) {
            String id = rs.getString("id");
            String leagueName = rs.getString("leagueName");
            String worldName = rs.getString("world");
            double x = rs.getDouble("x");
            double y = rs.getDouble("y");
            double z = rs.getDouble("z");

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                Bukkit.getLogger().warning("Could not load hologram '" + id + "': world '" + worldName + "' not found.");
                continue;
            }

            Location location = new Location(world, x, y, z);

            // Generate the standings to show in the hologram
            Map<String, League> laMap= TImingLeague.getLeagueMap();
            League league = laMap.get(leagueName);
            if (league == null) {
                Bukkit.getLogger().warning("Could not load hologram '" + id + "': league '" + leagueName + "' not found.");
                continue;
            }

            List<String> lines = new ArrayList<>();
            lines.add(leagueName + " leaderboard");

            DHAPI.createHologram(id, location, false, lines);
            holoNames.add(id);
        }

        rs.close();
        stmt.close();

        return holoNames;
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
            int points = driverResult.getInt("points");

            Team team = league.getTeams().stream()
                    .filter(t -> t.getName().equalsIgnoreCase(teamName))
                    .findFirst()
                    .orElseGet(() -> {
                        Team newTeam = new Team(teamName, "ffffff", league); // default color
                        league.addTeam(newTeam);
                        return newTeam;
                    });

            league.addDriver(uuid, team);
            league.setDriverPoints(uuid, points);
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
