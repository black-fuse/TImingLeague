package com.tekad.TimingLeague;

import me.makkuusen.timing.system.api.EventResultsAPI;
import me.makkuusen.timing.system.api.event.EventResult;
import me.makkuusen.timing.system.api.event.HeatResult;
import me.makkuusen.timing.system.api.event.RoundResult;

import java.util.ArrayList;
import java.util.List;

public interface StandingsUpdater {
    //TODO: make a class for versions where points don't move with drivers
    void updateStandingsFromEvents(League league);
    HeatResult getHeatResults(String event, String heat);
}
