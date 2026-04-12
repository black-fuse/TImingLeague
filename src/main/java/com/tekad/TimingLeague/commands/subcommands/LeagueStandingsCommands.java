package com.tekad.TimingLeague.commands.subcommands;

import com.tekad.TimingLeague.CalendarEntry;
import com.tekad.TimingLeague.EventCategory;
import com.tekad.TimingLeague.League;
import me.makkuusen.timing.system.theme.Theme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class LeagueStandingsCommands {

    // ─────────────────────────────────────────────────────────────────────────
    // /league <league> standings [drivers|teams] [page]
    // ─────────────────────────────────────────────────────────────────────────

    public static boolean handleStandings(Player player, League league, String leagueName, String[] args) {
        Theme theme = Theme.getTheme(player);
        boolean showTeams  = false;
        boolean showToggle = false; // only admins setting drivers/teams true|false reach handleStandingsToggle
        int page = 1;

        if (args.length >= 3) {
            String sub = args[2].toLowerCase();
            if (sub.equals("teams")) {
                showTeams = true;
                if (args.length >= 4) {
                    try { page = Integer.parseInt(args[3]); } catch (NumberFormatException ignored) {}
                }
            } else {
                try { page = Integer.parseInt(sub); } catch (NumberFormatException ignored) {}
            }
        }

        // Guard: if the requested standings type is disabled, redirect
        if (showTeams && !league.isTeamStandingsEnabled()) {
            player.sendMessage(theme.error("Team standings are disabled for this league."));
            if (league.isDriverStandingsEnabled()) {
                player.sendMessage(Component.text("View driver standings: ").color(theme.getPrimary())
                        .append(theme.getViewButton(player)
                                .clickEvent(ClickEvent.runCommand("/league " + leagueName + " standings"))));
            }
            return true;
        }
        if (!showTeams && !league.isDriverStandingsEnabled()) {
            player.sendMessage(theme.error("Driver standings are disabled for this league."));
            if (league.isTeamStandingsEnabled()) {
                player.sendMessage(Component.text("View team standings: ").color(theme.getPrimary())
                        .append(theme.getViewButton(player)
                                .clickEvent(ClickEvent.runCommand("/league " + leagueName + " standings teams"))));
            }
            return true;
        }

        int pageSize = 15;
        int start = (page - 1) * pageSize;

        if (showTeams) {
            var standings = league.getTeamStandings().entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .toList();
            if (standings.isEmpty() || start >= standings.size()) {
                player.sendMessage(theme.error("No team standings data or page out of range."));
                return true;
            }
            int totalPages = (int) Math.ceil((double) standings.size() / pageSize);

            player.sendMessage("");
            player.sendMessage(buildStandingsHeader(player, leagueName, "team standings", page, totalPages, showTeams, league, theme));

            for (int i = start; i < Math.min(start + pageSize, standings.size()); i++) {
                var entry = standings.get(i);
                player.sendMessage(Component.text((i + 1) + ". ").color(theme.getPrimary())
                        .append(Component.text(entry.getKey()).color(theme.getSecondary()))
                        .append(theme.hyphen())
                        .append(Component.text(entry.getValue() + " pts").color(theme.getSecondary())));
            }
            sendNavigationButtons(player, leagueName, page, totalPages, true, theme);

        } else {
            var standings = league.getDriverStandings().entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .toList();
            if (standings.isEmpty() || start >= standings.size()) {
                player.sendMessage(theme.error("No driver standings data or page out of range."));
                return true;
            }
            int totalPages = (int) Math.ceil((double) standings.size() / pageSize);

            player.sendMessage("");
            player.sendMessage(buildStandingsHeader(player, leagueName, "driver standings", page, totalPages, showTeams, league, theme));

            for (int i = start; i < Math.min(start + pageSize, standings.size()); i++) {
                var entry = standings.get(i);
                OfflinePlayer p = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey()));
                String name = p.getName() != null ? p.getName() : "Unknown";
                player.sendMessage(Component.text((i + 1) + ". ").color(theme.getPrimary())
                        .append(Component.text(name).color(theme.getSecondary()))
                        .append(theme.hyphen())
                        .append(Component.text(entry.getValue() + " pts").color(theme.getSecondary())));
            }
            sendNavigationButtons(player, leagueName, page, totalPages, false, theme);
        }
        return true;
    }

    /** Header row: title + page counter + tab buttons for switching driver/team view */
    private static Component buildStandingsHeader(Player player, String leagueName, String title,
            int page, int totalPages, boolean isTeams, League league, Theme theme) {

        Component header = theme.getRefreshButton()
                .clickEvent(ClickEvent.runCommand("/league " + leagueName + " standings" + (isTeams ? " teams" : "")))
                .append(Component.space())
                .append(theme.getTitleLine(
                        Component.text(leagueName + " " + title).color(theme.getSecondary())
                                .append(Component.space())
                                .append(theme.getBrackets(page + "/" + totalPages))));

        // Tab buttons — only show if both standings types are enabled
        if (league.isDriverStandingsEnabled() && league.isTeamStandingsEnabled()) {
            Component driverTab = Component.text(" [drivers]").color(isTeams ? theme.getPrimary() : theme.getButton())
                    .clickEvent(ClickEvent.runCommand("/league " + leagueName + " standings"));
            Component teamTab = Component.text(" [teams]").color(isTeams ? theme.getButton() : theme.getPrimary())
                    .clickEvent(ClickEvent.runCommand("/league " + leagueName + " standings teams"));
            header = header.append(driverTab).append(teamTab);
        }
        return header;
    }

    private static void sendNavigationButtons(Player player, String leagueName, int current,
            int total, boolean isTeams, Theme theme) {
        String base = "/league " + leagueName + " standings " + (isTeams ? "teams " : "");
        Component nav = Component.empty();

        nav = nav.append(current > 1
                ? Component.text("<<< ").color(theme.getButton()).clickEvent(ClickEvent.runCommand(base + (current - 1)))
                : Component.text("<<< ").color(theme.getPrimary()));

        nav = nav.append(Component.text("Page " + current + " of " + total).color(theme.getSecondary()));

        nav = nav.append(current < total
                ? Component.text(" >>>").color(theme.getButton()).clickEvent(ClickEvent.runCommand(base + (current + 1)))
                : Component.text(" >>>").color(theme.getPrimary()));

        player.sendMessage(nav);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // /league <league> calendar
    // ─────────────────────────────────────────────────────────────────────────

    public static boolean handleCalendar(Player player, League league) {
        Theme theme = Theme.getTheme(player);
        String leagueName = league.getName();
        boolean isAdmin = player.hasPermission("timingleague.admin");

        player.sendMessage("");
        player.sendMessage(theme.getRefreshButton()
                .clickEvent(ClickEvent.runCommand("/league " + leagueName + " calendar"))
                .append(Component.space())
                .append(theme.getTitleLine(Component.text(leagueName + " calendar").color(theme.getSecondary()))));

        List<CalendarEntry> entries = new ArrayList<>(league.getCalendarEntries());
        if (entries.isEmpty()) {
            player.sendMessage(Component.text("  No events in calendar.").color(theme.getPrimary()));
        }

        // Most recent at top
        Collections.reverse(entries);
        for (CalendarEntry entry : entries) {
            EventCategory cat = entry.hasCategory() ? league.getCategory(entry.getCategoryId()) : null;
            String catLabel = cat != null ? cat.getDisplayName()
                    : (entry.hasCategory() ? entry.getCategoryId() : "uncategorised");

            // Event name
            Component line = Component.text("  - " + entry.getEventName()).color(theme.getSecondary());

            // Heat pin indicator
            if (entry.hasPinnedHeat()) {
                line = line.append(Component.text("  [heat: " + entry.getHeatId() + "]").color(theme.getAward()));
            }

            // Category tag
            line = line.append(
                    Component.text("  [" + catLabel + "]").color(
                            entry.hasCategory() ? theme.getButton() : theme.getPrimary()));

            // Admin action buttons — one per line to keep it readable
            if (isAdmin) {
                line = line
                        // [heat]
                        .append(Component.text("  "))
                        .append(Component.text("[heat]").color(theme.getButton())
                                .clickEvent(ClickEvent.suggestCommand(
                                        "/league " + leagueName + " setEventHeat " + entry.getEventName() + " "))
                                .hoverEvent(HoverEvent.showText(Component.text(
                                        entry.hasPinnedHeat()
                                                ? "Pinned: " + entry.getHeatId() + "  (click to change or type 'clear')"
                                                : "Pin a specific heat (e.g. r1f1)"
                                ))))
                        // [cat]
                        .append(Component.text("  "))
                        .append(Component.text("[cat]").color(theme.getButton())
                                .clickEvent(ClickEvent.suggestCommand(
                                        "/league " + leagueName + " setEventCategory " + entry.getEventName() + " "))
                                .hoverEvent(HoverEvent.showText(Component.text("Set category"))))
                        // [remove]
                        .append(Component.text("  "))
                        .append(theme.getRemoveButton()
                                .clickEvent(ClickEvent.runCommand(
                                        "/league " + leagueName + " removeEvent " + entry.getEventName()))
                                .hoverEvent(HoverEvent.showText(Component.text("Remove from calendar"))));
            }
            player.sendMessage(line);
        }

        if (isAdmin) {
            player.sendMessage(theme.getAddButton(Component.text("Add event"))
                    .clickEvent(ClickEvent.suggestCommand("/league " + leagueName + " addEvent ")));
        }
        return true;
    }
}
