package com.tekad.TimingLeague;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private final Plugin plugin;
    private Connection connection;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void connect() throws SQLException {
        File dbfile = new File(plugin.getDataFolder(), "leagues.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbfile.getAbsolutePath());
    }

    public void createTables() throws SQLException {
        String sql = """
                    CREATE TABLE IF NOT EXISTS leagues (
                        name TEXT PRIMARY KEY,
                        predictedDrivers INTEGER
                    );
                    CREATE TABLE IF NOT EXISTS teams (
                        leagueName TEXT,
                        name TEXT,
                        color TEXT,
                        owner TEXT,
                        PRIMARY KEY (leagueName, name),
                        FOREIGN KEY (leagueName) REFERENCES leagues(name)
                    );
                    CREATE TABLE IF NOT EXISTS drivers (
                        leagueName TEXT,
                        uuid TEXT,
                        team TEXT,
                        role TEXT,  -- 'main', 'reserve', or 'none'
                        points INTEGER,
                        FOREIGN KEY (leagueName) REFERENCES leagues(name),
                        FOREIGN KEY (leagueName, team) REFERENCES teams(leagueName, name)
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
        connection.setAutoCommit(false); // start transaction

        try {
            // 1. Save league info
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO leagues (name, predictedDrivers) VALUES (?, ?)")) {
                ps.setString(1, league.getName());
                ps.setInt(2, league.getPredictedDriverCount());
                ps.executeUpdate();
            }

            // 2. Clear old teams, drivers, and calendar
            try (PreparedStatement clearTeams = connection.prepareStatement("DELETE FROM teams WHERE leagueName = ?");
                 PreparedStatement clearDrivers = connection.prepareStatement("DELETE FROM drivers WHERE leagueName = ?");
                 PreparedStatement clearCalendar = connection.prepareStatement("DELETE FROM calendar WHERE leagueName = ?")) {

                clearTeams.setString(1, league.getName());
                clearTeams.executeUpdate();

                clearDrivers.setString(1, league.getName());
                clearDrivers.executeUpdate();

                clearCalendar.setString(1, league.getName());
                clearCalendar.executeUpdate();
            }

            // 3. Save teams
            Set<String> insertedTeams = new HashSet<>();
            try (PreparedStatement teamStmt = connection.prepareStatement(
                    "INSERT OR REPLACE INTO teams (leagueName, name, color, owner) VALUES (?, ?, ?, ?)")) {

                for (Team team : league.getTeams()) {
                    if (!insertedTeams.add(team.getName())) continue; // skip duplicates

                    teamStmt.setString(1, league.getName());
                    teamStmt.setString(2, team.getName());
                    teamStmt.setString(3, team.getColor());
                    teamStmt.setString(4, team.getOwner());
                    teamStmt.addBatch();
                }

                teamStmt.executeBatch();
            }

            // 4. Save drivers
            try (PreparedStatement driverStmt = connection.prepareStatement(
                    "INSERT INTO drivers (leagueName, uuid, team, role, points) VALUES (?, ?, ?, ?, ?)")) {

                for (String uuid : league.getDrivers()) {
                    Team team = league.getTeamByDriver(uuid);
                    if (team == null) continue; // skip drivers without a team

                    String role = "none";
                    if (team.isMain(uuid)) role = "main";
                    else if (team.isReserve(uuid)) role = "reserve";

                    driverStmt.setString(1, league.getName());
                    driverStmt.setString(2, uuid);
                    driverStmt.setString(3, team.getName());
                    driverStmt.setString(4, role);
                    driverStmt.setInt(5, league.getDriverPoints(uuid));
                    driverStmt.addBatch();
                }

                driverStmt.executeBatch();
            }

            // 5. Save calendar
            try (PreparedStatement calendarStmt = connection.prepareStatement(
                    "INSERT INTO calendar (leagueName, eventName) VALUES (?, ?)")) {

                for (String event : league.getCalendar()) {
                    calendarStmt.setString(1, league.getName());
                    calendarStmt.setString(2, event);
                    calendarStmt.addBatch();
                }

                calendarStmt.executeBatch();
            }

            connection.commit(); // commit transaction
        } catch (SQLException e) {
            connection.rollback(); // rollback on error
            throw e;
        } finally {
            connection.setAutoCommit(true); // restore default
        }
    }


    public void saveAllLeagues(Collection<League> leagues) throws SQLException {
        for (League league : leagues) {
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

            // Format: league-holo-<leagueName>-<teamMode>-<UUID>
            // Strip "league-holo-" and then split the rest
            String stripped = hologramName.substring("league-holo-".length()); // e.g. realFc1-false-UUID
            String[] parts = stripped.split("-", 3); // limit to 3 parts to handle league names with hyphens

            if (parts.length < 3) {
                System.err.println("Invalid hologram name format: " + hologramName);
                continue;
            }

            String leagueName = parts[0];
            String teamMode = parts[1];
            String uuidPart = parts[2]; // not used for saving, but included in full name

            holoStmt.setString(1, hologramName);                      // Full ID
            holoStmt.setString(2, leagueName);                        // League name
            holoStmt.setString(3, location.getWorld().getName());     // World
            holoStmt.setDouble(4, location.getX());                   // X
            holoStmt.setDouble(5, location.getY());                   // Y
            holoStmt.setDouble(6, location.getZ());                   // Z

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

            Map<String, League> leagueMap = TImingLeague.getLeagueMap();
            League league = leagueMap.get(leagueName);
            if (league == null) {
                Bukkit.getLogger().warning("Could not load hologram '" + id + "': league '" + leagueName + "' not found.");
                continue;
            }

            // Extract the team mode from the hologram name
            String stripped = id.substring("league-holo-".length()); // realFc1-false-UUID
            String[] parts = stripped.split("-", 3); // [leagueName, teamMode, UUID]

            if (parts.length < 3) {
                Bukkit.getLogger().warning("Invalid hologram ID format: " + id);
                continue;
            }

            String teamModeRaw = parts[1];
            boolean teamMode = Boolean.parseBoolean(teamModeRaw);

            List<String> lines = new ArrayList<>();
            lines.add("&c" + leagueName + " " + (teamMode ? "team" : "driver") + " leaderboard");

            var standings = teamMode
                    ? league.getTeamStandings().entrySet()
                    : league.getDriverStandings().entrySet();

            List<Map.Entry<String, Integer>> sorted = standings.stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .toList();

            for (int i = 0; i < Math.min(15, sorted.size()); i++) {
                var entry = sorted.get(i);
                if (teamMode) {
                    lines.add("&e#" + (i + 1) + ". &b" + entry.getKey() + " &7- &a" + entry.getValue() + " pts");
                } else {
                    String name = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey())).getName();
                    lines.add("&e#" + (i + 1) + ". &b" + (name != null ? name : "Unknown") + " &7- &a" + entry.getValue() + " pts");
                }
            }

            while (lines.size() < 15) {
                lines.add("&e#" + (lines.size() + 1) + ". &7---");
            }

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

        // Load teams
        PreparedStatement teamStmt = connection.prepareStatement(
                "SELECT * FROM teams WHERE leagueName = ?"
        );
        teamStmt.setString(1, name);
        ResultSet teamResult = teamStmt.executeQuery();

        while (teamResult.next()) {
            String teamName = teamResult.getString("name");
            String color = teamResult.getString("color");
            String owner = teamResult.getString("owner");

            Team team = new Team(teamName, color, league);
            team.setOwner(owner);
            league.addTeam(team);
        }
        teamStmt.close();

// Load drivers
        PreparedStatement driverStmt = connection.prepareStatement(
                "SELECT * FROM drivers WHERE leagueName = ?"
        );
        driverStmt.setString(1, name);
        ResultSet driverResult = driverStmt.executeQuery();

        while (driverResult.next()) {
            String uuid = driverResult.getString("uuid");
            String teamName = driverResult.getString("team");
            String role = driverResult.getString("role");
            int points = driverResult.getInt("points");

            Team team = league.getTeams().stream()
                    .filter(t -> t.getName().equalsIgnoreCase(teamName))
                    .findFirst()
                    .orElseGet(() -> {
                        Team newTeam = new Team(teamName, "ffffff", league);
                        league.addTeam(newTeam);
                        return newTeam;
                    });

            league.addDriver(uuid, team);
            if ("main".equalsIgnoreCase(role)) {
                team.addMainDriver(uuid);
            } else if ("reserve".equalsIgnoreCase(role)) {
                team.addReserveDriver(uuid);
            }

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

    public void exportDatabaseToCSV(File dbFile, File outputFolder) throws SQLException, IOException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

        // Get all table names
        DatabaseMetaData meta = connection.getMetaData();
        ResultSet tables = meta.getTables(null, null, "%", new String[]{"TABLE"});

        while (tables.next()) {
            String tableName = tables.getString("TABLE_NAME");
            File csvFile = new File(outputFolder, tableName + ".csv");

            try (
                    PrintWriter writer = new PrintWriter(new FileWriter(csvFile));
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)
            ) {
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();

                // Write header
                for (int i = 1; i <= columnCount; i++) {
                    writer.print(rsmd.getColumnName(i));
                    if (i < columnCount) writer.print(",");
                }
                writer.println();

                // Write rows
                while (rs.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        writer.print(rs.getString(i));
                        if (i < columnCount) writer.print(",");
                    }
                    writer.println();
                }

                System.out.println("Exported table: " + tableName + " -> " + csvFile.getName());
            }
        }

        connection.close();
    }
}
