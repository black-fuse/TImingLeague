package com.tekad.TimingLeague.commands.subcommands;

import com.tekad.TimingLeague.League;
import me.makkuusen.timing.system.theme.Theme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LeagueStandingsCommands {

    public static boolean handleStandings(Player player, League league, String leagueName, String[] args) {
        Theme theme = Theme.getTheme(player);
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

            player.sendMessage("");
            player.sendMessage(theme.getRefreshButton().clickEvent(ClickEvent.runCommand("/league " + leagueName + " standings"))
                    .append(Component.space())
                    .append(theme.getTitleLine(Component.text(leagueName + " team standings").color(theme.getSecondary())
                            .append(Component.space())
                            .append(theme.getBrackets(page + "/" + totalPages))
                            .append(Component.space())
                    )));

            for (int i = start; i < Math.min(start + pageSize, standings.size()); i++) {
                var entry = standings.get(i);
                Component line = Component.text((i + 1) + ". ").color(theme.getPrimary())
                        .append(Component.text(entry.getKey()).color(theme.getSecondary()))
                        .append(theme.hyphen())
                        .append(Component.text(entry.getValue() + " pts").color(theme.getSecondary()));
                player.sendMessage(line);
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

            player.sendMessage("");
            player.sendMessage(theme.getRefreshButton().clickEvent(ClickEvent.runCommand("/league " + leagueName + " standings"))
                    .append(Component.space())
                    .append(theme.getTitleLine(Component.text(leagueName + " driver standings").color(theme.getSecondary())
                            .append(Component.space())
                            .append(theme.getBrackets(page + "/" + totalPages))
                            .append(Component.space())
                    )));

            for (int i = start; i < Math.min(start + pageSize, standings.size()); i++) {
                var entry = standings.get(i);
                OfflinePlayer p = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey()));
                Component line = Component.text((i + 1) + ". ").color(theme.getPrimary())
                        .append(Component.text(p.getName()).color(theme.getSecondary()))
                        .append(theme.hyphen())
                        .append(Component.text(entry.getValue() + " pts").color(theme.getSecondary()));
                player.sendMessage(line);
            }

            // Clickable navigation
            sendNavigationButtons(player, leagueName, page, totalPages, false);
        }

        return true;
    }

    private static void sendNavigationButtons(Player player, String leagueName, int currentPage, int totalPages, boolean isTeams) {
        Theme theme = Theme.getTheme(player);
        Component navigation = Component.empty();

        // Previous button
        if (currentPage > 1) {
            String prevCommand = "/league " + leagueName + " standings " + (isTeams ? "teams " : "") + (currentPage - 1);
            Component prevButton = Component.text("<<< ").color(theme.getButton())
                    .clickEvent(ClickEvent.runCommand(prevCommand));
            navigation = navigation.append(prevButton);
        } else {
            navigation = navigation.append(Component.text("<<< ").color(theme.getPrimary()));
        }

        navigation = navigation.append(Component.text("Page " + currentPage + " of " + totalPages).color(theme.getSecondary()));

        // Next button
        if (currentPage < totalPages) {
            String nextCommand = "/league " + leagueName + " standings " + (isTeams ? "teams " : "") + (currentPage + 1);
            Component nextButton = Component.text(" >>>").color(theme.getButton())
                    .clickEvent(ClickEvent.runCommand(nextCommand));
            navigation = navigation.append(nextButton);
        } else {
            navigation = navigation.append(Component.text(" >>>").color(theme.getPrimary()));
        }

        player.sendMessage(navigation);
    }

    public static boolean handleCalendar(Player player, League league) {
        Theme theme = Theme.getTheme(player);
        String leagueName = league.getName();

        player.sendMessage("");
        player.sendMessage(theme.getRefreshButton().clickEvent(ClickEvent.runCommand("/league " + leagueName + " calendar"))
                .append(Component.space())
                .append(theme.getTitleLine(Component.text(leagueName).color(theme.getSecondary())
                        .append(Component.space())
                )));

        StringBuilder builder = new StringBuilder();
        List<String> calendar = new ArrayList<>(league.getCalendar());

        for (int i = calendar.size() - 1; i >= 0; i--) {
            builder.append("- ")
                    .append(calendar.get(i))
                    .append("\n");
        }
        player.sendMessage(builder.toString());
        return true;
    }
}
