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

    // ─────────────────────────────────────────────────────────────────────────
    // Table creation
    // ─────────────────────────────────────────────────────────────────────────

    public void createTables() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS leagues (
                    name TEXT PRIMARY KEY,
                    predictedDrivers INTEGER,
                    teamMode TEXT DEFAULT 'MAIN_RESERVE',
                    teamMaxSize INTEGER DEFAULT 0,
                    teamScoringCount INTEGER DEFAULT 0,
                    customScalePoints TEXT DEFAULT NULL,
                    mulliganCount INTEGER DEFAULT 0,
                    teamMulligansEnabled INTEGER DEFAULT 0,
                    scoringSystem TEXT DEFAULT 'Basic',
                    driverStandingsEnabled INTEGER DEFAULT 1,
                    teamStandingsEnabled INTEGER DEFAULT 1
                );
                CREATE TABLE IF NOT EXISTS teams (
                    leagueName TEXT,
                    name TEXT,
                    color TEXT,
                    owner TEXT,
                    points INT,
                    PRIMARY KEY (leagueName, name),
                    FOREIGN KEY (leagueName) REFERENCES leagues(name)
                );
                CREATE TABLE IF NOT EXISTS drivers (
                    leagueName TEXT,
                    uuid TEXT,
                    team TEXT,
                    role TEXT,
                    points INTEGER,
                    PRIMARY KEY (leagueName, uuid),
                    FOREIGN KEY (leagueName) REFERENCES leagues(name)
                );
                CREATE TABLE IF NOT EXISTS calendar (
                    leagueName TEXT,
                    eventName TEXT,
                    category TEXT DEFAULT NULL,
                    heatId TEXT DEFAULT NULL
                );
                CREATE TABLE IF NOT EXISTS event_categories (
                    leagueName TEXT,
                    categoryId TEXT,
                    displayName TEXT,
                    scoringSystem TEXT DEFAULT 'Basic',
                    mulliganCount INTEGER DEFAULT 0,
                    PRIMARY KEY (leagueName, categoryId),
                    FOREIGN KEY (leagueName) REFERENCES leagues(name)
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
                CREATE TABLE IF NOT EXISTS point_history (
                    leagueName TEXT,
                    targetType TEXT,
                    targetId TEXT,
                    source TEXT,
                    eventId TEXT,
                    points INTEGER,
                    timestamp INTEGER,
                    PRIMARY KEY (leagueName, targetType, targetId, source),
                    FOREIGN KEY (leagueName) REFERENCES leagues(name)
                );
                """;
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(sql);
        stmt.close();
    }


    // ─────────────────────────────────────────────────────────────────────────
    // Migration
    // ─────────────────────────────────────────────────────────────────────────

    private boolean columnExists(Connection conn, String table, String column) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("PRAGMA table_info(" + table + ")");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) return true;
            }
        }
        return false;
    }

    private boolean tableExists(Connection conn, String table) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void migrate() throws SQLException {
        log("Running database migrations...");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }

        // leagues table additions
        addColumnIfMissing("leagues", "teamMode",              "TEXT DEFAULT 'MAIN_RESERVE'");
        addColumnIfMissing("leagues", "teamMaxSize",           "INTEGER DEFAULT 0");
        addColumnIfMissing("leagues", "teamScoringCount",      "INTEGER DEFAULT 0");
        addColumnIfMissing("leagues", "customScalePoints",     "TEXT DEFAULT NULL");
        addColumnIfMissing("leagues", "mulliganCount",         "INTEGER DEFAULT 0");
        addColumnIfMissing("leagues", "teamMulligansEnabled",  "INTEGER DEFAULT 0");
        addColumnIfMissing("leagues", "scoringSystem",         "TEXT DEFAULT 'Basic'");
        addColumnIfMissing("leagues", "driverStandingsEnabled","INTEGER DEFAULT 1");
        addColumnIfMissing("leagues", "teamStandingsEnabled",  "INTEGER DEFAULT 1");

        // calendar category + heatId columns
        addColumnIfMissing("calendar", "category", "TEXT DEFAULT NULL");
        addColumnIfMissing("calendar", "heatId",   "TEXT DEFAULT NULL");

        // event_categories table (new)
        if (!tableExists(connection, "event_categories")) {
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS event_categories (
                        leagueName TEXT,
                        categoryId TEXT,
                        displayName TEXT,
                        scoringSystem TEXT DEFAULT 'Basic',
                        mulliganCount INTEGER DEFAULT 0,
                        PRIMARY KEY (leagueName, categoryId),
                        FOREIGN KEY (leagueName) REFERENCES leagues(name)
                    )""");
                log("Created event_categories table");
            }
        }

        // point_history table
        if (!tableExists(connection, "point_history")) {
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS point_history (
                        leagueName TEXT,
                        targetType TEXT,
                        targetId TEXT,
                        source TEXT,
                        eventId TEXT,
                        points INTEGER,
                        timestamp INTEGER,
                        PRIMARY KEY (leagueName, targetType, targetId, source),
                        FOREIGN KEY (leagueName) REFERENCES leagues(name)
                    )""");
                log("Created point_history table");
            }
        }

        log("Database migrations complete.");
    }

    private void addColumnIfMissing(String table, String column, String definition) throws SQLException {
        if (!columnExists(connection, table, column)) {
            log("Migrating " + table + ": adding " + column);
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            }
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // Save
    // ─────────────────────────────────────────────────────────────────────────

    public void saveLeague(League league) throws SQLException {
        log("Saving league: " + league.getName());
        connection.setAutoCommit(false);
        try {
            // leagues row
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO leagues " +
                    "(name, predictedDrivers, teamMode, teamMaxSize, teamScoringCount, " +
                    "customScalePoints, mulliganCount, teamMulligansEnabled, " +
                    "scoringSystem, driverStandingsEnabled, teamStandingsEnabled) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?)")) {
                ps.setString(1, league.getName());
                ps.setInt(2, league.getPredictedDriverCount());
                ps.setString(3, league.getTeamMode().name());
                ps.setInt(4, league.getTeamMaxSize());
                ps.setInt(5, league.getTeamScoringCount());
                ps.setString(6, league.hasCustomScale() ? serializeIntMap(league.getCustomScalePoints()) : null);
                ps.setInt(7, league.getMulliganCount());
                ps.setInt(8, league.isTeamMulligansEnabled() ? 1 : 0);
                ps.setString(9, EventCategory.scoringSystemNameOf(league.getScoringSystem()));
                ps.setInt(10, league.isDriverStandingsEnabled() ? 1 : 0);
                ps.setInt(11, league.isTeamStandingsEnabled() ? 1 : 0);
                ps.executeUpdate();
            }
            logDebug("Saved leagues row for: " + league.getName());

            // clear child tables
            clearTable("drivers",          league.getName());
            clearTable("teams",            league.getName());
            clearTable("calendar",         league.getName());
            clearTable("event_categories", league.getName());
            clearTable("point_history",    league.getName());
            clearTable("holograms",        league.getName());
            logDebug("Cleared child tables for: " + league.getName());

            // teams — always include NoTeam so drivers referencing it have a valid row
            Set<Team> teamsToSave = new LinkedHashSet<>();
            teamsToSave.add(league.NoTeam); // NoTeam first, always
            teamsToSave.addAll(league.getTeams());
            logDebug("Saving " + teamsToSave.size() + " teams (including NoTeam)");

            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO teams (leagueName,name,color,owner,points) VALUES (?,?,?,?,?)")) {
                for (Team team : teamsToSave) {
                    ps.setString(1, league.getName());
                    ps.setString(2, team.getName());
                    ps.setString(3, team.getColor());
                    ps.setString(4, team.getOwner());  // may be null — that's fine
                    ps.setInt(5, team.getPoints());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // drivers
            int driversSaved = 0;
            int driversSkipped = 0;
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO drivers (leagueName,uuid,team,role,points) VALUES (?,?,?,?,?)")) {
                for (String uuid : league.getDrivers()) {
                    Team team = league.getTeamByDriver(uuid);
                    if (team == null) {
                        logDebug("Skipping driver " + uuid + " (null team)");
                        driversSkipped++;
                        continue;
                    }
                    String role = team.isMain(uuid) ? "main"
                            : team.isReserve(uuid) ? "reserve"
                            : String.valueOf(team.getPriority(uuid));
                    ps.setString(1, league.getName());
                    ps.setString(2, uuid);
                    ps.setString(3, team.getName());
                    ps.setString(4, role);
                    ps.setInt(5, league.getDriverPoints(uuid));
                    ps.addBatch();
                    driversSaved++;
                }
                ps.executeBatch();
            }
            logDebug("Saved " + driversSaved + " drivers" +
                    (driversSkipped > 0 ? ", skipped " + driversSkipped + " with null team" : ""));

            // calendar
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO calendar (leagueName,eventName,category,heatId) VALUES (?,?,?,?)")) {
                for (CalendarEntry entry : league.getCalendarEntries()) {
                    ps.setString(1, league.getName());
                    ps.setString(2, entry.getEventName());
                    ps.setString(3, entry.getCategoryId());   // may be null
                    ps.setString(4, entry.getHeatId());       // may be null
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            logDebug("Saved " + league.getCalendarEntries().size() + " calendar entries");

            // event_categories
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO event_categories " +
                    "(leagueName,categoryId,displayName,scoringSystem,mulliganCount) VALUES (?,?,?,?,?)")) {
                for (EventCategory cat : league.getCategories().values()) {
                    ps.setString(1, league.getName());
                    ps.setString(2, cat.getId());
                    ps.setString(3, cat.getDisplayName());
                    ps.setString(4, cat.getScoringSystemName());
                    ps.setInt(5, cat.getMulliganCount());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            logDebug("Saved " + league.getCategories().size() + " categories");

            // point_history
            savePointHistory(league);

            connection.commit();
            log("Saved league: " + league.getName());
        } catch (SQLException e) {
            log("ERROR saving " + league.getName() + ": " + e.getMessage() + " — rolling back");
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private void clearTable(String table, String leagueName) throws SQLException {
        logDebug("clearing table " + table + " " + leagueName);
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM " + table + " WHERE leagueName = ?")) {
            ps.setString(1, leagueName);
            ps.executeUpdate();
        }
    }

    private void savePointHistory(League league) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO point_history " +
                "(leagueName,targetType,targetId,source,eventId,points,timestamp) VALUES (?,?,?,?,?,?,?)")) {
            for (Map.Entry<String, List<PointEntry>> e : league.getDriverPointHistory().entrySet()) {
                for (PointEntry pe : e.getValue()) {
                    ps.setString(1, league.getName()); ps.setString(2, "driver");
                    ps.setString(3, e.getKey());       ps.setString(4, pe.getSource());
                    ps.setString(5, pe.getEventId());  ps.setInt(6, pe.getPoints());
                    ps.setLong(7, pe.getTimestamp());  ps.addBatch();
                }
            }
            for (Map.Entry<String, List<PointEntry>> e : league.getTeamPointHistory().entrySet()) {
                for (PointEntry pe : e.getValue()) {
                    ps.setString(1, league.getName()); ps.setString(2, "team");
                    ps.setString(3, e.getKey());       ps.setString(4, pe.getSource());
                    ps.setString(5, pe.getEventId());  ps.setInt(6, pe.getPoints());
                    ps.setLong(7, pe.getTimestamp());  ps.addBatch();
                }
            }
            ps.executeBatch();
        } catch (SQLException e) {
            log("WARNING: Failed to save point history for " + league.getName() + ": " + e.getMessage());
        }
    }

    public void saveAllLeagues(Collection<League> leagues) throws SQLException {
        for (League league : leagues) saveLeague(league);
    }


    // ─────────────────────────────────────────────────────────────────────────
    // Load
    // ─────────────────────────────────────────────────────────────────────────

    public League loadLeague(String name) throws SQLException {
        logDebug("Loading league: " + name);
        League league;

        // Base league row
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM leagues WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                league = new League(name, rs.getInt("predictedDrivers"));

                // TeamMode
                try { league.setTeamMode(TeamMode.valueOf(rs.getString("teamMode"))); }
                catch (Exception ignored) {}

                league.setTeamMaxSize(getIntSafe(rs, "teamMaxSize", 4));
                league.setTeamScoringCount(getIntSafe(rs, "teamScoringCount", 2));

                // Custom scale
                try {
                    String scaleJson = rs.getString("customScalePoints");
                    if (scaleJson != null && !scaleJson.isEmpty()) {
                        deserializeIntMap(scaleJson).forEach(league::setCustomScalePoint);
                    }
                } catch (Exception ignored) {}

                // Global mulligans
                league.setMulliganCount(getIntSafe(rs, "mulliganCount", 0));
                league.setTeamMulligansEnabled(getIntSafe(rs, "teamMulligansEnabled", 0) == 1);

                // Scoring system
                try {
                    String sysName = rs.getString("scoringSystem");
                    league.setScoringSystem(EventCategory.scoringSystemFromName(sysName));
                } catch (Exception ignored) {}

                // Standings toggles
                league.setDriverStandingsEnabled(getIntSafe(rs, "driverStandingsEnabled", 1) == 1);
                league.setTeamStandingsEnabled(getIntSafe(rs, "teamStandingsEnabled", 1) == 1);
            }
        }

        // event_categories
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM event_categories WHERE leagueName = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                int catCount = 0;
                while (rs.next()) {
                    String id          = rs.getString("categoryId");
                    String displayName = rs.getString("displayName");
                    String sysName     = rs.getString("scoringSystem");
                    int mulligans      = rs.getInt("mulliganCount");
                    EventCategory cat  = new EventCategory(
                            id, displayName,
                            EventCategory.scoringSystemFromName(sysName),
                            mulligans);
                    league.addCategory(cat);
                    catCount++;
                }
                logDebug("Loaded " + catCount + " categories for: " + name);
            }
        }

        // teams
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM teams WHERE leagueName = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                int teamCount = 0;
                while (rs.next()) {
                    String teamName = rs.getString("name");
                    Team team = new Team(teamName, rs.getString("color"), league);
                    team.setOwner(rs.getString("owner"));
                    league.addTeam(team);
                    int pts = Integer.parseInt(rs.getString("points"));
                    league.setTeamPoints(teamName, pts);
                    team.setPoints(pts);
                    teamCount++;
                }
                logDebug("Loaded " + teamCount + " teams for: " + name);
            }
        }

        // drivers
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM drivers WHERE leagueName = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                int driverCount = 0;
                while (rs.next()) {
                    String uuid     = rs.getString("uuid");
                    String teamName = rs.getString("team");
                    String role     = rs.getString("role");
                    int points      = rs.getInt("points");

                    Team team = league.getTeams().stream()
                            .filter(t -> t.getName().equalsIgnoreCase(teamName))
                            .findFirst()
                            .orElseGet(() -> {
                                log("WARNING: driver " + uuid + " references unknown team '" + teamName + "' — creating placeholder");
                                Team nt = new Team(teamName, "ffffff", league);
                                league.addTeam(nt);
                                return nt;
                            });

                    league.addDriver(uuid, team);
                    if ("main".equalsIgnoreCase(role))         team.addMainDriver(uuid);
                    else if ("reserve".equalsIgnoreCase(role)) team.addReserveDriver(uuid);
                    else {
                        try { team.addPriorityDriver(uuid, Integer.parseInt(role)); }
                        catch (NumberFormatException ignored) {}
                    }
                    league.setDriverPoints(uuid, points);
                    driverCount++;
                }
                logDebug("Loaded " + driverCount + " drivers for: " + name);
            }
        }

        // calendar (now with category + heatId)
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM calendar WHERE leagueName = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String eventName = rs.getString("eventName");
                    String category  = rs.getString("category");  // may be null
                    String heatId    = rs.getString("heatId");    // may be null
                    league.addCalendarEntry(new CalendarEntry(eventName, category, heatId));
                }
            }
        }

        // point_history
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM point_history WHERE leagueName = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PointEntry entry = new PointEntry(
                            rs.getString("source"),
                            rs.getString("eventId"),
                            rs.getInt("points"),
                            rs.getLong("timestamp"));
                    if ("driver".equals(rs.getString("targetType"))) {
                        league.addDriverPointEntry(rs.getString("targetId"), entry);
                    } else {
                        league.addTeamPointEntry(rs.getString("targetId"), entry);
                    }
                }
            }
        } catch (SQLException e) {
            log("WARNING: Failed to load point history for " + name + ": " + e.getMessage());
        }

        return league;
    }

    public Map<String, League> loadAllLeagues() throws SQLException {
        Map<String, League> leagues = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT name FROM leagues");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("name");
                League league = loadLeague(name);
                if (league != null) leagues.put(name, league);
            }
        }
        return leagues;
    }

    private int getIntSafe(ResultSet rs, String col, int fallback) {
        try { return rs.getInt(col); } catch (Exception ignored) { return fallback; }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // Hologram save / load (unchanged logic, kept here)
    // ─────────────────────────────────────────────────────────────────────────

    public void saveHolograms(List<String> holograms) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM holograms")) {
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO holograms (id,leagueName,world,x,y,z) VALUES (?,?,?,?,?,?)")) {
            for (String hologramName : holograms) {
                Hologram holo = DHAPI.getHologram(hologramName);
                if (holo == null) continue;
                Location loc = holo.getLocation();
                String stripped = hologramName.substring("league-holo-".length());
                String[] parts = stripped.split("-", 3);
                if (parts.length < 3) continue;
                ps.setString(1, hologramName);
                ps.setString(2, parts[0]);
                ps.setString(3, loc.getWorld().getName());
                ps.setDouble(4, loc.getX());
                ps.setDouble(5, loc.getY());
                ps.setDouble(6, loc.getZ());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<String> loadHolograms() throws SQLException {
        List<String> holoNames = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id,leagueName,world,x,y,z FROM holograms");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id         = rs.getString("id");
                String leagueName = rs.getString("leagueName");
                String worldName  = rs.getString("world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) { Bukkit.getLogger().warning("[TL] Hologram world not found: " + worldName); continue; }

                Map<String, League> leagueMap = TImingLeague.getLeagueMap();
                League league = leagueMap.get(leagueName);
                if (league == null) { Bukkit.getLogger().warning("[TL] Hologram league not found: " + leagueName); continue; }

                Location location = new Location(world, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"));
                String stripped = id.substring("league-holo-".length());
                String[] parts = stripped.split("-", 3);
                if (parts.length < 3) { Bukkit.getLogger().warning("[TL] Invalid hologram ID: " + id); continue; }

                boolean teamMode = Boolean.parseBoolean(parts[1]);
                List<String> lines = new ArrayList<>();
                lines.add("&c" + leagueName + " " + (teamMode ? "team" : "driver") + " leaderboard");

                var standings = teamMode ? league.getTeamStandings().entrySet()
                                         : league.getDriverStandings().entrySet();
                var sorted = standings.stream()
                        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                        .toList();

                for (int i = 0; i < Math.min(15, sorted.size()); i++) {
                    var entry = sorted.get(i);
                    if (teamMode) {
                        lines.add("&e#" + (i+1) + ". &b" + entry.getKey() + " &7- &a" + entry.getValue() + " pts");
                    } else {
                        String pName = Bukkit.getOfflinePlayer(java.util.UUID.fromString(entry.getKey())).getName();
                        lines.add("&e#" + (i+1) + ". &b" + (pName != null ? pName : "Unknown") + " &7- &a" + entry.getValue() + " pts");
                    }
                }
                while (lines.size() < 15) lines.add("&e#" + lines.size() + ". &7---");

                DHAPI.createHologram(id, location, false, lines);
                holoNames.add(id);
            }
        }
        return holoNames;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CSV export
    // ─────────────────────────────────────────────────────────────────────────

    public void exportDatabaseToCSV(File dbFile, File outputFolder) throws SQLException, IOException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        DatabaseMetaData meta = conn.getMetaData();
        ResultSet tables = meta.getTables(null, null, "%", new String[]{"TABLE"});
        while (tables.next()) {
            String tableName = tables.getString("TABLE_NAME");
            File csvFile = new File(outputFolder, tableName + ".csv");
            try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile));
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {
                ResultSetMetaData rsmd = rs.getMetaData();
                int cols = rsmd.getColumnCount();
                for (int i = 1; i <= cols; i++) {
                    writer.print(rsmd.getColumnName(i));
                    if (i < cols) writer.print(",");
                }
                writer.println();
                while (rs.next()) {
                    for (int i = 1; i <= cols; i++) {
                        writer.print(rs.getString(i));
                        if (i < cols) writer.print(",");
                    }
                    writer.println();
                }
            }
        }
        conn.close();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON helpers for Map<Integer,Integer>
    // ─────────────────────────────────────────────────────────────────────────

    private String serializeIntMap(Map<Integer, Integer> map) {
        if (map == null || map.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<Integer, Integer> e : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":").append(e.getValue());
            first = false;
        }
        return sb.append("}").toString();
    }

    private Map<Integer, Integer> deserializeIntMap(String json) {
        Map<Integer, Integer> result = new HashMap<>();
        if (json == null || json.isBlank()) return result;
        String content = json.trim().replaceAll("^\\{|\\}$", "");
        if (content.isBlank()) return result;
        for (String pair : content.split(",")) {
            String[] parts = pair.split(":");
            if (parts.length != 2) continue;
            try {
                int key = Integer.parseInt(parts[0].trim().replace("\"", ""));
                int val = Integer.parseInt(parts[1].trim());
                if (key > 0) result.put(key, val);
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    private void log(String msg) {
        Bukkit.getLogger().info("[TimingLeague] " + msg);
    }

    /**
     * Debug-level log — shows progress through save/load steps.
     * Kept at INFO level but prefixed with [DEBUG] so it can be grepped
     * without flooding the console the way FINE/FINER would.
     * To silence these, search for [TimingLeague] [DEBUG] in your log filter.
     */
    private void logDebug(String msg) {
        Boolean debug = false;
        if (debug){
            Bukkit.getLogger().info("[TimingLeague] [DEBUG] " + msg);
        }
    }
}
