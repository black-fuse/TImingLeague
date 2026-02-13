package com.tekad.TimingLeague;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Team {

    private final String name;
    private final League league;
    private String color;

    // MAIN_RESERVE team mode
    private final Set<String> mainDrivers = new HashSet<>();
    private final Set<String> reserveDrivers = new HashSet<>();

    // PRIORITY team mode
    private final List<String> priorityDrivers = new ArrayList<>();

    private String teamOwner;
    private int points;

    @Getter @Setter
    private int maxMains = 2;
    @Getter @Setter
    private int maxReserves = 2;

    private int countedPrioDrivers = 2;

    public Team(String name, String color, League league) {
        this.name = name;
        this.color = color;
        this.league = league;
    }

    // ======== Getters ========

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public League getLeague() {
        return league;
    }

    public String getOwner() {
        return teamOwner;
    }

    public boolean isOwner(String uuid) {
        return teamOwner != null && teamOwner.equalsIgnoreCase(uuid);
    }

    public Set<String> getMainDrivers() {
        return new HashSet<>(mainDrivers);
    }

    public Set<String> getReserveDrivers() {
        return new HashSet<>(reserveDrivers);
    }

    public List<String> getPriorityDrivers(){return new ArrayList<>(priorityDrivers);}

    public int getCountedPrioDrivers() {return countedPrioDrivers;}

    public void setCountedPrioDrivers(int count) {countedPrioDrivers = count;}

    public List<String> getDriversWithinPrio(){
        int n = 0;
        List<String> countedDrivers = new ArrayList<>();

        for (String driver : priorityDrivers){
            if (n >= countedPrioDrivers){
                break;
            }

            countedDrivers.add(driver);
            n++;
        }

        return countedDrivers;
    }

    public Set<String> getMembers() {
        Set<String> all = new HashSet<>();
        all.addAll(mainDrivers);
        all.addAll(reserveDrivers);
        return all;
    }

    public void setOwner(String uuid) {
        this.teamOwner = uuid;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points){
        this.points = points;
    }

    // ======== Membership Handling ========

    public boolean addDriver(String uuid, int priority){
        return switch(league.getTeamMode()){
            case MAIN_RESERVE -> {
                if (priority == 1){
                    yield addMainDriver(uuid);
                }
                else {
                    yield addReserveDriver(uuid);
                }
            }
            case PRIORITY -> addPriorityDriver(uuid, priority);
            case HIGHEST -> addPriorityDriver(uuid, priority);
        };
    }

    public boolean addPriorityDriver(String uuid, int priority){
        priorityDrivers.remove(uuid);
        if (priorityDrivers.size() >= maxMains){
            return false;
        }

        int index = Math.max(0, Math.min(priority, priorityDrivers.size()));

        if (index >= priorityDrivers.size()){
            return priorityDrivers.add(uuid);
        }
        else {
            priorityDrivers.add(index, uuid);
            return true;
        }
    }

    public boolean addMainDriver(String uuid) {
        if (mainDrivers.size() >= maxMains || getMembers().contains(uuid)) {
            return false;
        }
        return mainDrivers.add(uuid);
    }

    public boolean addReserveDriver(String uuid) {
        if (reserveDrivers.size() >= maxReserves || getMembers().contains(uuid)) {
            return false;
        }
        return reserveDrivers.add(uuid);
    }

    public boolean removeMember(String uuid) {
        return mainDrivers.remove(uuid) || reserveDrivers.remove(uuid) || priorityDrivers.remove(uuid);
    }

    public boolean isMember(String uuid) {
        return mainDrivers.contains(uuid) || reserveDrivers.contains(uuid) || priorityDrivers.contains(uuid);
    }

    public boolean isMain(String uuid) {
        return mainDrivers.contains(uuid);
    }

    public boolean isReserve(String uuid) {
        return reserveDrivers.contains(uuid);
    }

    public int getPriority(String uuid){
        int n = 0;

        for (String driver : priorityDrivers){
            n++;
            if (driver.equals(uuid)) break;
        }

        return n;
    }

    public boolean promoteToMain(String uuid) {
        if (reserveDrivers.contains(uuid) && mainDrivers.size() < maxMains) {
            reserveDrivers.remove(uuid);
            mainDrivers.add(uuid);
            return true;
        }
        return false;
    }

    public boolean demoteToReserve(String uuid) {
        if (mainDrivers.contains(uuid) && reserveDrivers.size() < maxReserves) {
            mainDrivers.remove(uuid);
            reserveDrivers.add(uuid);
            return true;
        }
        return false;
    }
}
