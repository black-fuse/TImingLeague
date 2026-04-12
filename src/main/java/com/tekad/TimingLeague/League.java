package com.tekad.TimingLeague;

import com.tekad.TimingLeague.ScoringSystems.BasicScoringSystem;
import com.tekad.TimingLeague.ScoringSystems.ScoringSystem;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.function.UnaryOperator;

public class League {

    @Getter
    private final String name;
    private final Set<Team> teamsList = new HashSet<>();

    @Getter
    private final LinkedHashSet<CalendarEntry> calendarEntries = new LinkedHashSet<>();

    @Getter
    private final Map<String, EventCategory> categories = new LinkedHashMap<>();

    @Getter
    private int predictedDriverCount;
    @Getter @Setter
    private ScoringSystem scoringSystem = new BasicScoringSystem();

    public final Team NoTeam = new Team("No Team", "a8a8a8", this);
    public int flapPoints = 0;

    @Getter @Setter
    private boolean driverStandingsEnabled = true;
    @Getter @Setter
    private boolean teamStandingsEnabled = true;

    private final Map<String, Integer> driverStandings = new HashMap<>();
    private final Map<String, Integer> teamStandings   = new HashMap<>();
    @Getter
    private final Map<String, Team> driversList = new HashMap<>();

    private StandingsUpdater updater = new DefaultStandingsUpdater();

    @Getter
    private final Map<Integer, Integer> customScalePoints = new HashMap<>();

    @Getter @Setter
    private int mulliganCount = 0;
    @Getter @Setter
    private boolean teamMulligansEnabled = false;

    @Getter
    private final Map<String, List<PointEntry>> driverPointHistory = new HashMap<>();
    @Getter
    private final Map<String, List<PointEntry>> teamPointHistory   = new HashMap<>();
    @Getter
    private final Map<String, List<String>> driverMulliganedEvents = new HashMap<>();
    @Getter
    private final Map<String, List<String>> teamMulliganedEvents   = new HashMap<>();

    @Getter @Setter
    private TeamMode teamMode = TeamMode.MAIN_RESERVE;
    @Getter @Setter
    private int teamMaxSize = 4;
    @Getter @Setter
    private int teamScoringCount = 2;


    public League(String name, int predictedDriverCount) {
        this.name = name;
        this.predictedDriverCount = predictedDriverCount;
        teamsList.add(NoTeam);
    }

    // ── Calendar ──────────────────────────────────────────────────────────────

    /** Add uncategorised, no pinned heat. */
    public void addEvent(String eventName) {
        addCalendarEntry(new CalendarEntry(eventName));
    }

    /** Add with category, no pinned heat. */
    public void addEvent(String eventName, String categoryId) {
        addCalendarEntry(new CalendarEntry(eventName, categoryId));
    }

    /** Add with category and specific heat to score. */
    public void addEvent(String eventName, String categoryId, String heatId) {
        addCalendarEntry(new CalendarEntry(eventName, categoryId, heatId));
    }

    /** Replaces any existing entry for the same event name (preserves order on update). */
    public void addCalendarEntry(CalendarEntry entry) {
        if (calendarEntries.contains(entry)) {
            // Already present — do an in-place replace to preserve ordering
            replaceEntry(entry.getEventName(), existing -> entry);
        } else {
            calendarEntries.add(entry);
        }
    }

    /** Removes an event from the calendar. Returns false if not found. */
    public boolean removeEvent(String eventName) {
        return calendarEntries.removeIf(e -> e.getEventName().equals(eventName));
    }

    /** Backwards-compat: returns ordered set of event name strings only. */
    public Set<String> getCalendar() {
        Set<String> names = new LinkedHashSet<>();
        for (CalendarEntry e : calendarEntries) names.add(e.getEventName());
        return names;
    }

    public CalendarEntry getCalendarEntry(String eventName) {
        for (CalendarEntry e : calendarEntries) {
            if (e.getEventName().equals(eventName)) return e;
        }
        return null;
    }

    /** Sets the category on an existing entry, preserving its heatId. */
    public boolean setEventCategory(String eventName, String categoryId) {
        return replaceEntry(eventName, e -> new CalendarEntry(e.getEventName(), categoryId, e.getHeatId()));
    }

    /** Pins a specific heat on an existing entry, preserving its categoryId. */
    public boolean setPinnedHeat(String eventName, String heatId) {
        return replaceEntry(eventName, e -> new CalendarEntry(e.getEventName(), e.getCategoryId(), heatId));
    }

    /** In-place replace: finds entry by name, applies replacer, reinserts at same position. */
    private boolean replaceEntry(String eventName, UnaryOperator<CalendarEntry> replacer) {
        List<CalendarEntry> ordered = new ArrayList<>(calendarEntries);
        boolean found = false;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getEventName().equals(eventName)) {
                ordered.set(i, replacer.apply(ordered.get(i)));
                found = true;
                break;
            }
        }
        if (found) {
            calendarEntries.clear();
            calendarEntries.addAll(ordered);
        }
        return found;
    }


    // ── Categories ────────────────────────────────────────────────────────────

    public void addCategory(EventCategory category) { categories.put(category.getId(), category); }
    public EventCategory getCategory(String id) { return categories.get(id); }
    public boolean hasCategory(String id) { return categories.containsKey(id); }
    public void removeCategory(String id) { categories.remove(id); }

    public EventCategory getCategoryForEvent(String eventName) {
        CalendarEntry entry = getCalendarEntry(eventName);
        if (entry == null || !entry.hasCategory()) return null;
        return categories.get(entry.getCategoryId());
    }

    public ScoringSystem getScoringSystemForEvent(String eventName) {
        EventCategory cat = getCategoryForEvent(eventName);
        return cat != null ? cat.getScoringSystem() : scoringSystem;
    }

    public int getMulliganCountForEvent(String eventName) {
        EventCategory cat = getCategoryForEvent(eventName);
        return cat != null ? cat.getMulliganCount() : mulliganCount;
    }

    // ── Standings toggles ─────────────────────────────────────────────────────

    public boolean isOnlyTeamStandings()   { return teamStandingsEnabled && !driverStandingsEnabled; }
    public boolean isOnlyDriverStandings() { return driverStandingsEnabled && !teamStandingsEnabled; }

    // ── Standings update ──────────────────────────────────────────────────────

    public void updateStandings() { updater.updateStandingsFromEvents(this); }
    public StandingsUpdater getUpdater() { return updater; }
    public void setUpdater(StandingsUpdater updater) { this.updater = updater; }

    // ── Teams ─────────────────────────────────────────────────────────────────

    public Set<Team> getTeams() { return teamsList; }
    public Set<String> getTeamsString() {
        Set<String> list = new HashSet<>();
        for (Team team : teamsList) list.add(team.getName());
        return list;
    }
    public void setPredictedDrivers(int num) { predictedDriverCount = num; }
    public void addTeam(Team team) { teamsList.add(team); }
    public Set<String> getDrivers() { return driversList.keySet(); }
    public int getDriverPoints(String driver) { return driverStandings.getOrDefault(driver, 0); }
    public void setDriverPoints(String driver, int points) { driverStandings.put(driver, points); }
    public Map<String, Integer> getDriverStandings() { return driverStandings; }
    public void addDriver(String driver, Team team) { driversList.put(driver, team); }

    public boolean addMainDriverToTeam(String uuid, String teamName) {
        Team team = getTeam(teamName);
        boolean success = team.addMainDriver(uuid);
        if (success) driversList.put(uuid, team);
        return success;
    }

    public boolean addReserveDriverToTeam(String uuid, String teamName) {
        Team team = getTeam(teamName);
        boolean success = team.addReserveDriver(uuid);
        if (success) driversList.put(uuid, team);
        return success;
    }

    public boolean addDriverToTeam(String uuid, String teamName, int priority) {
        Team team = getTeam(teamName);
        boolean success;
        if (teamMode == TeamMode.HIGHEST || teamMode == TeamMode.PRIORITY) {
            success = team.addDriver(uuid, priority);
        } else {
            success = team.addMainDriver(uuid);
            if (!success) team.addReserveDriver(uuid);
        }
        if (success) driversList.put(uuid, team);
        return success;
    }

    public void addPointsToDriver(String uuid, int points) {
        driverStandings.put(uuid, driverStandings.getOrDefault(uuid, 0) + points);
    }

    public void addPointsToTeam(String team, int points) {
        teamStandings.put(team, teamStandings.getOrDefault(team, 0) + points);
        for (Team t : teamsList) { if (team.equals(t.getName())) t.setPoints(teamStandings.get(team)); }
    }

    public void setTeamPoints(String team, int points) {
        teamStandings.put(team, points);
        for (Team t : teamsList) { if (team.equals(t.getName())) t.setPoints(teamStandings.get(team)); }
    }

    public Team getTeamByDriver(String driver) { return driversList.getOrDefault(driver, NoTeam); }
    public Team getTeam(String teamName) {
        for (Team team : teamsList) { if (team.getName().equalsIgnoreCase(teamName)) return team; }
        return null;
    }
    public Map<String, Integer> getTeamStandings() { return teamStandings; }
    public int getTeamPoints(String target) { return teamStandings.getOrDefault(target, 0); }

    // ── Custom scale ──────────────────────────────────────────────────────────

    public void setCustomScalePoint(int position, int points) { if (position > 0) customScalePoints.put(position, points); }
    public void clearCustomScale() { customScalePoints.clear(); }
    public boolean hasCustomScale() { return !customScalePoints.isEmpty(); }

    // ── Point history ─────────────────────────────────────────────────────────

    public void addDriverPointEntry(String uuid, PointEntry entry) {
        driverPointHistory.computeIfAbsent(uuid, k -> new ArrayList<>()).add(entry);
    }
    public void addTeamPointEntry(String teamName, PointEntry entry) {
        teamPointHistory.computeIfAbsent(teamName, k -> new ArrayList<>()).add(entry);
    }
    public void clearPointHistory() {
        driverPointHistory.clear(); teamPointHistory.clear();
        driverMulliganedEvents.clear(); teamMulliganedEvents.clear();
    }
    public void setDriverMulliganedEvents(String uuid, List<String> events) {
        driverMulliganedEvents.put(uuid, new ArrayList<>(events));
    }
    public void setTeamMulliganedEvents(String teamName, List<String> events) {
        teamMulliganedEvents.put(teamName, new ArrayList<>(events));
    }
}
