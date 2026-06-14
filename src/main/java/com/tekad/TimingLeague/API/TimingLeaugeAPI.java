package com.tekad.TimingLeague.API;

import com.tekad.TimingLeague.League;
import com.tekad.TimingLeague.TImingLeague;

import java.util.Collection;

public class TimingLeaugeAPI {
    public static Collection<League> getLeagues() {
        return TImingLeague.getLeagueMap().values();
    }

    public static League getLeague(String name) {
        return TImingLeague.getLeagueMap().get(name);
    }
}
