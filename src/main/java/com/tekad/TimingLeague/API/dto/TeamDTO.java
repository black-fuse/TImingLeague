package com.tekad.TimingLeague.API.dto;

import com.tekad.TimingLeague.Team;

import java.util.List;
import java.util.Set;

public class TeamDTO {

    public String name;

    public String color;

    public String owner;

    public int points;

    public Set<String> mainDrivers;

    public Set<String> reserveDrivers;

    public List<String> priorityDrivers;

    public TeamDTO(Team team){
        this.name = team.getName();
        this.color = team.getColor();
        this.owner = team.getOwner();
        this.points = team.getPoints();
        this.mainDrivers = team.getMainDrivers();
        this.reserveDrivers = team.getReserveDrivers();
        this.priorityDrivers = team.getPriorityDrivers();
    }
}
