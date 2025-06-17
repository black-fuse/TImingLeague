package com.tekad.TimingLeague;

import com.tekad.TimingLeague.League;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Team {

    private String name;
    private final Set<UUID> members = new HashSet<>();
    private League league;
    private String color;

    public Team(String name, String color, League league) {
        this.name = name;
        this.color = color;
        this.league = league;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public League getLeague() {
        return league;
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }
}
