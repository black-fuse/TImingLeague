package com.tekad.TimingLeague;

import me.makkuusen.timing.system.api.EventResultsAPI;
import me.makkuusen.timing.system.api.event.DriverResult;
import me.makkuusen.timing.system.api.event.EventResult;
import me.makkuusen.timing.system.api.event.HeatResult;
import me.makkuusen.timing.system.api.event.RoundResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultStandingsUpdater implements StandingsUpdater{

    @Override
    public void updateStandingsFromEvents(League league) {
        Set<String> calendar = league.getCalendar();
        Map<String, Team> driversList = league.getDriversList();

        for (String event : calendar){
            EventResult results = EventResultsAPI.getEventResult(event);
            if (results == null || results.getRounds() == null) continue;

            for (RoundResult roundResult: results.getRounds()){
                for (HeatResult heat: roundResult.getHeatResults()){
                    List<DriverResult> drivers = heat.getDriverResultList();
                    int driverCount = drivers.size();

                    for (DriverResult driver : heat.getDriverResultList()){
                        String DriverUUID = driver.getUuid();

                        if (!driversList.containsKey(DriverUUID)){
                            league.addDriver(DriverUUID, league.NoTeam);
                        }

                        Team DriversTeam = driversList.getOrDefault(DriverUUID, league.NoTeam);

                        int points = league.getScoringSystem().getPointsForPosition(driver.getPosition(), driverCount);
                        league.addPointsToDriver(DriverUUID, points);
                        league.addPointsToTeam(DriversTeam.getName(), points);
                    }
                }
            }
        }
    }


}
