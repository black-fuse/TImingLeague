package com.tekad.TimingLeague.API.dto;

import com.tekad.TimingLeague.Team;

public class TeamStandingDTO {

    public String team;

    public int points;

    public TeamStandingDTO(Team team){
        this.team = team.getName();
        this.points = team.getPoints();
    }
}
