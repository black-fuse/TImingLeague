package me.makkuusen.timing.system.team;

import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.tplayer.TPlayer;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Static utility class for team management with simplified caching
 */
public class TeamManager {
    // Simple cache for teams (team ID -> team)
    private static final Map<Integer, Team> teamCache = new ConcurrentHashMap<>();
    // Cache for team name to ID mapping
    private static final Map<String, Integer> nameToIdCache = new ConcurrentHashMap<>();

    /**
     * Initialize team manager
     */
    public static void initializeTeams() {
        // Clear any existing cache
        teamCache.clear();
        nameToIdCache.clear();
    }

    /**
     * Unload all teams (cleanup cache)
     */
    public static void unload() {
        teamCache.clear();
        nameToIdCache.clear();
    }

    /**
     * Load a team from database by ID
     * @param teamId the team ID
     * @return Optional containing the team if found
     */
    private static Optional<Team> loadTeamFromDatabase(int teamId) {
        try {
            DbRow teamRow = TimingSystem.getTeamDatabase().selectTeam(teamId);
            if (teamRow == null) {
                return Optional.empty();
            }
            
            Team team = new Team(teamRow);
            loadTeamPlayers(team);
            return Optional.of(team);
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Load team players from database
     * @param team the team to load players for
     */
    private static void loadTeamPlayers(Team team) {
        try {
            // Clear existing players and reload from database
            team.clearPlayers();
            
            List<DbRow> playerRows = TimingSystem.getTeamDatabase().selectTeamPlayers(team.getId());
            for (DbRow playerRow : playerRows) {
                UUID playerUuid = UUID.fromString(playerRow.getString("playerUuid"));
                TPlayer player = TSDatabase.getPlayer(playerUuid);
                if (player != null) {
                    team.addPlayer(player);
                }
            }
            
            // Mark players as loaded
            team.setPlayersLoaded(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cache a team
     * @param team the team to cache
     */
    private static void cacheTeam(Team team) {
        teamCache.put(team.getId(), team);
        nameToIdCache.put(team.getName().toLowerCase(), team.getId());
    }

    /**
     * Invalidate cache for a specific team
     * @param teamId the team ID to invalidate
     */
    private static void invalidateTeamCache(int teamId) {
        Team team = teamCache.remove(teamId);
        if (team != null) {
            nameToIdCache.remove(team.getName().toLowerCase());
        }
    }

    /**
     * Create a new team
     * @param name team name
     * @param creator team creator
     * @return the created team or null if creation failed
     */
    public static Team createTeam(String name, UUID creator) {
        try {
            // Validate team name
            if (!Team.isValidTeamName(name)) {
                return null;
            }
            
            // Check if name already exists (check database directly)
            if (TimingSystem.getTeamDatabase().teamNameExists(name)) {
                return null;
            }
            
            long dateCreated = ApiUtilities.getTimestamp();
            long teamId = TimingSystem.getTeamDatabase().createTeam(name, creator, dateCreated);
            
            Team team = new Team((int) teamId, name, creator, dateCreated);
            cacheTeam(team);
            
            return team;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Delete a team
     * @param team the team to delete
     * @return true if deleted successfully
     */
    public static boolean deleteTeam(Team team) {
        try {
            // Remove from database first
            TimingSystem.getTeamDatabase().deleteTeam(team.getId());
            
            // Clear cache completely to avoid stale data
            invalidateTeamCache(team.getId());
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get a team by name (case insensitive)
     * @param name team name
     * @return Optional containing the team if found
     */
    public static Optional<Team> getTeam(String name) {
        // Check cache first
        Integer teamId = nameToIdCache.get(name.toLowerCase());
        if (teamId != null) {
            Team cachedTeam = teamCache.get(teamId);
            if (cachedTeam != null) {
                return Optional.of(cachedTeam);
            }
        }
        
        // Not in cache, load from database
        try {
            DbRow teamRow = TimingSystem.getTeamDatabase().selectTeamByName(name);
            if (teamRow == null) {
                return Optional.empty();
            }
            
            Team team = new Team(teamRow);
            loadTeamPlayers(team);
            cacheTeam(team);
            return Optional.of(team);
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Get a team by ID
     * @param id team ID
     * @return Optional containing the team if found
     */
    public static Optional<Team> getTeam(int id) {
        // Check cache first
        Team cachedTeam = teamCache.get(id);
        if (cachedTeam != null) {
            return Optional.of(cachedTeam);
        }
        
        // Not in cache, load from database
        Optional<Team> team = loadTeamFromDatabase(id);
        if (team.isPresent()) {
            cacheTeam(team.get());
        }
        return team;
    }

    /**
     * Get all teams
     * @return list of all teams
     */
    public static List<Team> getAllTeams() {
        try {
            List<DbRow> teamRows = TimingSystem.getTeamDatabase().selectTeams();
            List<Team> allTeams = new ArrayList<>();
            
            for (DbRow row : teamRows) {
                int teamId = row.getInt("id");
                
                // Check cache first
                Team cachedTeam = teamCache.get(teamId);
                if (cachedTeam != null) {
                    allTeams.add(cachedTeam);
                } else {
                    // Load from database
                    Team team = new Team(row);
                    loadTeamPlayers(team);
                    cacheTeam(team);
                    allTeams.add(team);
                }
            }
            
            // Sort teams by name
            allTeams.sort(Team::compareTo);
            return allTeams;
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Add a player to a team
     * @param team the team
     * @param player the player to add
     * @return true if added successfully
     */
    public static boolean addPlayerToTeam(Team team, TPlayer player) {
        try {
            if (!canAddPlayerToTeam(team, player)) {
                return false;
            }
            
            int position = TimingSystem.getTeamDatabase().getNextPlayerPosition(team.getId());
            TimingSystem.getTeamDatabase().addPlayerToTeam(team.getId(), player.getUniqueId(), position);
            
            // Reload players from database to ensure consistency
            loadTeamPlayers(team);
            
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Remove a player from a team
     * @param team the team
     * @param player the player to remove
     * @return true if removed successfully
     */
    public static boolean removePlayerFromTeam(Team team, TPlayer player) {
        try {
            if (!team.hasPlayer(player)) {
                return false;
            }
            
            TimingSystem.getTeamDatabase().removePlayerFromTeam(team.getId(), player.getUniqueId());
            
            // Reload players from database to ensure consistency
            loadTeamPlayers(team);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all teams that a player is a member of
     * @param player the player
     * @return list of teams the player belongs to
     */
    public static List<Team> getPlayerTeams(TPlayer player) {
        // Get all teams and filter for ones containing the player
        return getAllTeams().stream()
                .filter(team -> team.hasPlayer(player))
                .collect(Collectors.toList());
    }

    /**
     * Check if a team name is available
     * @param name team name to check
     * @return true if name is available
     */
    public static boolean isTeamNameAvailable(String name) {
        try {
            return !TimingSystem.getTeamDatabase().teamNameExists(name);
        } catch (SQLException e) {
            e.printStackTrace();
            return false; // Assume not available on error
        }
    }

    /**
     * Check if a player can be added to a team
     * @param team the team
     * @param player the player
     * @return true if player can be added
     */
    public static boolean canAddPlayerToTeam(Team team, TPlayer player) {
        return !team.hasPlayer(player);
    }

    /**
     * Get team names for command completion (lightweight database query)
     * @return list of team names
     */
    public static List<String> getTeamNames() {
        try {
            List<DbRow> teamRows = TimingSystem.getTeamDatabase().selectTeams();
            return teamRows.stream()
                    .map(row -> row.getString("name"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get player names for a specific team (for command completion)
     * @param team the team
     * @return list of player names in the team
     */
    public static List<String> getTeamPlayerNames(Team team) {
        return team.getPlayers().stream()
                .map(TPlayer::getName)
                .collect(Collectors.toList());
    }
}