package com.tekad.TimingLeague;

import com.tekad.TimingLeague.League;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Team {

    private String name;
    private final Set<String> members = new HashSet<>();
    private League league;
    private String color;
    private int maxMembers = 4;

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

    public void setColor(String color){this.color = color; }

    public Set<String> getMembers() {
        return members;
    }

    public League getLeague() {
        return league;
    }

    public boolean addMember(String uuid) {
        if (members.size() >= maxMembers) {
            return false;
        }
        return members.add(uuid); // returns false if already a member
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }
}
