package com.tekad.TimingLeague.commands.subcommands;

import com.tekad.TimingLeague.League;
import com.tekad.TimingLeague.PointEntry;
import me.makkuusen.timing.system.theme.Theme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class LeagueMulliganCommands {

    /**
     * /league <league> mulligans ...
     *
     *   <count>                  – set the fallback mulligan count for uncategorised events
     *   team <true|false>        – toggle team mulligans
     *
     * Per-category mulligans are managed via:
     *   /league <league> category mulligans <id> <count>
     */
    public static boolean handleMulligans(Player player, League league, String[] args) {
        Theme theme = Theme.getTheme(player);

        if (!player.hasPermission("timingleague.admin")) {
            player.sendMessage(theme.error("You do not have permission to run this command."));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(theme.error("Usage: /league <league> mulligans <count|team>"));
            player.sendMessage(theme.warning("For per-category mulligans use: /league " + league.getName() + " category mulligans <id> <count>"));
            return true;
        }

        String action = args[2].toLowerCase();

        if (action.equals("team")) {
            if (args.length < 4) {
                player.sendMessage(theme.error("Usage: /league <league> mulligans team <true|false>"));
                return true;
            }
            boolean enabled = Boolean.parseBoolean(args[3]);
            league.setTeamMulligansEnabled(enabled);
            player.sendMessage(theme.success("Team mulligans " + (enabled ? "enabled" : "disabled") + "."));
            return true;
        }

        // Numeric: global fallback for uncategorised events
        try {
            int count = Integer.parseInt(args[2]);
            if (count < 0) {
                player.sendMessage(theme.error("Mulligan count must be 0 or higher."));
                return true;
            }
            league.setMulliganCount(count);
            player.sendMessage(theme.success("Global (uncategorised) mulligan count set to " + count + "."));
            if (!league.getCategories().isEmpty()) {
                player.sendMessage(theme.warning("This only affects uncategorised events. Use /league " + league.getName() + " category mulligans <id> <count> for category-specific mulligans."));
            }
            if (count > 0) {
                player.sendMessage(theme.warning("Run /league " + league.getName() + " update to apply mulligans."));
            }
        } catch (NumberFormatException e) {
            player.sendMessage(theme.error("Invalid action or number: '" + args[2] + "'"));
            player.sendMessage(theme.warning("Usage: /league <league> mulligans <count|team>"));
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Breakdown
    // ─────────────────────────────────────────────────────────────────────────

    public static boolean handleBreakdown(Player player, League league, String leagueName, String[] args) {
        Theme theme = Theme.getTheme(player);

        if (args.length < 3) {
            player.sendMessage(theme.error("Usage: /league <league> breakdown <player>"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        String uuid = target.getUniqueId().toString();

        List<PointEntry> entries = league.getDriverPointHistory().getOrDefault(uuid, new ArrayList<>());
        if (entries.isEmpty()) {
            player.sendMessage(theme.error("No point history found for " + target.getName()));
            return true;
        }

        // Group by event, separate manual adjustments
        Map<String, Integer> eventTotals = new LinkedHashMap<>();
        List<PointEntry> manualAdjustments = new ArrayList<>();

        for (PointEntry entry : entries) {
            if (entry.getEventId() != null) {
                eventTotals.merge(entry.getEventId(), entry.getPoints(), Integer::sum);
            } else {
                manualAdjustments.add(entry);
            }
        }

        List<String> mulliganedEvents = league.getDriverMulliganedEvents().getOrDefault(uuid, new ArrayList<>());
        int finalTotal = league.getDriverPoints(uuid);

        player.sendMessage("");
        player.sendMessage(theme.getRefreshButton()
                .clickEvent(ClickEvent.runCommand("/league " + leagueName + " breakdown " + target.getName()))
                .append(Component.space())
                .append(theme.getTitleLine(Component.text(target.getName() + " breakdown").color(theme.getSecondary()))));

        // Events grouped by category for readability
        Map<String, List<Map.Entry<String, Integer>>> byCategory = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : eventTotals.entrySet()) {
            String catKey = "(uncategorised)";
            var calEntry = league.getCalendarEntry(e.getKey());
            if (calEntry != null && calEntry.hasCategory()) {
                var cat = league.getCategory(calEntry.getCategoryId());
                catKey = cat != null ? cat.getDisplayName() : calEntry.getCategoryId();
            }
            byCategory.computeIfAbsent(catKey, k -> new ArrayList<>()).add(e);
        }

        for (Map.Entry<String, List<Map.Entry<String, Integer>>> catGroup : byCategory.entrySet()) {
            player.sendMessage(Component.text("  " + catGroup.getKey() + ":").color(theme.getSecondary()));
            for (Map.Entry<String, Integer> e : catGroup.getValue()) {
                boolean isMulliganed = mulliganedEvents.contains(e.getKey());
                Component line = Component.text("    " + e.getKey() + ": ").color(theme.getPrimary());
                if (isMulliganed) {
                    line = line.append(Component.text(e.getValue() + " pts")
                            .color(theme.getError())
                            .decorate(TextDecoration.STRIKETHROUGH));
                } else {
                    line = line.append(Component.text(e.getValue() + " pts").color(theme.getSecondary()));
                }
                player.sendMessage(line);
            }
        }

        if (!manualAdjustments.isEmpty()) {
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("  Manual Adjustments:").color(theme.getSecondary()));
            Map<String, Integer> reasonTotals = new LinkedHashMap<>();
            for (PointEntry e : manualAdjustments) {
                reasonTotals.merge(e.getSource().replace("manual:", ""), e.getPoints(), Integer::sum);
            }
            for (Map.Entry<String, Integer> e : reasonTotals.entrySet()) {
                player.sendMessage(Component.text("    " + e.getKey() + ": ").color(theme.getPrimary())
                        .append(Component.text((e.getValue() > 0 ? "+" : "") + e.getValue() + " pts").color(theme.getAward())));
            }
        }

        player.sendMessage(Component.empty());
        Component totalLine = Component.text("  Final Total: ").color(theme.getPrimary())
                .append(Component.text(finalTotal + " pts").color(theme.getAward()));
        if (!mulliganedEvents.isEmpty()) {
            totalLine = totalLine.append(Component.text(
                    " (with " + mulliganedEvents.size() + " mulligan" + (mulliganedEvents.size() == 1 ? "" : "s") + ")")
                    .color(theme.getPrimary()));
        }
        player.sendMessage(totalLine);
        return true;
    }
}
