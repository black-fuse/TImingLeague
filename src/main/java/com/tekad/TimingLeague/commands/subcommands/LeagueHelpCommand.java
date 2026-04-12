package com.tekad.TimingLeague.commands.subcommands;

import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

public class LeagueHelpCommand {

    public static void showHelp(Player player) {
        boolean isAdmin = player.hasPermission("timingleague.admin");
        List<String> lines = new ArrayList<>();

        lines.add("§6=== /league Command Help ===");
        lines.add("§e/league help §7- Show this help menu");

        if (isAdmin) {
            lines.add("§e/league create <leagueName> §7- Create a league");
            lines.add("§e/league delete <leagueName> §7- Delete a league");
        }

        lines.add("§6--- League ---");
        lines.add("§e/league <league> §7- Open league info panel");
        lines.add("§e/league <league> calendar §7- View event calendar with categories");
        lines.add("§e/league <league> standings [page] §7- Driver standings");
        lines.add("§e/league <league> standings teams [page] §7- Team standings");
        lines.add("§e/league <league> breakdown <player> §7- Per-event point breakdown");

        if (isAdmin) {
            lines.add("§6--- Events & Scoring ---");
            lines.add("§e/league <league> addEvent <eventId> [categoryId] §7- Add event (optionally tagged)");
            lines.add("§e/league <league> removeEvent <eventId> §7- Remove event from calendar");
            lines.add("§e/league <league> setEventCategory <eventId> <categoryId> §7- Tag an existing event");
            lines.add("§e/league <league> setEventHeat <eventId> <heatId|clear> §7- Pin a specific heat to score (e.g. r1f1)");
            lines.add("§e/league <league> update §7- Recalculate standings");
            lines.add("§e/league <league> updateWithHeat <eventId> <heatId> §7- Update from specific heat");
            lines.add("§e/league <league> scoring <system> §7- Set default scoring (fc1|fc2|iec|iecdouble|iecopener|wibrs|linear|default)");
            lines.add("§e/league <league> predictedDrivers [number] §7- Set/view predicted drivers");
            lines.add("§e/league <league> points <player> <+/-num> §7- Adjust driver points");
            lines.add("§e/league <league> teamPoints <team> <+/-num> §7- Adjust team points");
            lines.add("§6--- Categories ---");
            lines.add("§e/league <league> category list §7- View all categories");
            lines.add("§e/league <league> category create <id> [displayName] §7- Create a category");
            lines.add("§e/league <league> category info <id> §7- Category details panel");
            lines.add("§e/league <league> category scoring <id> <system> §7- Set category scoring");
            lines.add("§e/league <league> category mulligans <id> <count> §7- Set category mulligan drops");
            lines.add("§e/league <league> category rename <id> <name> §7- Rename category display name");
            lines.add("§e/league <league> category delete <id> §7- Delete category");
            lines.add("§6--- Mulligans ---");
            lines.add("§e/league <league> mulligans <count> §7- Fallback mulligan count (uncategorised events)");
            lines.add("§e/league <league> mulligans team <true|false> §7- Enable/disable team mulligans");
            lines.add("§6--- Standings Toggles ---");
            lines.add("§e/league <league> standings drivers <true|false> §7- Enable/disable driver standings");
            lines.add("§e/league <league> standings teams <true|false> §7- Enable/disable team standings");
            lines.add("§6--- Custom Point Scale ---");
            lines.add("§e/league <league> customscale set <pos> <pts> §7- Set position points");
            lines.add("§e/league <league> customscale list §7- View custom scale");
            lines.add("§e/league <league> customscale use §7- Activate custom scale");
            lines.add("§e/league <league> customscale clear §7- Clear custom scale");
        }

        lines.add("§6--- Teams ---");
        lines.add("§e/league <league> team list §7- List teams");
        lines.add("§e/league <league> team view <team> §7- View team roster");
        lines.add("§e/league <league> team accept <team> §7- Accept invite");
        lines.add("§e/league <league> team decline <team> §7- Decline invite");
        lines.add("§e/league <league> team invite <team> <player> §7- Invite a driver");
        lines.add("§e/league <league> team leave §7- Leave your team");
        lines.add("§e/league <league> team setowner <team> <player> §7- Transfer ownership");

        if (isAdmin) {
            lines.add("§e/league <league> team create <team> [hex] §7- Create team");
            lines.add("§e/league <league> team color <team> <hex> §7- Set team colour");
            lines.add("§e/league <league> team add <team> <main|reserve> <player> §7- Add driver");
            lines.add("§e/league <league> teammode <MAIN_RESERVE|PRIORITY|HIGHEST> §7- Set team mode");
            lines.add("§e/league <league> teamconfig <maxSize> <scoringCount> §7- Configure team sizes");
            lines.add("§6--- Holograms ---");
            lines.add("§e/league <league> holo <driver|team> §7- Place leaderboard hologram");
            lines.add("§e/league <league> holo deleteClosest §7- Delete nearest hologram");
            lines.add("§e/league <league> holo update §7- Refresh holograms");
        }

        player.sendMessage(lines.toArray(new String[0]));
    }
}
