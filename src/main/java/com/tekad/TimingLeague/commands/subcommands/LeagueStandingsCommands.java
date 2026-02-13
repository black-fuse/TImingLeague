package com.tekad.TimingLeague.commands.subcommands;

import com.tekad.TimingLeague.League;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LeagueStandingsCommands {

    public static boolean handleStandings(Player player, League league, String leagueName, String[] args) {
        int page = 1;
        boolean showTeams = false;

        if (args.length >= 3) {
            if (args[2].equalsIgnoreCase("teams")) {
                showTeams = true;
                if (args.length >= 4) {
                    try {
                        page = Integer.parseInt(args[3]);
                    } catch (NumberFormatException ignored) {}
                }
            } else {
                try {
                    page = Integer.parseInt(args[2]);
                } catch (NumberFormatException ignored) {}
            }
        }

        int pageSize = 15;
        int start = (page - 1) * pageSize;

        if (showTeams) {
            var standings = league.getTeamStandings().entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .toList();

            if (start >= standings.size()) {
                player.sendMessage("Page out of range.");
                return true;
            }

            int totalPages = (int) Math.ceil((double) standings.size() / pageSize);
            player.sendMessage("§6=== Team Standings (Page " + page + "/" + totalPages + ") ===");

            for (int i = start; i < Math.min(start + pageSize, standings.size()); i++) {
                var entry = standings.get(i);
                player.sendMessage((i + 1) + ". " + entry.getKey() + " - " + entry.getValue() + " pts");
            }

            // Clickable navigation
            sendNavigationButtons(player, leagueName, page, totalPages, true);

        } else {
            var standings = league.getDriverStandings().entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .toList();

            if (start >= standings.size()) {
                player.sendMessage("Page out of range.");
                return true;
            }

            int totalPages = (int) Math.ceil((double) standings.size() / pageSize);
            player.sendMessage("§6=== Driver Standings (Page " + page + "/" + totalPages + ") ===");

            for (int i = start; i < Math.min(start + pageSize, standings.size()); i++) {
                var entry = standings.get(i);
                OfflinePlayer p = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey()));
                player.sendMessage((i + 1) + ". " + p.getName() + " - " + entry.getValue() + " pts");
            }

            // Clickable navigation
            sendNavigationButtons(player, leagueName, page, totalPages, false);
        }

        return true;
    }

    private static void sendNavigationButtons(Player player, String leagueName, int currentPage, int totalPages, boolean isTeams) {
        Component navigation = Component.empty();

        // Previous button
        if (currentPage > 1) {
            String prevCommand = "/league " + leagueName + " standings " + (isTeams ? "teams " : "") + (currentPage - 1);
            Component prevButton = Component.text("[< Previous]")
                    .color(NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand(prevCommand));
            navigation = navigation.append(prevButton);
        } else {
            navigation = navigation.append(Component.text("[< Previous]").color(NamedTextColor.DARK_GRAY));
        }

        navigation = navigation.append(Component.text("  "));

        // Next button
        if (currentPage < totalPages) {
            String nextCommand = "/league " + leagueName + " standings " + (isTeams ? "teams " : "") + (currentPage + 1);
            Component nextButton = Component.text("[Next >]")
                    .color(NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand(nextCommand));
            navigation = navigation.append(nextButton);
        } else {
            navigation = navigation.append(Component.text("[Next >]").color(NamedTextColor.DARK_GRAY));
        }

        player.sendMessage(navigation);
    }

    public static boolean handleCalendar(Player player, League league) {
        StringBuilder builder = new StringBuilder("=== " + league.getName() + " Calendar ===\n");
        for (String event : league.getCalendar()) {
            builder.append("- ").append(event).append("\n");
        }
        player.sendMessage(builder.toString());
        return true;
    }
}
