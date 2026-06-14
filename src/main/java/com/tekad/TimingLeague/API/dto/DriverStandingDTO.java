package com.tekad.TimingLeague.API.dto;

import com.tekad.TimingLeague.League;

import java.util.Map;

public class DriverStandingDTO {
    public String uuid;

    public int points;

    public String team;

    public DriverStandingDTO(League league, String uuid){
        this.uuid = uuid;
        this.points = league.getDriverPoints(uuid);
        this.team = league.getTeamByDriver(uuid).getName();

    }
}
