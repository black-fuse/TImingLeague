package com.tekad.TimingLeague.commands.subcommands;

import com.tekad.TimingLeague.DefaultStandingsUpdater;
import com.tekad.TimingLeague.EventCategory;
import com.tekad.TimingLeague.League;
import com.tekad.TimingLeague.ScoringSystems.*;
import com.tekad.TimingLeague.TeamMode;
import com.tekad.TimingLeague.Team;
import me.makkuusen.timing.system.api.EventResultsAPI;
import me.makkuusen.timing.system.api.event.HeatResult;
import me.makkuusen.timing.system.theme.Theme;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class LeagueAdminCommands {

    public static boolean handleUpdate(Player player, League league) {
        if (!player.hasPermission("timingleague.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }
        try {
            league.updateStandings();
        } catch (Exception e) {
            league.setScoringSystem(new BasicScoringSystem());
        }
        player.sendMessage("Standings updated for league: " + league.getName());
        return true;
    }

    /** /league <league> addEvent <eventId> [categoryId] */
    public static boolean handleAddEvent(Player player, League league, String[] args) {
        if (!player.hasPermission("timingleague.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }
        if (args.length < 3) {
            player.sendMessage("Usage: /league <leagueName> addEvent <eventId> [categoryId]");
            return true;
        }
        String eventId = args[2];
        if (EventResultsAPI.getEventResult(eventId) == null) {
            player.sendMessage("Event does not exist: " + eventId);
            return true;
        }
        if (args.length >= 4) {
            String categoryId = args[3].toLowerCase();
            if (!league.hasCategory(categoryId)) {
                player.sendMessage(ChatColor.YELLOW + "Warning: category '" + categoryId + "' does not exist yet. Event added but category tag will be unresolved until the category is created.");
            }
            league.addEvent(eventId, categoryId);
            player.sendMessage("Event added to calendar: " + eventId + " [" + categoryId + "]");
        } else {
            league.addEvent(eventId);
            player.sendMessage("Event added to calendar: " + eventId + " (uncategorised)");
        }
        return true;
    }

    /** /league <league> removeEvent <eventId> */
    public static boolean handleRemoveEvent(Player player, League league, String[] args) {
        Theme theme = Theme.getTheme(player);
        if (!player.hasPermission("timingleague.admin")) {
            player.sendMessage(theme.error("You do not have permission to run this command."));
            return true;
        }
        if (args.length < 3) {
            player.sendMessage(theme.error("Usage: /league <league> removeEvent <eventId>"));
            return true;
        }
        String eventId = args[2];
        if (league.removeEvent(eventId)) {
            player.sendMessage(theme.success("Removed '" + eventId + "' from the calendar."));
            player.sendMessage(theme.warning("Run /league " + league.getName() + " update to recalculate standings."));
        } else {
            player.sendMessage(theme.error("Event '" + eventId + "' is not in the calendar."));
        }
        return true;
    }

    /** /league <league> setEventHeat <eventId> <heatId|clear> */
    public static boolean handleSetEventHeat(Player player, League league, String[] args) {
        Theme theme = Theme.getTheme(player);
        if (!player.hasPermission("timingleague.admin")) {
            player.sendMessage(theme.error("You do not have permission to run this command."));
            return true;
        }
        if (args.length < 4) {
            player.sendMessage(theme.error("Usage: /league <league> setEventHeat <eventId> <heatId|clear>"));
            player.sendMessage(theme.warning("Heat format: r1f1, r2f1 etc.  Use 'clear' to remove the pinned heat."));
            return true;
        }
        String eventId = args[2];
        String heatId  = args[3].equalsIgnoreCase("clear") ? null : args[3].toLowerCase();

        if (league.getCalendarEntry(eventId) == null) {
            player.sendMessage(theme.error("Event '" + eventId + "' is not in the calendar."));
            return true;
        }
        league.setPinnedHeat(eventId, heatId);
        if (heatId == null) {
            player.sendMessage(theme.success("Pinned heat cleared for '" + eventId + "'. Will scan all final heats."));
        } else {
            player.sendMessage(theme.success("Event '" + eventId + "' will score heat: " + heatId));
        }
        player.sendMessage(theme.warning("Run /league " + league.getName() + " update to recalculate standings."));
        return true;
    }

    /** /league <league> setEventCategory <eventId> <categoryId> */
    public static boolean handleSetEventCategory(Player player, League league, String[] args) {
        Theme theme = Theme.getTheme(player);
        if (!player.hasPermission("timingleague.admin")) {
            player.sendMessage(theme.error("You do not have permission to run this command."));
            return true;
        }
        if (args.length < 4) {
            player.sendMessage(theme.error("Usage: /league <league> setEventCategory <eventId> <categoryId>"));
            return true;
        }
        String eventId    = args[2];
        String categoryId = args[3].toLowerCase();

        if (league.getCalendarEntry(eventId) == null) {
            player.sendMessage(theme.error("Event '" + eventId + "' is not in the calendar."));
            return true;
        }
        if (!league.hasCategory(categoryId)) {
            player.sendMessage(theme.error("Category '" + categoryId + "' does not exist. Create it first with /league " + league.getName() + " category create " + categoryId));
            return true;
        }
        league.setEventCategory(eventId, categoryId);
        player.sendMessage(theme.success("Event '" + eventId + "' tagged as [" + categoryId + "]."));
        return true;
    }

    public static boolean handleUpdateWithHeat(Player player, League league, String[] args) {
        if (!player.hasPermission("timingleague.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }
        if (args.length < 4) {
            player.sendMessage("Usage: /league <leagueName> updateWithHeat <eventId> <heatId>");
            return true;
        }
        try {
            DefaultStandingsUpdater updater = (DefaultStandingsUpdater) league.getUpdater();
            HeatResult heat = updater.getHeatResults(args[2], args[3]);
            updater.processHeatWithTracking(heat, args[2], league);
            player.sendMessage("Standings updated from heat: " + args[3]);
        } catch (Exception e) {
            player.sendMessage("Failed to update from heat: " + e.getMessage());
        }
        return true;
    }

    /** /league <league> scoring <systemName>  — now also persists via the league object */
    public static boolean handleScoring(Player player, League league, String[] args) {
        if (!player.hasPermission("timingleague.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }
        if (args.length < 3) {
            player.sendMessage("Current scoring system: " + EventCategory.scoringSystemNameOf(league.getScoringSystem()));
            return true;
        }
        var system = EventCategory.scoringSystemFromName(args[2]);
        league.setScoringSystem(system);
        player.sendMessage("League default scoring system set to: " + EventCategory.scoringSystemNameOf(system));
        return true;
    }

    /** /league <league> standings drivers <true|false>  or  teams <true|false> */
    public static boolean handleStandingsToggle(Player player, League league, String[] args) {
        Theme theme = Theme.getTheme(player);
        if (!player.hasPermission("timingleague.admin")) {
            player.sendMessage(theme.error("You do not have permission to run this command."));
            return true;
        }
        if (args.length < 4) {
            player.sendMessage(theme.error("Usage: /league <league> standings <drivers|teams> <true|false>"));
            return true;
        }
        String type    = args[2].toLowerCase();
        boolean enable = Boolean.parseBoolean(args[3]);

        switch (type) {
            case "drivers" -> {
                league.setDriverStandingsEnabled(enable);
                player.sendMessage(theme.success("Driver standings " + (enable ? "enabled" : "disabled") + "."));
            }
            case "teams" -> {
                league.setTeamStandingsEnabled(enable);
                player.sendMessage(theme.success("Team standings " + (enable ? "enabled" : "disabled") + "."));
            }
            default -> player.sendMessage(theme.error("Unknown type. Use 'drivers' or 'teams'."));
        }
        return true;
    }

    public static boolean handlePredictedDrivers(Player player, League league, String[] args) {
        if (!player.hasPermission("timingleague.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }
        if (args.length > 2) {
            try {
                league.setPredictedDrivers(Integer.parseInt(args[2]));
                player.sendMessage("Predicted drivers set to: " + args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Please provide a valid number.");
            }
        } else {
            player.sendMessage("Predicted drivers: " + league.getPredictedDriverCount());
        }
        return true;
    }

    public static boolean handleTeamMode(Player player, League league, String[] args) {
        if (!player.hasPermission("timingleague.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }
        if (args.length < 3) {
            player.sendMessage("Current team mode: " + league.getTeamMode());
            player.sendMessage("Usage: /league <league> teammode <MAIN_RESERVE|PRIORITY|HIGHEST>");
            return true;
        }
        try {
            league.setTeamMode(TeamMode.valueOf(args[2].toUpperCase()));
            player.sendMessage("Team mode set to: " + league.getTeamMode());
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid mode. Options: MAIN_RESERVE, PRIORITY, HIGHEST");
        }
        return true;
    }

    public static boolean handleTeamConfig(Player player, League league, String[] args) {
        if (!player.hasPermission("timingleague.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }
        if (args.length < 4) {
            player.sendMessage("Current config - Max size: " + league.getTeamMaxSize() + ", Scoring count: " + league.getTeamScoringCount());
            player.sendMessage("Usage: /league <league> teamconfig <maxSize> <scoringCount>");
            return true;
        }
        try {
            int maxSize      = Integer.parseInt(args[2]);
            int scoringCount = Integer.parseInt(args[3]);
            league.setTeamMaxSize(maxSize);
            league.setTeamScoringCount(scoringCount);
            for (Team team : league.getTeams()) team.setCountedPrioDrivers(scoringCount);
            player.sendMessage("Team config updated - Max: " + maxSize + ", Scoring: " + scoringCount);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid numbers provided.");
        }
        return true;
    }
}
