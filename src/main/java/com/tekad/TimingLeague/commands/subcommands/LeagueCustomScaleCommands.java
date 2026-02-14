package com.tekad.TimingLeague.commands.subcommands;

import com.tekad.TimingLeague.League;
import com.tekad.TimingLeague.ScoringSystems.CustomScoringSystem;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import me.makkuusen.timing.system.theme.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LeagueCustomScaleCommands {

    public static boolean handleCustomScale(Player player, League league, String[] args) {
        Theme theme = Theme.getTheme(player);
        
        if (!player.hasPermission("timingleague.admin")) {
            player.sendMessage(theme.error("You do not have permission to run this command."));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(theme.error("Usage: /league <league> customscale <set|list|use|clear>"));
            return true;
        }

        String action = args[2].toLowerCase();

        switch (action) {
            case "set" -> {
                if (args.length < 5) {
                    player.sendMessage(theme.error("Usage: /league <league> customscale set <position> <points>"));
                    return true;
                }

                try {
                    int position = Integer.parseInt(args[3]);
                    int points = Integer.parseInt(args[4]);

                    if (position <= 0) {
                        player.sendMessage(theme.error("Position must be 1 or higher."));
                        return true;
                    }

                    league.setCustomScalePoint(position, points);
                    player.sendMessage(theme.success("Set position " + position + " = " + points + " points"));
                } catch (NumberFormatException e) {
                    player.sendMessage(theme.error("Invalid numbers provided."));
                }
                return true;
            }

            case "list" -> {
                if (!league.hasCustomScale()) {
                    player.sendMessage(theme.error("No custom scale configured for this league."));
                    return true;
                }

                Map<Integer, Integer> scale = league.getCustomScalePoints();
                player.sendMessage(theme.getTitleLine(Component.text("Custom Point Scale").color(theme.getSecondary())));

                scale.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> {
                            Component line = Component.text("Position " + entry.getKey()).color(theme.getPrimary())
                                    .append(theme.arrow())
                                    .append(Component.text(entry.getValue() + " points").color(theme.getAward()));
                            player.sendMessage(line);
                        });

                return true;
            }

            case "use" -> {
                if (!league.hasCustomScale()) {
                    player.sendMessage(theme.error("No custom scale configured. Use /league <league> customscale set <pos> <pts> first."));
                    return true;
                }

                // Convert Map<Integer, Integer> to List<Integer> for CustomScoringSystem
                List<Integer> pointsList = convertMapToList(league.getCustomScalePoints());
                league.setScoringSystem(new CustomScoringSystem(pointsList));

                player.sendMessage(theme.success("Now using custom point scale!"));
                return true;
            }

            case "clear" -> {
                league.clearCustomScale();
                player.sendMessage(theme.warning("Cleared custom point scale."));
                return true;
            }

            default -> {
                player.sendMessage(theme.error("Unknown action. Use: set, list, use, or clear"));
                return true;
            }
        }
    }

    private static List<Integer> convertMapToList(Map<Integer, Integer> scaleMap) {
        if (scaleMap.isEmpty()) {
            return new ArrayList<>();
        }

        // Find max position to determine list size
        int maxPosition = scaleMap.keySet().stream().max(Integer::compare).orElse(0);

        List<Integer> result = new ArrayList<>();
        for (int i = 1; i <= maxPosition; i++) {
            result.add(scaleMap.getOrDefault(i, 0));
        }

        return result;
    }
}
