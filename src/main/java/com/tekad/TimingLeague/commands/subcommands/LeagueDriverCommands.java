package com.tekad.TimingLeague.commands.subcommands;

import com.tekad.TimingLeague.League;
import com.tekad.TimingLeague.PointEntry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LeagueDriverCommands {

    public static boolean handleAddDriver(Player player, League league, String[] args) {
        if (args.length < 3) {
            player.sendMessage("Usage: /league <leagueName> addDriver <username>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        UUID uuid = target.getUniqueId();
        league.addDriver(uuid.toString(), league.NoTeam);
        player.sendMessage("Added driver to league (note: this happens automatically after racing).");
        return true;
    }

    public static boolean handlePoints(Player player, League league, String[] args) {
        if (!player.hasPermission("timingleague.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }

        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Format: /league <leaguename> points <driver> <+/-num>");
            return true;
        }

        int points;
        try {
            points = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number: " + args[3]);
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        String uuid = target.getUniqueId().toString();

        // Extract reason if provided
        String reason = args.length > 4 ? String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length)) : "manual";

        // Track manual adjustment
        PointEntry entry = new PointEntry("manual:" + reason, null, points, System.currentTimeMillis());
        league.addDriverPointEntry(uuid, entry);

        // Apply to current standings
        league.addPointsToDriver(uuid, points);
        
        player.sendMessage("Added " + points + " points to " + target.getName() + " (" + reason + ")");
        return true;
    }

    public static boolean handleTeamPoints(Player player, League league, String[] args) {
        if (!player.hasPermission("timingleague.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }

        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Format: /league <leaguename> teampoints <teamName> <+/-num>");
            return true;
        }

        int points;
        try {
            points = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number: " + args[3]);
            return true;
        }

        // Extract reason if provided
        String reason = args.length > 4 ? String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length)) : "manual";

        // Track manual adjustment
        PointEntry entry = new PointEntry("manual:" + reason, null, points, System.currentTimeMillis());
        league.addTeamPointEntry(args[2], entry);

        // Apply to current standings
        league.addPointsToTeam(args[2], points);
        
        player.sendMessage("Added " + points + " points to team " + args[2] + " (" + reason + ")");
        return true;
    }
}
