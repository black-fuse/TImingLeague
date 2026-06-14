package com.tekad.TimingLeague.API.dto;

import com.tekad.TimingLeague.CalendarEntry;
import com.tekad.TimingLeague.League;

import java.util.List;

public class LeagueDTO {
    public String name;

    public int predictedDriverCount;

    public String scoringSystem;

    public boolean driverStandingsEnabled;
    public boolean teamStandingsEnabled;

    public String teamMode;

    public int teamMaxSize;
    public int teamScoringCount;

    public int mulliganCount;

    public List<CalendarEntryDTO> calendar;

    public List<CategoryDTO> categories;

    public LeagueDTO(League league) {
        this.name = league.getName();
        this.scoringSystem = league.getScoringSystem().getName();
        this.predictedDriverCount = league.getPredictedDriverCount();
        this.driverStandingsEnabled = league.isDriverStandingsEnabled();
        this.teamStandingsEnabled = league.isTeamStandingsEnabled();
        this.teamMode = league.getTeamMode().toString();
        this.teamMaxSize = league.getTeamMaxSize();
        this.teamScoringCount = league.getTeamScoringCount();
        this.mulliganCount = league.getMulliganCount();

        for (CalendarEntry entry : league.getCalendarEntries()){
            this.calendar.add(new CalendarEntryDTO(entry));
        }



    }

}
