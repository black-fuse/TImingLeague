package com.tekad.TimingLeague.commands.subcommands;

import com.tekad.TimingLeague.*;
import com.tekad.TimingLeague.ScoringSystems.*;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.theme.messages.Hover;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;

import java.util.*;


public class LeagueCategoryCommands {

    public static boolean handle(Player player, League league, String leagueName, String[] args) {
        Theme theme = Theme.getTheme(player);

        if (!player.hasPermission("timingleague.admin")) {
            player.sendMessage(theme.error("You do not have permission to manage categories."));
            return true;
        }

        if (args.length < 3) {
            sendUsage(player, theme, leagueName);
            return true;
        }

        String action = args[2].toLowerCase();
        return switch (action) {
            case "create"    -> handleCreate(player, league, leagueName, theme, args);
            case "delete"    -> handleDelete(player, league, leagueName, theme, args);
            case "list"      -> handleList(player, league, leagueName, theme);
            case "info"      -> handleInfo(player, league, leagueName, theme, args);
            case "scoring"   -> handleScoring(player, league, leagueName, theme, args);
            case "mulligans" -> handleMulligans(player, league, leagueName, theme, args);
            case "rename"    -> handleRename(player, league, leagueName, theme, args);
            default -> {
                sendUsage(player, theme, leagueName);
                yield true;
            }
        };
    }

    // ── create ───────────────────────────────────────────────────────────────

    private static boolean handleCreate(Player player, League league, String leagueName, Theme theme, String[] args) {
        if (args.length < 4) {
            player.sendMessage(theme.error("Usage: /league " + leagueName + " category create <id> [displayName]"));
            return true;
        }
        String id = args[3].toLowerCase();
        if (league.hasCategory(id)) {
            player.sendMessage(theme.error("Category '" + id + "' already exists."));
            return true;
        }
        String displayName = args.length >= 5
                ? String.join(" ", Arrays.copyOfRange(args, 4, args.length))
                : id;
        league.addCategory(new EventCategory(id, displayName, new BasicScoringSystem(), 0));
        player.sendMessage(theme.success("Created category '" + id + "' (" + displayName + ")."));
        player.sendMessage(theme.warning("Set its scoring: /league " + leagueName + " category scoring " + id + " <system>"));
        return true;
    }

    // ── delete ───────────────────────────────────────────────────────────────

    private static boolean handleDelete(Player player, League league, String leagueName, Theme theme, String[] args) {
        if (args.length < 4) {
            player.sendMessage(theme.error("Usage: /league " + leagueName + " category delete <id>"));
            return true;
        }
        String id = args[3].toLowerCase();
        if (!league.hasCategory(id)) {
            player.sendMessage(theme.error("Category '" + id + "' not found."));
            return true;
        }
        league.removeCategory(id);
        // Clear this category tag from any calendar events that reference it
        for (CalendarEntry entry : league.getCalendarEntries()) {
            if (id.equals(entry.getCategoryId())) {
                league.setEventCategory(entry.getEventName(), null);
            }
        }
        player.sendMessage(theme.success("Deleted category '" + id + "'. Affected calendar events are now uncategorised."));
        return true;
    }

    // ── list ─────────────────────────────────────────────────────────────────

    private static boolean handleList(Player player, League league, String leagueName, Theme theme) {
        player.sendMessage("");
        player.sendMessage(theme.getRefreshButton()
                .clickEvent(ClickEvent.runCommand("/league " + leagueName + " category list"))
                .append(Component.space())
                .append(theme.getTitleLine(Component.text(leagueName + " › categories").color(theme.getSecondary()))));

        Map<String, EventCategory> categories = league.getCategories();
        if (categories.isEmpty()) {
            player.sendMessage(Component.text("  No categories defined.").color(theme.getPrimary()));
        }

        for (EventCategory cat : categories.values()) {
            long eventCount = league.getCalendarEntries().stream()
                    .filter(e -> cat.getId().equals(e.getCategoryId()))
                    .count();

            Component line = Component.text("  [" + cat.getId() + "] ").color(theme.getSecondary())
                    .append(Component.text(cat.getDisplayName() + "  ").color(theme.getPrimary()))
                    .append(Component.text("scoring: ").color(theme.getPrimary()))
                    .append(theme.getBrackets(cat.getScoringSystemName()))
                    .append(Component.text("  mulligans: ").color(theme.getPrimary()))
                    .append(theme.getBrackets(String.valueOf(cat.getMulliganCount())))
                    .append(Component.text("  events: " + eventCount + "  ").color(theme.getPrimary()))
                    .append(theme.getViewButton(player)
                            .clickEvent(ClickEvent.runCommand("/league " + leagueName + " category info " + cat.getId())));
            player.sendMessage(line);
        }

        // Uncategorised events
        long uncatCount = league.getCalendarEntries().stream().filter(e -> !e.hasCategory()).count();
        if (uncatCount > 0) {
            Component uncatLine = Component.text("  [uncategorised] ").color(theme.getPrimary())
                    .append(Component.text(uncatCount + " event(s) — scoring: ").color(theme.getPrimary()))
                    .append(theme.getBrackets(EventCategory.scoringSystemNameOf(league.getScoringSystem())))
                    .append(Component.text("  mulligans: ").color(theme.getPrimary()))
                    .append(theme.getBrackets(String.valueOf(league.getMulliganCount())));
            player.sendMessage(uncatLine);
        }

        Component addBtn = theme.getAddButton(Component.text("Add category"))
                .clickEvent(ClickEvent.suggestCommand("/league " + leagueName + " category create "))
                .hoverEvent(HoverEvent.showText(Text.get(player, Hover.CLICK_TO_EDIT)));
        player.sendMessage(addBtn);
        return true;
    }

    // ── info ─────────────────────────────────────────────────────────────────

    private static boolean handleInfo(Player player, League league, String leagueName, Theme theme, String[] args) {
        if (args.length < 4) {
            player.sendMessage(theme.error("Usage: /league " + leagueName + " category info <id>"));
            return true;
        }
        String id = args[3].toLowerCase();
        EventCategory cat = league.getCategory(id);
        if (cat == null) {
            player.sendMessage(theme.error("Category '" + id + "' not found."));
            return true;
        }

        player.sendMessage("");
        player.sendMessage(theme.getRefreshButton()
                .clickEvent(ClickEvent.runCommand("/league " + leagueName + " category info " + id))
                .append(Component.space())
                .append(theme.getTitleLine(Component.text(leagueName + " › " + cat.getDisplayName()).color(theme.getSecondary()))));

        // Scoring
        Component scoringLine = Component.text("  scoring: ").color(theme.getPrimary())
                .append(theme.getBrackets(cat.getScoringSystemName())
                        .clickEvent(ClickEvent.suggestCommand("/league " + leagueName + " category scoring " + id + " "))
                        .hoverEvent(HoverEvent.showText(Text.get(player, Hover.CLICK_TO_EDIT))));
        player.sendMessage(scoringLine);

        // Mulligans
        Component mullLine = Component.text("  mulligans: ").color(theme.getPrimary())
                .append(theme.getBrackets(String.valueOf(cat.getMulliganCount()))
                        .clickEvent(ClickEvent.suggestCommand("/league " + leagueName + " category mulligans " + id + " "))
                        .hoverEvent(HoverEvent.showText(Text.get(player, Hover.CLICK_TO_EDIT))));
        player.sendMessage(mullLine);

        // Events in this category
        List<CalendarEntry> tagged = league.getCalendarEntries().stream()
                .filter(e -> id.equals(e.getCategoryId()))
                .toList();

        player.sendMessage(Component.text("  events (" + tagged.size() + "):").color(theme.getPrimary()));
        if (tagged.isEmpty()) {
            player.sendMessage(Component.text("    (none)").color(theme.getPrimary()));
        } else {
            for (CalendarEntry e : tagged) {
                player.sendMessage(Component.text("    - " + e.getEventName()).color(theme.getSecondary()));
            }
        }

        Component addEventBtn = theme.getAddButton(Component.text("Tag an event with this category"))
                .clickEvent(ClickEvent.suggestCommand("/league " + leagueName + " setEventCategory  " + id))
                .hoverEvent(HoverEvent.showText(Text.get(player, Hover.CLICK_TO_EDIT)));
        player.sendMessage(addEventBtn);

        return true;
    }

    // ── scoring ──────────────────────────────────────────────────────────────

    private static boolean handleScoring(Player player, League league, String leagueName, Theme theme, String[] args) {
        if (args.length < 5) {
            player.sendMessage(theme.error("Usage: /league " + leagueName + " category scoring <id> <system>"));
            player.sendMessage(theme.warning("Available: fc1  fc2  default  wibrs  iec  iecdouble  iecopener  linear"));
            return true;
        }
        String id = args[3].toLowerCase();
        EventCategory cat = league.getCategory(id);
        if (cat == null) {
            player.sendMessage(theme.error("Category '" + id + "' not found."));
            return true;
        }
        String sysName = args[4].toLowerCase();
        var system = EventCategory.scoringSystemFromName(sysName);
        cat.setScoringSystem(system);
        player.sendMessage(theme.success("Category '" + id + "' scoring set to: " + cat.getScoringSystemName()));
        return true;
    }

    // ── mulligans ────────────────────────────────────────────────────────────

    private static boolean handleMulligans(Player player, League league, String leagueName, Theme theme, String[] args) {
        if (args.length < 5) {
            player.sendMessage(theme.error("Usage: /league " + leagueName + " category mulligans <id> <count>"));
            return true;
        }
        String id = args[3].toLowerCase();
        EventCategory cat = league.getCategory(id);
        if (cat == null) {
            player.sendMessage(theme.error("Category '" + id + "' not found."));
            return true;
        }
        try {
            int count = Integer.parseInt(args[4]);
            if (count < 0) { player.sendMessage(theme.error("Mulligan count must be 0 or higher.")); return true; }
            cat.setMulliganCount(count);
            player.sendMessage(theme.success("Category '" + id + "' mulligan count set to " + count + "."));
            player.sendMessage(theme.warning("Run /league " + leagueName + " update to recalculate standings."));
        } catch (NumberFormatException e) {
            player.sendMessage(theme.error("Invalid number: " + args[4]));
        }
        return true;
    }

    // ── rename ───────────────────────────────────────────────────────────────

    private static boolean handleRename(Player player, League league, String leagueName, Theme theme, String[] args) {
        if (args.length < 5) {
            player.sendMessage(theme.error("Usage: /league " + leagueName + " category rename <id> <newDisplayName>"));
            return true;
        }
        String id = args[3].toLowerCase();
        EventCategory cat = league.getCategory(id);
        if (cat == null) {
            player.sendMessage(theme.error("Category '" + id + "' not found."));
            return true;
        }
        String newName = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
        cat.setDisplayName(newName);
        player.sendMessage(theme.success("Category '" + id + "' display name updated to: " + newName));
        return true;
    }

    // ── usage ────────────────────────────────────────────────────────────────

    private static void sendUsage(Player player, Theme theme, String leagueName) {
        player.sendMessage(theme.error("Usage: /league " + leagueName + " category <create|delete|list|info|scoring|mulligans|rename>"));
    }

    /** Returns all category IDs for a league — used by tab completer. */
    public static List<String> getCategoryIds(League league) {
        return new ArrayList<>(league.getCategories().keySet());
    }
}
