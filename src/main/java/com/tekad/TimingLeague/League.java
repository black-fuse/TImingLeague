package com.tekad.TimingLeague;

import me.makkuusen.timing.system.api.DriverDetails;
import me.makkuusen.timing.system.event.Event;

import java.util.HashSet;
import java.util.Set;

public class League {

    private String name;
    private final Set<Team> teamsList = new HashSet<>();
    private final Set<DriverDetails> driversList = new HashSet<>();
    private final Set<Event> calendar = new HashSet<>();
    private int predictedDriverCount;
    private ScoringSystem scoringSystem;

    public League(String name, int predictedDriverCount) {
        this.name = name;
        this.predictedDriverCount = predictedDriverCount;
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

    public void addTeam(Team team) {
        teamsList.add(team);
    }

    public Set<DriverDetails> getDrivers() {
        return driversList;
    }

    public void addDriver(DriverDetails driver) {
        driversList.add(driver);
    }

    public Set<Event> getCalendar() {
        return calendar;
    }

    public void addEvent(Event event) {
        calendar.add(event);
    }
}
