package com.tekad.TimingLeague.API;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tekad.TimingLeague.API.config.APIConfig;
import com.tekad.TimingLeague.API.models.ErrorResponse;
import com.tekad.TimingLeague.League;
import com.tekad.TimingLeague.PointEntry;
import com.tekad.TimingLeague.Team;
import com.tekad.TimingLeague.TImingLeague;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import spark.Spark;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class RestAPIServer {
    private final TImingLeague plugin;
    private final APIConfig config;
    private final Gson gson;
    private final Map<String, RateLimitBucket> rateLimits = new ConcurrentHashMap<>();

    public RestAPIServer(TImingLeague plugin, APIConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void start() {
        if (!config.isEnabled()) {
            plugin.getLogger().info("[API] REST API is disabled in config");
            return;
        }

        ipAddress("0.0.0.0");
        port(config.getPort());

        // CORS
        if (config.isCorsEnabled()) {
            before((req, res) -> {
                res.header("Access-Control-Allow-Origin", getAllowedOrigin(req));
                res.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                res.header("Access-Control-Allow-Headers", "X-API-Key, Content-Type");
                res.type("application/json");
            });

            options("/*", (req, res) -> "OK");
        }

        // Auth middleware
        before("/api/*", (req, res) -> {
            String apiKey = req.headers("X-API-Key");
            String path = req.pathInfo();
            String method = req.requestMethod();
            
            plugin.getLogger().info("[API] " + method + " " + path);

            if (apiKey == null || apiKey.isEmpty()) {
                halt(401, gson.toJson(new ErrorResponse("Missing X-API-Key header", 401)));
            }

            // Check rate limit
            if (isRateLimited(apiKey)) {
                halt(429, gson.toJson(new ErrorResponse("Rate limit exceeded", 429)));
            }

            // Validate key
            boolean isWriteEndpoint = method.equals("POST");

            if (isWriteEndpoint) {
                if (!apiKey.equals(config.getAdminApiKey())) {
                    halt(401, gson.toJson(new ErrorResponse("Invalid admin API key", 401)));
                }
            } else {
                if (!apiKey.equals(config.getReadApiKey()) && !apiKey.equals(config.getAdminApiKey())) {
                    halt(401, gson.toJson(new ErrorResponse("Invalid API key", 401)));
                }
            }
        });

        setupRoutes();

        plugin.getLogger().info("[API] REST API started on port " + config.getPort());
        plugin.getLogger().info("[API] Read Key: " + config.getReadApiKey());
        plugin.getLogger().info("[API] Admin Key: " + config.getAdminApiKey());
    }

    private void setupRoutes() {
        // Health check
        get("/api/health", (req, res) -> {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "ok");
            health.put("timestamp", System.currentTimeMillis());
            return gson.toJson(health);
        });

        // List all leagues
        get("/api/leagues", (req, res) -> {
            Set<String> leagues = TImingLeague.getLeagueMap().keySet();
            Map<String, Object> response = new HashMap<>();
            response.put("leagues", leagues);
            response.put("count", leagues.size());
            return gson.toJson(response);
        });

        // GET /api/league/{id}/standings
        get("/api/league/:id/standings", (req, res) -> {
            String leagueId = req.params(":id");
            plugin.getLogger().info("[API] GET /standings - League: " + leagueId);
            
            League league = TImingLeague.getLeagueMap().get(leagueId);

            if (league == null) {
                res.status(404);
                return gson.toJson(new ErrorResponse("League not found", 404));
            }

            return gson.toJson(buildStandingsResponse(league));
        });

        // GET /api/league/{id}/drivers  
        get("/api/league/:id/drivers", (req, res) -> {
        String leagueId = req.params(":id");
        plugin.getLogger().info("[API] GET /drivers - League: " + leagueId);
            
            League league = TImingLeague.getLeagueMap().get(leagueId);

            if (league == null) {
                res.status(404);
                return gson.toJson(new ErrorResponse("League not found", 404));
            }

            return gson.toJson(buildDriversResponse(league));
        });

        // GET /api/league/{id}/teams
        get("/api/league/:id/teams", (req, res) -> {
            String leagueId = req.params(":id");
            plugin.getLogger().info("[API] GET /teams - League: " + leagueId);
            
            League league = TImingLeague.getLeagueMap().get(leagueId);

            if (league == null) {
                res.status(404);
                return gson.toJson(new ErrorResponse("League not found", 404));
            }

            return gson.toJson(buildTeamsResponse(league));
        });

        // GET /api/league/{id}/calendar
        get("/api/league/:id/calendar", (req, res) -> {
            String leagueId = req.params(":id");
            plugin.getLogger().info("[API] GET /calendar - League: " + leagueId);
            
            League league = TImingLeague.getLeagueMap().get(leagueId);

            if (league == null) {
                res.status(404);
                return gson.toJson(new ErrorResponse("League not found", 404));
            }

            return gson.toJson(buildCalendarResponse(league));
        });

        // POST /api/league/{id}/penalty
        post("/api/league/:id/penalty", (req, res) -> {
            String leagueId = req.params(":id");
            plugin.getLogger().info("[API] POST /penalty - League: " + leagueId);
            
            League league = TImingLeague.getLeagueMap().get(leagueId);

            if (league == null) {
                plugin.getLogger().warning("[API] League '" + leagueId + "' not found");
                res.status(404);
                return gson.toJson(new ErrorResponse("League not found", 404));
            }

            String requestBody = req.body();
            plugin.getLogger().info("[API] Request body: " + requestBody);
            
            if (requestBody == null || requestBody.trim().isEmpty()) {
                plugin.getLogger().warning("[API] Empty request body");
                res.status(400);
                return gson.toJson(new ErrorResponse("Request body is required", 400));
            }

            Map<String, Object> body;
            try {
                body = gson.fromJson(requestBody, Map.class);
            } catch (Exception e) {
                plugin.getLogger().warning("[API] Failed to parse JSON: " + e.getMessage());
                res.status(400);
                return gson.toJson(new ErrorResponse("Invalid JSON format: " + e.getMessage(), 400));
            }
            String target = (String) body.get("target");
            String targetType = (String) body.get("targetType");
            int points = ((Number) body.get("points")).intValue();
            String reason = (String) body.getOrDefault("reason", "manual");

            if (target == null || targetType == null) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Missing target or targetType", 400));
            }

            if ("driver".equals(targetType)) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(target);
                String uuid = player.getUniqueId().toString();
                
                plugin.getLogger().info("[API] Applying " + points + " points to driver " + target + " (" + uuid + ") - Reason: " + reason);

                PointEntry entry = new PointEntry("manual:" + reason, null, points, System.currentTimeMillis());
                league.addDriverPointEntry(uuid, entry);
                league.addPointsToDriver(uuid, points);

                int newTotal = league.getDriverPoints(uuid);
                plugin.getLogger().info("[API] Driver " + target + " new total: " + newTotal);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Applied " + points + " points to " + target + " (" + reason + ")");
                response.put("newTotal", newTotal);
                return gson.toJson(response);
            } else if ("team".equals(targetType)) {
                plugin.getLogger().info("[API] Applying " + points + " points to team " + target + " - Reason: " + reason);
                
                PointEntry entry = new PointEntry("manual:" + reason, null, points, System.currentTimeMillis());
                league.addTeamPointEntry(target, entry);
                league.addPointsToTeam(target, points);
                
                int newTotal = league.getTeamPoints(target);
                plugin.getLogger().info("[API] Team " + target + " new total: " + newTotal);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Applied " + points + " points to team " + target + " (" + reason + ")");
                response.put("newTotal", newTotal);
                return gson.toJson(response);
            } else {
                res.status(400);
                return gson.toJson(new ErrorResponse("Invalid targetType (must be 'driver' or 'team')", 400));
            }
        });

        // POST /api/league/{id}/standings (trigger update)
        post("/api/league/:id/standings", (req, res) -> {
            String leagueId = req.params(":id");
            plugin.getLogger().info("[API] POST /standings - League: " + leagueId);
            
            League league = TImingLeague.getLeagueMap().get(leagueId);

            if (league == null) {
                res.status(404);
                return gson.toJson(new ErrorResponse("League not found", 404));
            }

            plugin.getLogger().info("[API] Triggering standings update for league " + leagueId);
            league.updateStandings();
            plugin.getLogger().info("[API] Standings updated successfully");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Standings updated for league '" + leagueId + "'");
            response.put("timestamp", System.currentTimeMillis());
            return gson.toJson(response);
        });

        // POST /api/league/{id}/event
        post("/api/league/:id/event", (req, res) -> {
            String leagueId = req.params(":id");
            plugin.getLogger().info("[API] POST /event - League: " + leagueId);
            
            League league = TImingLeague.getLeagueMap().get(leagueId);

            if (league == null) {
                res.status(404);
                return gson.toJson(new ErrorResponse("League not found", 404));
            }

            String requestBody = req.body();
            plugin.getLogger().info("[API] Request body: " + requestBody);
            
            if (requestBody == null || requestBody.trim().isEmpty()) {
                plugin.getLogger().warning("[API] Empty request body");
                res.status(400);
                return gson.toJson(new ErrorResponse("Request body is required", 400));
            }
            
            Map<String, Object> body;
            try {
                body = gson.fromJson(requestBody, Map.class);
            } catch (Exception e) {
                plugin.getLogger().warning("[API] Failed to parse JSON: " + e.getMessage());
                res.status(400);
                return gson.toJson(new ErrorResponse("Invalid JSON format: " + e.getMessage(), 400));
            }
            
            String eventId = (String) body.get("eventId");
            boolean autoUpdate = body.containsKey("autoUpdate") && (boolean) body.get("autoUpdate");
            
            plugin.getLogger().info("[API] Event ID: " + eventId + ", Auto-update: " + autoUpdate);

            if (eventId == null || eventId.isEmpty()) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Missing eventId", 400));
            }

            plugin.getLogger().info("[API] Adding event " + eventId + " to league " + leagueId);
            league.addEvent(eventId);

            if (autoUpdate) {
                plugin.getLogger().info("[API] Auto-updating standings");
                league.updateStandings();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Event '" + eventId + "' added to league '" + leagueId + "'");
            response.put("autoUpdated", autoUpdate);
            return gson.toJson(response);
        });
    }

    // Build response objects
    private Map<String, Object> buildStandingsResponse(League league) {
        Map<String, Object> response = new HashMap<>();
        response.put("league", league.getName());
        response.put("mulliganCount", league.getMulliganCount());
        response.put("teamMulligansEnabled", league.isTeamMulligansEnabled());

        List<Map<String, Object>> drivers = league.getDriverStandings().entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(entry -> {
                    Map<String, Object> driver = new HashMap<>();
                    String uuid = entry.getKey();
                    OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));

                    driver.put("uuid", uuid);
                    driver.put("name", player.getName());
                    driver.put("points", entry.getValue());

                    Team team = league.getTeamByDriver(uuid);
                    driver.put("team", team != null ? team.getName() : "No Team");

                    List<String> mulliganed = league.getDriverMulliganedEvents().getOrDefault(uuid, new ArrayList<>());
                    driver.put("mulliganedEvents", mulliganed);

                    return driver;
                })
                .collect(Collectors.toList());

        // Add position
        for (int i = 0; i < drivers.size(); i++) {
            drivers.get(i).put("position", i + 1);
        }

        response.put("drivers", drivers);

        List<Map<String, Object>> teams = league.getTeamStandings().entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(entry -> {
                    Map<String, Object> teamData = new HashMap<>();
                    teamData.put("name", entry.getKey());
                    teamData.put("points", entry.getValue());

                    Team team = league.getTeam(entry.getKey());
                    if (team != null) {
                        teamData.put("color", team.getColor());

                        List<String> teamDrivers = new ArrayList<>();
                        team.getMainDrivers().forEach(teamDrivers::add);
                        team.getReserveDrivers().forEach(teamDrivers::add);
                        teamData.put("drivers", teamDrivers);
                    }

                    List<String> mulliganed = league.getTeamMulliganedEvents().getOrDefault(entry.getKey(), new ArrayList<>());
                    teamData.put("mulliganedEvents", mulliganed);

                    return teamData;
                })
                .collect(Collectors.toList());

        for (int i = 0; i < teams.size(); i++) {
            teams.get(i).put("position", i + 1);
        }

        response.put("teams", teams);
        response.put("lastUpdated", System.currentTimeMillis());

        return response;
    }

    private Map<String, Object> buildDriversResponse(League league) {
        Map<String, Object> response = new HashMap<>();
        response.put("league", league.getName());

        List<Map<String, Object>> drivers = new ArrayList<>();

        for (String uuid : league.getDrivers()) {
            Map<String, Object> driver = new HashMap<>();
            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));

            driver.put("uuid", uuid);
            driver.put("name", player.getName());
            driver.put("points", league.getDriverPoints(uuid));

            Team team = league.getTeamByDriver(uuid);
            driver.put("team", team != null ? team.getName() : "No Team");

            if (team != null) {
                if (team.isMain(uuid)) {
                    driver.put("role", "main");
                } else if (team.isReserve(uuid)) {
                    driver.put("role", "reserve");
                } else {
                    driver.put("role", team.getPriority(uuid));
                }
            }

            // Event breakdown
            List<PointEntry> entries = league.getDriverPointHistory().getOrDefault(uuid, new ArrayList<>());
            Map<String, Map<String, Object>> eventBreakdown = new HashMap<>();
            List<Map<String, Object>> manualAdjustments = new ArrayList<>();

            List<String> mulliganedEvents = league.getDriverMulliganedEvents().getOrDefault(uuid, new ArrayList<>());

            for (PointEntry entry : entries) {
                if (entry.getEventId() != null) {
                    if (!eventBreakdown.containsKey(entry.getEventId())) {
                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("points", 0);
                        eventData.put("mulliganed", mulliganedEvents.contains(entry.getEventId()));
                        eventBreakdown.put(entry.getEventId(), eventData);
                    }
                    Map<String, Object> eventData = eventBreakdown.get(entry.getEventId());
                    eventData.put("points", (int) eventData.get("points") + entry.getPoints());
                } else {
                    Map<String, Object> adjustment = new HashMap<>();
                    adjustment.put("reason", entry.getSource().replace("manual:", ""));
                    adjustment.put("points", entry.getPoints());
                    adjustment.put("timestamp", entry.getTimestamp());
                    manualAdjustments.add(adjustment);
                }
            }

            driver.put("eventBreakdown", eventBreakdown);
            driver.put("manualAdjustments", manualAdjustments);

            drivers.add(driver);
        }

        response.put("drivers", drivers);
        return response;
    }

    private Map<String, Object> buildTeamsResponse(League league) {
        Map<String, Object> response = new HashMap<>();
        response.put("league", league.getName());
        response.put("teamMode", league.getTeamMode().toString());

        List<Map<String, Object>> teams = new ArrayList<>();

        for (Team team : league.getTeams()) {
            if (team.getName().equals("No Team")) continue;

            Map<String, Object> teamData = new HashMap<>();
            teamData.put("name", team.getName());
            teamData.put("color", team.getColor());
            teamData.put("owner", team.getOwner());
            teamData.put("points", team.getPoints());
            teamData.put("mainDrivers", new ArrayList<>(team.getMainDrivers()));
            teamData.put("reserveDrivers", new ArrayList<>(team.getReserveDrivers()));
            teamData.put("maxMains", team.getMaxMains());
            teamData.put("maxReserves", team.getMaxReserves());

            teams.add(teamData);
        }

        response.put("teams", teams);
        return response;
    }

    private Map<String, Object> buildCalendarResponse(League league) {
        Map<String, Object> response = new HashMap<>();
        response.put("league", league.getName());

        List<Map<String, Object>> events = new ArrayList<>();
        for (String eventId : league.getCalendar()) {
            Map<String, Object> event = new HashMap<>();
            event.put("id", eventId);
            event.put("name", eventId);
            events.add(event);
        }

        response.put("events", events);
        return response;
    }

    private String getAllowedOrigin(spark.Request req) {
        List<String> allowed = config.getAllowedOrigins();
        if (allowed.contains("*")) {
            return "*";
        }

        String origin = req.headers("Origin");
        if (origin != null && allowed.contains(origin)) {
            return origin;
        }

        return allowed.isEmpty() ? "*" : allowed.get(0);
    }

    private boolean isRateLimited(String apiKey) {
        RateLimitBucket bucket = rateLimits.computeIfAbsent(apiKey, k -> new RateLimitBucket());

        long now = System.currentTimeMillis();
        long windowDuration = 60000; // 1 minute

        if (now - bucket.windowStart > windowDuration) {
            bucket.count = 1;
            bucket.windowStart = now;
            return false;
        }

        bucket.count++;
        return bucket.count > config.getRateLimit();
    }

    public void stop() {
        Spark.stop();
        plugin.getLogger().info("[API] REST API stopped");
    }

    private static class RateLimitBucket {
        int count = 0;
        long windowStart = System.currentTimeMillis();
    }
}