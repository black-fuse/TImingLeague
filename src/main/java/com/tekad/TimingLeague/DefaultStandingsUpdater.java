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
        Set<String> calendar = league.getCalendar();
        Map<String, Team> driversList = league.getDriversList();

        for (String event : calendar){
            updateStandingsWithSingleEvent(event, league);
        }
    }

    public void updateStandingsWithSingleEvent(String event, League league){
        Set<String> calendar = league.getCalendar();
        Map<String, Team> driversList = league.getDriversList();

        EventResult results = EventResultsAPI.getEventResult(event);
        if (results == null || results.getRounds() == null){
            //keep for now
            league = league;
        }

        assert results != null;
        for (RoundResult roundResult: results.getRounds()){
            for (HeatResult heat: roundResult.getHeatResults()){
                updateStandingsWithHeat(heat, league);

            }
        }
    }

    public void updateStandingsWithHeat(HeatResult heat, League league) {
        Map<String, Team> driversList = league.getDriversList();
        List<DriverResult> drivers = heat.getDriverResultList();
        int driverCount = drivers.size();

        // Award individual points to all drivers
        for (DriverResult driver : drivers) {
            String driverUUID = driver.getUuid();

            if (!driversList.containsKey(driverUUID)) {
                league.addDriver(driverUUID, league.NoTeam);
            }

            int points = league.getScoringSystem().getPointsForPosition(driver.getPosition(), driverCount);
            league.addPointsToDriver(driverUUID, points);
        }

        // Handle team points
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
                    league.addPointsToTeam(team.getName(), points);
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
                    league.addPointsToTeam(team.getName(), points);
                    filledReserves++;
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


}
