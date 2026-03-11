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
        // Clear existing point history and standings
        league.clearPointHistory();
        league.getDriverStandings().clear();
        league.getTeamStandings().clear();

        Set<String> calendar = league.getCalendar();

        for (String event : calendar){
            updateStandingsWithSingleEvent(event, league);
        }

        // Apply mulligans and recalculate final standings
        recalculateStandingsWithMulligans(league);
    }

    public void updateStandingsWithSingleEvent(String event, League league){
        EventResult results = EventResultsAPI.getEventResult(event);
        if (results == null || results.getRounds() == null){
            return;
        }

        for (RoundResult roundResult: results.getRounds()){
            for (HeatResult heat: roundResult.getHeatResults()){
                // Only process FINAL heats (skip qualifiers)
                if (isFinalHeat(heat)) {
                    processHeatWithTracking(heat, event, league);
                }
            }
        }
    }

    private boolean isFinalHeat(HeatResult heat) {
        String heatName = heat.getName().toLowerCase();
        return heatName.contains("f") && !heatName.contains("q");
    }

    public void processHeatWithTracking(HeatResult heat, String eventId, League league) {
        Map<String, Team> driversList = league.getDriversList();
        List<DriverResult> drivers = heat.getDriverResultList();
        int driverCount = drivers.size();
        String source = "heat:" + eventId + ":" + heat.getName();
        long timestamp = System.currentTimeMillis();

        // Award individual points to all drivers
        for (DriverResult driver : drivers) {
            String driverUUID = driver.getUuid();

            if (!driversList.containsKey(driverUUID)) {
                league.addDriver(driverUUID, league.NoTeam);
            }

            int points = league.getScoringSystem().getPointsForPosition(driver.getPosition(), driverCount);
            
            // Track point entry
            PointEntry entry = new PointEntry(source, eventId, points, timestamp);
            league.addDriverPointEntry(driverUUID, entry);
        }

        // Handle team points
        if (league.getTeamMode() == TeamMode.PRIORITY){
            for (Team team : league.getTeams()){
                List<String> priorityDrivers = team.getPriorityDrivers();
                int scorerCount = team.getCountedPrioDrivers();
                int scored = 0;

                for (String driverUUID : priorityDrivers){
                    if (scored >= scorerCount) break;

                    DriverResult result = drivers.stream()
                            .filter(d -> d.getUuid().equals(driverUUID))
                            .findFirst()
                            .orElse(null);

                    if (result != null){
                        int points = league.getScoringSystem().getPointsForPosition(result.getPosition(), driverCount);
                        PointEntry entry = new PointEntry(source, eventId, points, timestamp);
                        league.addTeamPointEntry(team.getName(), entry);
                        scored++;
                    }
                }
            }
        }
        else if (league.getTeamMode() == TeamMode.HIGHEST) {
            for (Team team : league.getTeams()) {
                int maxScorers = team.getCountedPrioDrivers();

                List<DriverResult> teamResults = drivers.stream()
                        .filter(d -> team.getPriorityDrivers().contains(d.getUuid()))
                        .sorted(Comparator.comparingInt(DriverResult::getPosition))
                        .limit(maxScorers)
                        .toList();

                for (DriverResult result : teamResults) {
                    int points = league.getScoringSystem()
                            .getPointsForPosition(result.getPosition(), driverCount);
                    PointEntry entry = new PointEntry(source, eventId, points, timestamp);
                    league.addTeamPointEntry(team.getName(), entry);
                }
            }
        }
        else{
            for (Team team : league.getTeams()) {
                Set<String> mainDrivers = team.getMainDrivers();
                Set<String> reserveDrivers = team.getReserveDrivers();

                int missingMains = 0;

                // Award points for mains
                for (String driverUUID : mainDrivers) {
                    DriverResult result = drivers.stream()
                            .filter(d -> d.getUuid().equals(driverUUID))
                            .findFirst()
                            .orElse(null);

                    if (result != null) {
                        int points = league.getScoringSystem().getPointsForPosition(result.getPosition(), driverCount);
                        PointEntry entry = new PointEntry(source, eventId, points, timestamp);
                        league.addTeamPointEntry(team.getName(), entry);
                    } else {
                        missingMains++;
                    }
                }

                // Fill missing slots with reserves
                int filledReserves = 0;
                for (String driverUUID : reserveDrivers) {
                    if (filledReserves >= missingMains) break;

                    DriverResult result = drivers.stream()
                            .filter(d -> d.getUuid().equals(driverUUID))
                            .findFirst()
                            .orElse(null);

                    if (result != null) {
                        int points = league.getScoringSystem().getPointsForPosition(result.getPosition(), driverCount);
                        PointEntry entry = new PointEntry(source, eventId, points, timestamp);
                        league.addTeamPointEntry(team.getName(), entry);
                        filledReserves++;
                    }
                }
            }
        }
    }

    public HeatResult getHeatResults(String event, String heatId) {
        // Validate heatId format (e.g., r1q2, r2f1)
        if (!heatId.matches("r\\d+[fq]\\d+")) {
            throw new IllegalArgumentException("Invalid heat ID format. Expected formats like r1q2 or r2f1.");
        }

        // Extract components
        int roundStart = 1; // after 'r'
        int typeIndex = heatId.indexOf('f') != -1 ? heatId.indexOf('f') : heatId.indexOf('q');
        char heatType = heatId.charAt(typeIndex);
        int roundNumber = Integer.parseInt(heatId.substring(roundStart, typeIndex));
        int heatIndex = Integer.parseInt(heatId.substring(typeIndex + 1));

        // Get event result
        EventResult eventResult = EventResultsAPI.getEventResult(event);
        if (eventResult == null) {
            throw new IllegalArgumentException("Event not found: " + event);
        }

        List<RoundResult> rounds = eventResult.getRounds();
        if (roundNumber < 1 || roundNumber > rounds.size()) {
            throw new IllegalArgumentException("Invalid round number: " + roundNumber);
        }

        RoundResult round = rounds.get(roundNumber - 1); // zero-based indexing

        List<HeatResult> heats = round.getHeatResults();
        List<HeatResult> filteredHeats = new ArrayList<>();

        for (HeatResult heat : heats) {
            String name = heat.getName().toLowerCase();
            if (name.matches("r\\d+" + heatType + "\\d+")) {
                filteredHeats.add(heat);
            }
        }

        if (heatIndex < 1 || heatIndex > filteredHeats.size()) {
            throw new IllegalArgumentException("Heat index " + heatIndex + " not found for type '" + heatType + "' in round " + roundNumber);
        }

        return filteredHeats.get(heatIndex - 1); // again, zero-based indexing
    }

    private void recalculateStandingsWithMulligans(League league) {
        int mulliganCount = league.getMulliganCount();
        boolean teamMulligansEnabled = league.isTeamMulligansEnabled();

        // Recalculate driver standings with mulligans
        for (String uuid : league.getDriversList().keySet()) {
            List<PointEntry> entries = league.getDriverPointHistory().getOrDefault(uuid, new ArrayList<>());
            
            // Group by event
            Map<String, Integer> eventTotals = new HashMap<>();
            int manualTotal = 0;

            for (PointEntry entry : entries) {
                if (entry.getEventId() != null) {
                    eventTotals.merge(entry.getEventId(), entry.getPoints(), Integer::sum);
                } else {
                    // Manual adjustments are immune to mulligans
                    manualTotal += entry.getPoints();
                }
            }

            // Apply mulligans
            List<String> mulliganedEvents;
            if (mulliganCount > 0 && eventTotals.size() > mulliganCount) {
                mulliganedEvents = eventTotals.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue()) // worst first
                        .limit(mulliganCount)
                        .map(Map.Entry::getKey)
                        .toList();
            } else {
                mulliganedEvents = new ArrayList<>();
            }

            // Calculate final total
            int total = eventTotals.entrySet().stream()
                    .filter(e -> !mulliganedEvents.contains(e.getKey()))
                    .mapToInt(Map.Entry::getValue)
                    .sum();

            total += manualTotal;

            league.setDriverPoints(uuid, total);
            league.setDriverMulliganedEvents(uuid, mulliganedEvents);
        }

        // Recalculate team standings with mulligans (if enabled)
        if (teamMulligansEnabled) {
            for (Team team : league.getTeams()) {
                List<PointEntry> entries = league.getTeamPointHistory().getOrDefault(team.getName(), new ArrayList<>());

                // Group by event
                Map<String, Integer> eventTotals = new HashMap<>();
                int manualTotal = 0;

                for (PointEntry entry : entries) {
                    if (entry.getEventId() != null) {
                        eventTotals.merge(entry.getEventId(), entry.getPoints(), Integer::sum);
                    } else {
                        manualTotal += entry.getPoints();
                    }
                }

                // Apply mulligans
                List<String> mulliganedEvents;
                if (mulliganCount > 0 && eventTotals.size() > mulliganCount) {
                    mulliganedEvents = eventTotals.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue())
                            .limit(mulliganCount)
                            .map(Map.Entry::getKey)
                            .toList();
                } else {
                    mulliganedEvents = new ArrayList<>();
                }

                // Calculate final total
                int total = eventTotals.entrySet().stream()
                        .filter(e -> !mulliganedEvents.contains(e.getKey()))
                        .mapToInt(Map.Entry::getValue)
                        .sum();

                total += manualTotal;

                league.setTeamPoints(team.getName(), total);
                league.setTeamMulliganedEvents(team.getName(), mulliganedEvents);
            }
        } else {
            // Team mulligans disabled - just sum all points
            for (Team team : league.getTeams()) {
                List<PointEntry> entries = league.getTeamPointHistory().getOrDefault(team.getName(), new ArrayList<>());
                int total = entries.stream().mapToInt(PointEntry::getPoints).sum();
                league.setTeamPoints(team.getName(), total);
            }
        }
    }


}
