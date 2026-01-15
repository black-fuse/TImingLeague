package com.tekad.TimingLeague;

import com.tekad.TimingLeague.ScoringSystems.BasicScoringSystem;
import com.tekad.TimingLeague.ScoringSystems.ScoringSystem;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

public class League {

    @Getter
    private final String name;
    private final Set<Team> teamsList = new HashSet<>();
    @Getter
    private final Set<String> calendar = new HashSet<>();
    @Getter
    private int predictedDriverCount;
    @Getter
    @Setter
    private ScoringSystem scoringSystem = new BasicScoringSystem();
    public final Team NoTeam = new Team("No Team", "a8a8a8", this);
    public int flapPoints = 0;

    private final Map<String, Integer> driverStandings = new HashMap<>();
    private final Map<String, Integer> teamStandings = new HashMap<>();
    @Getter
    private final Map<String, Team> driversList = new HashMap<>();

    private StandingsUpdater updater = new DefaultStandingsUpdater();

    @Getter @Setter
    private TeamMode teamMode = TeamMode.MAIN_RESERVE;
    @Getter @Setter
    private int teamMaxSize = 4;
    @Getter @Setter
    private int teamScoringCount = 2;

    public League(String name, int predictedDriverCount) {
        this.name = name;
        this.predictedDriverCount = predictedDriverCount;
        teamsList.add(NoTeam);
    }

    public void updateStandings() {
        updater.updateStandingsFromEvents(this);
    }

    public StandingsUpdater getUpdater(){
        return updater;
    }

    public void setUpdater(StandingsUpdater updater){
        this.updater = updater;
    }

    public Set<Team> getTeams() {
        return teamsList;
    }

    public Set<String> getTeamsString(){
        Set<String> list = new HashSet<>();
        for (Team team : teamsList){
            list.add(team.getName());
        }

        return list;
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

    public int getDriverPoints(String driver){
        return driverStandings.getOrDefault(driver, 0);
    }

    public void setDriverPoints(String driver, int points){
        driverStandings.put(driver, points);
    }

    public Map<String, Integer> getDriverStandings(){
        return  driverStandings;
    }

    public void addDriver(String driver, Team team) {
        driversList.put(driver, team);
    }

    public void addEvent(String event) {
        calendar.add(event);
    }

    private void setFlapPoints(int points){
        flapPoints = points;
    }

    public boolean addMainDriverToTeam(String uuid, String teamName){
        Team team = getTeam(teamName);
        Boolean success = team.addMainDriver(uuid);
        if (success){
            driversList.put(uuid, team);
        }

        return success;
    }

    public boolean addReserveDriverToTeam(String uuid, String teamName){
        Team team = getTeam(teamName);
        Boolean success = team.addReserveDriver(uuid);
        if (success){
            driversList.put(uuid, team);
        }

        return success;
    }

    public boolean addDriverToTeam(String uuid, String teamName, int priority){
        Team team = getTeam(teamName);
        Boolean success;
        if (teamMode == TeamMode.HIGHEST || teamMode == TeamMode.PRIORITY){

            success = team.addDriver(uuid, priority);

        } else{
            success = team.addMainDriver(uuid);

            if (!success){
                team.addReserveDriver(uuid);
            }
        }

        if (success){
            driversList.put(uuid, team);
        }

        return success;
    }

    public void addPointsToDriver(String uuid, int points){
        driverStandings.put(uuid, driverStandings.getOrDefault(uuid, 0) + points);
    }

    public void addPointsToTeam(String team, int points){
        teamStandings.put(team, teamStandings.getOrDefault(team, 0) + points);

        for (Team daTeam : teamsList){
            if (team.equals(daTeam.getName())){
                daTeam.setPoints(teamStandings.get(team));
            }
        }
    }

    public void setTeamPoints(String team, int points){
        teamStandings.put(team, points);

        for (Team daTeam : teamsList){
            if (team.equals(daTeam.getName())){
                daTeam.setPoints(teamStandings.get(team));
            }
        }
    }

    public Team getTeamByDriver(String driver){
        return driversList.getOrDefault(driver, NoTeam);
    }

    public Team getTeam(String teamName) {
        for (Team team : teamsList) {
            if (team.getName().equalsIgnoreCase(teamName)) {
                return team;
            }
        }
        return null;
    }


    public Map<String, Integer> getTeamStandings() {
        return  teamStandings;
    }
}
