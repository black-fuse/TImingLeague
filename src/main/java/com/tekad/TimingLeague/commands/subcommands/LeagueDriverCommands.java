package com.tekad.TimingLeague.commands.subcommands;

import com.tekad.TimingLeague.League;
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

        league.addPointsToDriver(uuid, points);
        player.sendMessage("Added " + points + " points to " + target.getName());
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

        league.addPointsToTeam(args[2], points);
        player.sendMessage("Added " + points + " points to team " + args[2]);
        return true;
    }
}
