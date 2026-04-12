package com.tekad.TimingLeague;

import me.makkuusen.timing.system.api.EventResultsAPI;
import me.makkuusen.timing.system.api.event.DriverResult;
import me.makkuusen.timing.system.api.event.EventResult;
import me.makkuusen.timing.system.api.event.HeatResult;
import me.makkuusen.timing.system.api.event.RoundResult;

import java.util.*;

public class DefaultStandingsUpdater implements StandingsUpdater {

    @Override
    public void updateStandingsFromEvents(League league) {
        league.clearPointHistory();
        league.getDriverStandings().clear();
        league.getTeamStandings().clear();

        for (CalendarEntry entry : league.getCalendarEntries()) {
            updateStandingsWithSingleEvent(entry.getEventName(), league);
        }

        recalculateStandingsWithMulligans(league);
    }

    public void updateStandingsWithSingleEvent(String event, League league) {
        EventResult results = EventResultsAPI.getEventResult(event);
        if (results == null || results.getRounds() == null) return;

        CalendarEntry calEntry = league.getCalendarEntry(event);

        if (calEntry != null && calEntry.hasPinnedHeat()) {
            // Score only the specific pinned heat
            try {
                HeatResult heat = getHeatResults(event, calEntry.getHeatId());
                processHeatWithTracking(heat, event, league);
            } catch (IllegalArgumentException e) {
                org.bukkit.Bukkit.getLogger().warning("[TimingLeague] Pinned heat '"
                        + calEntry.getHeatId() + "' not found for event '" + event + "': " + e.getMessage());
            }
        } else {
            // Legacy: scan all final heats
            for (RoundResult roundResult : results.getRounds()) {
                for (HeatResult heat : roundResult.getHeatResults()) {
                    if (isFinalHeat(heat)) {
                        processHeatWithTracking(heat, event, league);
                    }
                }
            }
        }
    }

    private boolean isFinalHeat(HeatResult heat) {
        String heatName = heat.getName().toLowerCase();
        return heatName.contains("f") && !heatName.contains("q");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Heat processing — uses per-event scoring system from category
    // ─────────────────────────────────────────────────────────────────────────

    public void processHeatWithTracking(HeatResult heat, String eventId, League league) {
        Map<String, Team> driversList = league.getDriversList();
        List<DriverResult> drivers = heat.getDriverResultList();
        int driverCount = drivers.size();
        String source = "heat:" + eventId + ":" + heat.getName();
        long timestamp = System.currentTimeMillis();

        // Resolve scoring system: category override or league default
        var scoringSystem = league.getScoringSystemForEvent(eventId);

        // ── Driver points ─────────────────────────────────────────────────────
        if (league.isDriverStandingsEnabled()) {
            for (DriverResult driver : drivers) {
                String driverUUID = driver.getUuid();
                if (!driversList.containsKey(driverUUID)) {
                    league.addDriver(driverUUID, league.NoTeam);
                }
                int points = scoringSystem.getPointsForPosition(driver.getPosition(), driverCount);
                league.addDriverPointEntry(driverUUID, new PointEntry(source, eventId, points, timestamp));
            }
        }

        // ── Team points ───────────────────────────────────────────────────────
        if (!league.isTeamStandingsEnabled()) return;

        switch (league.getTeamMode()) {
            case PRIORITY -> {
                for (Team team : league.getTeams()) {
                    List<String> priorityDrivers = team.getPriorityDrivers();
                    int scorerCount = team.getCountedPrioDrivers();
                    int scored = 0;
                    for (String driverUUID : priorityDrivers) {
                        if (scored >= scorerCount) break;
                        DriverResult result = findResult(drivers, driverUUID);
                        if (result != null) {
                            int points = scoringSystem.getPointsForPosition(result.getPosition(), driverCount);
                            league.addTeamPointEntry(team.getName(), new PointEntry(source, eventId, points, timestamp));
                            scored++;
                        }
                    }
                }
            }
            case HIGHEST -> {
                for (Team team : league.getTeams()) {
                    int maxScorers = team.getCountedPrioDrivers();
                    drivers.stream()
                            .filter(d -> team.getPriorityDrivers().contains(d.getUuid()))
                            .sorted(Comparator.comparingInt(DriverResult::getPosition))
                            .limit(maxScorers)
                            .forEach(result -> {
                                int points = scoringSystem.getPointsForPosition(result.getPosition(), driverCount);
                                league.addTeamPointEntry(team.getName(), new PointEntry(source, eventId, points, timestamp));
                            });
                }
            }
            default -> { // MAIN_RESERVE
                for (Team team : league.getTeams()) {
                    Set<String> mains = team.getMainDrivers();
                    Set<String> reserves = team.getReserveDrivers();
                    int missingMains = 0;

                    for (String driverUUID : mains) {
                        DriverResult result = findResult(drivers, driverUUID);
                        if (result != null) {
                            int points = scoringSystem.getPointsForPosition(result.getPosition(), driverCount);
                            league.addTeamPointEntry(team.getName(), new PointEntry(source, eventId, points, timestamp));
                        } else {
                            missingMains++;
                        }
                    }

                    int filled = 0;
                    for (String driverUUID : reserves) {
                        if (filled >= missingMains) break;
                        DriverResult result = findResult(drivers, driverUUID);
                        if (result != null) {
                            int points = scoringSystem.getPointsForPosition(result.getPosition(), driverCount);
                            league.addTeamPointEntry(team.getName(), new PointEntry(source, eventId, points, timestamp));
                            filled++;
                        }
                    }
                }
            }
        }
    }

    private DriverResult findResult(List<DriverResult> drivers, String uuid) {
        return drivers.stream().filter(d -> d.getUuid().equals(uuid)).findFirst().orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mulligan recalculation — category-aware
    // ─────────────────────────────────────────────────────────────────────────

    private void recalculateStandingsWithMulligans(League league) {
        if (league.isDriverStandingsEnabled()) {
            recalculateDriverStandings(league);
        }
        if (league.isTeamStandingsEnabled()) {
            recalculateTeamStandings(league);
        }
    }

    private void recalculateDriverStandings(League league) {
        for (String uuid : league.getDriversList().keySet()) {
            List<PointEntry> entries = league.getDriverPointHistory().getOrDefault(uuid, new ArrayList<>());
            int total = applyMulligans(entries, league, uuid, true);
            league.setDriverPoints(uuid, total);
        }
    }

    private void recalculateTeamStandings(League league) {
        boolean teamMulligans = league.isTeamMulligansEnabled();

        for (Team team : league.getTeams()) {
            List<PointEntry> entries = league.getTeamPointHistory().getOrDefault(team.getName(), new ArrayList<>());

            if (teamMulligans) {
                int total = applyMulligans(entries, league, team.getName(), false);
                league.setTeamPoints(team.getName(), total);
            } else {
                // No mulligans — straight sum
                int total = entries.stream().mapToInt(PointEntry::getPoints).sum();
                league.setTeamPoints(team.getName(), total);
            }
        }
    }

    /**
     * Applies category-aware mulligans to a list of point entries and returns
     * the final total. Also writes mulligan tracking back to the league for
     * breakdown display.
     *
     * Strategy:
     *  1. Split entries by category (using the league's calendar for lookup).
     *  2. Manual adjustments (null eventId) are always immune.
     *  3. For each category pool: drop the N worst events (N = category.mulliganCount).
     *  4. Uncategorised events use the league-level mulliganCount as a pool.
     *  5. Sum across all pools → final total.
     */
    private int applyMulligans(List<PointEntry> entries, League league, String targetId, boolean isDriver) {
        // Separate manual adjustments
        List<PointEntry> manual = new ArrayList<>();
        // Pool: categoryId (or "__uncategorised__") → eventName → total points
        Map<String, Map<String, Integer>> categoryPools = new LinkedHashMap<>();

        for (PointEntry entry : entries) {
            if (entry.getEventId() == null) {
                manual.add(entry);
                continue;
            }
            String catKey = getCategoryKey(entry.getEventId(), league);
            categoryPools
                .computeIfAbsent(catKey, k -> new HashMap<>())
                .merge(entry.getEventId(), entry.getPoints(), Integer::sum);
        }

        int manualTotal = manual.stream().mapToInt(PointEntry::getPoints).sum();
        int raceTotal = 0;
        List<String> allMulliganedEvents = new ArrayList<>();

        for (Map.Entry<String, Map<String, Integer>> poolEntry : categoryPools.entrySet()) {
            String catKey = poolEntry.getKey();
            Map<String, Integer> eventTotals = poolEntry.getValue();

            int mulliganCount = getMulliganCountForPool(catKey, league);

            List<String> mulliganedInPool;
            if (mulliganCount > 0 && eventTotals.size() > mulliganCount) {
                mulliganedInPool = eventTotals.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue())   // worst first
                        .limit(mulliganCount)
                        .map(Map.Entry::getKey)
                        .toList();
            } else {
                mulliganedInPool = new ArrayList<>();
            }

            allMulliganedEvents.addAll(mulliganedInPool);

            raceTotal += eventTotals.entrySet().stream()
                    .filter(e -> !mulliganedInPool.contains(e.getKey()))
                    .mapToInt(Map.Entry::getValue)
                    .sum();
        }

        // Write mulligan tracking back to league for breakdown display
        if (isDriver) {
            league.setDriverMulliganedEvents(targetId, allMulliganedEvents);
        } else {
            league.setTeamMulliganedEvents(targetId, allMulliganedEvents);
        }

        return raceTotal + manualTotal;
    }

    /**
     * Returns the pool key for a given event: the category id if the event
     * has one, otherwise "__uncategorised__".
     */
    private String getCategoryKey(String eventId, League league) {
        CalendarEntry entry = league.getCalendarEntry(eventId);
        if (entry != null && entry.hasCategory()) return entry.getCategoryId();
        return "__uncategorised__";
    }

    private int getMulliganCountForPool(String catKey, League league) {
        if (catKey.equals("__uncategorised__")) return league.getMulliganCount();
        EventCategory cat = league.getCategory(catKey);
        return cat != null ? cat.getMulliganCount() : 0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Heat lookup utility (used by updateWithHeat command)
    // ─────────────────────────────────────────────────────────────────────────

    public HeatResult getHeatResults(String event, String heatId) {
        if (!heatId.matches("r\\d+[fq]\\d+")) {
            throw new IllegalArgumentException("Invalid heat ID format. Expected formats like r1q2 or r2f1.");
        }

        int typeIndex = heatId.indexOf('f') != -1 ? heatId.indexOf('f') : heatId.indexOf('q');
        char heatType = heatId.charAt(typeIndex);
        int roundNumber = Integer.parseInt(heatId.substring(1, typeIndex));
        int heatIndex = Integer.parseInt(heatId.substring(typeIndex + 1));

        EventResult eventResult = EventResultsAPI.getEventResult(event);
        if (eventResult == null) throw new IllegalArgumentException("Event not found: " + event);

        List<RoundResult> rounds = eventResult.getRounds();
        if (roundNumber < 1 || roundNumber > rounds.size()) {
            throw new IllegalArgumentException("Invalid round number: " + roundNumber);
        }

        RoundResult round = rounds.get(roundNumber - 1);
        List<HeatResult> filteredHeats = new ArrayList<>();
        for (HeatResult heat : round.getHeatResults()) {
            String name = heat.getName().toLowerCase();
            if (name.matches("r\\d+" + heatType + "\\d+")) filteredHeats.add(heat);
        }

        if (heatIndex < 1 || heatIndex > filteredHeats.size()) {
            throw new IllegalArgumentException("Heat index " + heatIndex + " not found for type '" + heatType + "'");
        }
        return filteredHeats.get(heatIndex - 1);
    }
}
