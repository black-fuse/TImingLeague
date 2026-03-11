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
import java.util.stream.Collectors;

public class LeagueMulliganCommands {

    public static boolean handleMulligans(Player player, League league, String[] args) {
        Theme theme = Theme.getTheme(player);

        if (!player.hasPermission("timingleague.admin")) {
            player.sendMessage(theme.error("You do not have permission to run this command."));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(theme.error("Usage: /league <league> mulligans <count|team>"));
            return true;
        }

        String action = args[2].toLowerCase();

        if (action.equals("team")) {
            // Toggle team mulligans
            if (args.length < 4) {
                player.sendMessage(theme.error("Usage: /league <league> mulligans team <true|false>"));
                return true;
            }

            boolean enabled = Boolean.parseBoolean(args[3]);
            league.setTeamMulligansEnabled(enabled);
            
            player.sendMessage(theme.success("Team mulligans " + (enabled ? "enabled" : "disabled")));
            return true;
        }

        // Set mulligan count
        try {
            int count = Integer.parseInt(args[2]);
            
            if (count < 0) {
                player.sendMessage(theme.error("Mulligan count must be 0 or higher"));
                return true;
            }

            league.setMulliganCount(count);
            player.sendMessage(theme.success("Set mulligan count to " + count));
            
            if (count > 0) {
                player.sendMessage(theme.warning("Run /league " + league.getName() + " update to apply mulligans"));
            }
            
            return true;
        } catch (NumberFormatException e) {
            player.sendMessage(theme.error("Invalid number: " + args[2]));
            return true;
        }
    }

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

        // Group by event
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

        // Display
        player.sendMessage("");
        player.sendMessage(theme.getRefreshButton().clickEvent(ClickEvent.runCommand("/league " + leagueName + " breakdown " + target.getName()))
                .append(Component.space())
                .append(theme.getTitleLine(Component.text(target.getName() + "breakdown").color(theme.getSecondary())
                        .append(Component.space())
                )));

        // Show events
        for (Map.Entry<String, Integer> entry : eventTotals.entrySet()) {
            boolean isMulliganed = mulliganedEvents.contains(entry.getKey());
            
            Component line = Component.text(entry.getKey() + ": ").color(theme.getPrimary());
            
            if (isMulliganed) {
                line = line.append(Component.text(entry.getValue() + " pts")
                    .color(theme.getError())
                    .decorate(TextDecoration.STRIKETHROUGH));
            } else {
                line = line.append(Component.text(entry.getValue() + " pts").color(theme.getSecondary()));
            }
            
            player.sendMessage(line);
        }

        // Show manual adjustments
        if (!manualAdjustments.isEmpty()) {
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("Manual Adjustments:").color(theme.getSecondary()));
            
            int manualTotal = 0;
            Map<String, Integer> reasonTotals = new LinkedHashMap<>();
            
            for (PointEntry entry : manualAdjustments) {
                String reason = entry.getSource().replace("manual:", "");
                reasonTotals.merge(reason, entry.getPoints(), Integer::sum);
                manualTotal += entry.getPoints();
            }
            
            for (Map.Entry<String, Integer> entry : reasonTotals.entrySet()) {
                Component line = Component.text("  " + entry.getKey() + ": ").color(theme.getPrimary())
                    .append(Component.text((entry.getValue() > 0 ? "+" : "") + entry.getValue() + " pts").color(theme.getAward()));
                player.sendMessage(line);
            }
        }

        // Show total
        player.sendMessage(Component.empty());
        Component totalLine = Component.text("Final Total: ").color(theme.getPrimary())
            .append(Component.text(finalTotal + " pts").color(theme.getAward()));
        
        if (league.getMulliganCount() > 0) {
            totalLine = totalLine.append(Component.text(" (with " + mulliganedEvents.size() + " mulligan" + 
                (mulliganedEvents.size() == 1 ? "" : "s") + ")").color(theme.getPrimary()));
        }
        
        player.sendMessage(totalLine);

        return true;
    }
}
