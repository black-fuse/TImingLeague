package com.tekad.TimingLeague;

import java.util.HashSet;
import java.util.Set;

public class Team {

    private final String name;
    private final League league;
    private String color;

    private final Set<String> mainDrivers = new HashSet<>();
    private final Set<String> reserveDrivers = new HashSet<>();

    private String teamOwner;
    private int points;

    private final int maxMains = 2;
    private final int maxReserves = 2;

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
        return mainDrivers.remove(uuid) || reserveDrivers.remove(uuid);
    }

    public boolean isMember(String uuid) {
        return mainDrivers.contains(uuid) || reserveDrivers.contains(uuid);
    }

    public boolean isMain(String uuid) {
        return mainDrivers.contains(uuid);
    }

    public boolean isReserve(String uuid) {
        return reserveDrivers.contains(uuid);
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
