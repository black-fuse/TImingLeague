package com.tekad.TimingLeague.API;

import com.tekad.TimingLeague.League;
import com.tekad.TimingLeague.TImingLeague;

import java.util.Collection;

public class TimingLeagueAPI {
    public static Collection<League> getLeagues() {
        return TImingLeague.getInstance().getLeagueMap().values();
    }

    public static League getLeague(String name) {
        return TImingLeague.getInstance().getLeagueMap().get(name);
    }
}
