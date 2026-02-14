package com.tekad.TimingLeague.commands.subcommands;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class LeagueHelpCommand {

    public static void showHelp(Player player) {
        boolean isAdmin = player.hasPermission("timingleague.admin");

        List<String> lines = new ArrayList<>();

        lines.add("§6=== /league Command Help ===");

        // ===== General =====
        lines.add("§e/league help §7- Show this help menu");

        if (isAdmin) {
            lines.add("§e/league create <leagueName> §7- Create a league");
            lines.add("§e/league delete <leagueName> §7- Delete a league");
        }

        // ===== League basics =====
        lines.add("§6--- League ---");
        lines.add("§e/league <league> addDriver <player> §7- Manually add a driver");
        lines.add("§e/league <league> calendar §7- View event calendar");
        lines.add("§e/league <league> standings [page] §7- Driver standings");
        lines.add("§e/league <league> standings teams [page] §7- Team standings");

        // ===== Events / scoring (admin) =====
        if (isAdmin) {
            lines.add("§6--- Events & Scoring ---");
            lines.add("§e/league <league> addEvent <eventId> §7- Add event");
            lines.add("§e/league <league> update §7- Recalculate standings");
            lines.add("§e/league <league> updateWithHeat <eventId> <heatId> §7- Update from heat");
            lines.add("§e/league <league> scoring <fc1|fc2|default> §7- Set scoring system");
            lines.add("§e/league <league> predictedDrivers [number] §7- Set/view predicted drivers");
            lines.add("§e/league <league> points <player> <+/-num> §7- Adjust driver points");
            lines.add("§e/league <league> teamPoints <team> <+/-num> §7- Adjust team points");
            lines.add("§6--- Custom Point Scale ---");
            lines.add("§e/league <league> customscale set <pos> <pts> §7- Set position points");
            lines.add("§e/league <league> customscale list §7- View custom scale");
            lines.add("§e/league <league> customscale use §7- Activate custom scale");
            lines.add("§e/league <league> customscale clear §7- Clear custom scale");
        }

        // ===== Teams =====
        lines.add("§6--- Teams ---");
        lines.add("§e/league <league> team list §7- List teams");
        lines.add("§e/league <league> team view <team> §7- View team");
        lines.add("§e/league <league> team <team> §7- Shorthand view");
        lines.add("§e/league <league> team accept <team> §7- Accept invite");
        lines.add("§e/league <league> team decline <team> §7- Decline invite");
        lines.add("§e/league <league> team setowner <team> <player> §7- Transfer ownership");

        if (isAdmin) {
            lines.add("§e/league <league> team create <team> [hex] §7- Create team");
            lines.add("§e/league <league> team color <team> <hex> §7- Set team color");
            lines.add("§e/league <league> team add <team> <main|reserve> <player>");
        }

        lines.add("§e/league <league> team remove <team> <player> §7- Remove member");
        lines.add("§e/league <league> team invite <team> <player> §7- Invite driver");

        // ===== MAIN_RESERVE Mode Commands =====
        lines.add("§6--- MAIN_RESERVE Mode ---");
        lines.add("§e/league <league> team promote <team> <player> §7- Promote reserve");
        lines.add("§e/league <league> team demote <team> <player> §7- Demote main");

        // ===== PRIORITY/HIGHEST Mode Commands =====
        lines.add("§6--- PRIORITY/HIGHEST Mode ---");
        lines.add("§e/league <league> team setpriority <team> <player> <pos>");
        lines.add("§e/league <league> team removepriority <team> <player>");
        lines.add("§e/league <league> team swap <team> <player1> <player2>");

        // ===== Holograms =====
        if (isAdmin) {
            lines.add("§6--- Holograms ---");
            lines.add("§e/league <league> holo <driver|team> [page]");
            lines.add("§e/league <league> holo deleteClosest");
            lines.add("§e/league <league> holo update");
        }

        // Send
        player.sendMessage(lines.toArray(new String[0]));
    }
}
