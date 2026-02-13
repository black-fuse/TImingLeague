package com.tekad.TimingLeague.commands.subcommands;

import com.tekad.TimingLeague.DefaultStandingsUpdater;
import com.tekad.TimingLeague.League;
import com.tekad.TimingLeague.ScoringSystems.*;
import com.tekad.TimingLeague.TeamMode;
import com.tekad.TimingLeague.Team;
import me.makkuusen.timing.system.api.EventResultsAPI;
import me.makkuusen.timing.system.api.event.HeatResult;
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

    public static boolean handleAddEvent(Player player, League league, String[] args) {
        if (!player.hasPermission("timingleague.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }

        if (args.length < 3) {
            player.sendMessage("Usage: /league <leagueName> addEvent <eventId>");
            return true;
        }

        String eventId = args[2];
        if (EventResultsAPI.getEventResult(eventId) == null) {
            player.sendMessage("Event does not exist: " + eventId);
            return true;
        }

        league.addEvent(eventId);
        player.sendMessage("Event added to league calendar: " + eventId);
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
            updater.updateStandingsWithHeat(heat, league);
            player.sendMessage("Standings updated from heat: " + args[3]);
        } catch (Exception e) {
            player.sendMessage("Failed to update from heat: " + e.getMessage());
        }
        return true;
    }

    public static boolean handleScoring(Player player, League league, String[] args) {
        if (!player.hasPermission("timingleague.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }

        if (args.length < 3) {
            player.sendMessage("Current scoring system: " + league.getScoringSystem().toString());
            return true;
        }

        switch (args[2].toLowerCase()) {
            case "fc1" -> league.setScoringSystem(new FC1ScoringSystem());
            case "fc2" -> league.setScoringSystem(new FC2ScoringSystem());
            case "default" -> league.setScoringSystem(new BasicScoringSystem());
            case "wibrs" -> league.setScoringSystem(new WIBRSScoringSystem());
            case "iec" -> league.setScoringSystem(new IECScoringSystem());
            case "iecdouble" -> league.setScoringSystem(new IECDouble());
            case "iecopener" -> league.setScoringSystem(new IECOpenerSystem());
            default -> {
                player.sendMessage("Not a valid scoring system");
                player.sendMessage("Current scoring system: " + league.getScoringSystem().toString());
            }
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
                int predictedDrivers = Integer.parseInt(args[2]);
                league.setPredictedDrivers(predictedDrivers);
                player.sendMessage("Predicted drivers set to: " + predictedDrivers);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Please provide a valid number for predicted drivers.");
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
            TeamMode mode = TeamMode.valueOf(args[2].toUpperCase());
            league.setTeamMode(mode);
            player.sendMessage("Team mode set to: " + mode);
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
            player.sendMessage("Current config - Max size: " + league.getTeamMaxSize() +
                    ", Scoring count: " + league.getTeamScoringCount());
            player.sendMessage("Usage: /league <league> teamconfig <maxSize> <scoringCount>");
            return true;
        }

        try {
            int maxSize = Integer.parseInt(args[2]);
            int scoringCount = Integer.parseInt(args[3]);

            league.setTeamMaxSize(maxSize);
            league.setTeamScoringCount(scoringCount);

            // Update all teams
            for (Team team : league.getTeams()) {
                team.setCountedPrioDrivers(scoringCount);
            }

            player.sendMessage("Team config updated - Max: " + maxSize + ", Scoring: " + scoringCount);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid numbers provided.");
        }
        return true;
    }
}
