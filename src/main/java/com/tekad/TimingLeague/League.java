package com.tekad.TimingLeague;

import me.makkuusen.timing.system.api.EventResultsAPI;
import me.makkuusen.timing.system.api.event.DriverResult;
import me.makkuusen.timing.system.api.event.EventResult;
import me.makkuusen.timing.system.api.event.HeatResult;
import me.makkuusen.timing.system.api.event.RoundResult;

import java.util.*;

public class League {

    private final String name;
    private final Set<Team> teamsList = new HashSet<>();
    private final Set<String> calendar = new HashSet<>();
    private int predictedDriverCount;
    private ScoringSystem scoringSystem;
    private final Team NoTeam = new Team("No Team", "a8a8a8", this);

    private Map<String, Integer> driverStandings = new HashMap<>();
    private Map<String, Integer> teamStandings = new HashMap<>();
    private Map<String, Team> driversList = new HashMap<>();

    public League(String name, int predictedDriverCount) {
        this.name = name;
        this.predictedDriverCount = predictedDriverCount;
        teamsList.add(NoTeam);
    }

    public void setScoringSystem(ScoringSystem scoringSystem) {
        this.scoringSystem = scoringSystem;
    }

    public String getName() {
        return name;
    }

    public int getPredictedDriverCount() {
        return predictedDriverCount;
    }

    public Set<Team> getTeams() {
        return teamsList;
    }

    public void setPredictedDrivers(int num){
        predictedDriverCount = num;
    }

    public void addTeam(Team team) {
        teamsList.add(team);
    }

    public Set<String> getDrivers() {
        return driversList.keySet();
    }

    public void addDriver(String driver, Team team) {
        driversList.put(driver, team);
    }

    public Set<String> getCalendar() {
        return calendar;
    }

    public void addEvent(String event) {
        calendar.add(event);
    }

    public void addPointsToDriver(String uuid, int points){
        driverStandings.put(uuid, driverStandings.getOrDefault(uuid, 0) + points);
    }

    public void addPointsToTeam(String team, int points){
        teamStandings.put(team, teamStandings.getOrDefault(team, 0) + points);
    }

    public Team getTeamByDriver(String driver){
        return driversList.getOrDefault(driver, NoTeam);
    }

    public void updateStandingsFromEvents() {
        for (String event : calendar){
            EventResult results = EventResultsAPI.getEventResult(event);
            if (results == null || results.getRounds() == null) continue;

            for (RoundResult roundResult: results.getRounds()){
                for (HeatResult heat: roundResult.getHeatResults()){
                    List<DriverResult> drivers = heat.getDriverResultList();
                    int driverCount = drivers.size();

                    for (DriverResult driver : heat.getDriverResultList()){
                        String DriverUUID = driver.getUuid();

                        if (!driversList.containsKey(DriverUUID)){
                            addDriver(DriverUUID, NoTeam);
                        }

                        Team DriversTeam = this.getTeamByDriver(DriverUUID);

                        int points = scoringSystem.getPointsForPosition(driver.getPosition(), driverCount);
                        this.addPointsToDriver(DriverUUID, points);
                        this.addPointsToTeam(DriversTeam.getName(), points);
                    }
                }
            }
        }
    }
}
