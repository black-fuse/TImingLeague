package com.tekad.TimingLeague.commands;

import com.tekad.TimingLeague.*;
import me.makkuusen.timing.system.api.EventResultsAPI;
import me.makkuusen.timing.system.api.event.HeatResult;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class leagueCommand implements CommandExecutor {

    private final Map<String, League> leagues = TImingLeague.getLeagueMap();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by players.");
            return true;
        }

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create" -> {
                if (args.length < 2) {
                    player.sendMessage("Usage: /league create <leagueName>");
                    return true;
                }

                String leagueName = args[1];
                if (leagues.containsKey(leagueName)) {
                    player.sendMessage("A league with that name already exists.");
                    return true;
                }

                League league = new League(leagueName, 20);
                leagues.put(leagueName, league);
                player.sendMessage("Created league: " + leagueName);
            }

            case "help" -> showHelp(player);

            default -> {
                // Assume first argument is league name
                String leagueName = args[0];
                League league = leagues.get(leagueName);

                if (league == null) {
                    player.sendMessage("League '" + leagueName + "' not found.");
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage("You must specify an action for league '" + leagueName + "'. Try /league help");
                    return true;
                }

                String action = args[1].toLowerCase();

                switch (action) {
                    case "adddriver" -> {
                        if (args.length < 3) {
                            player.sendMessage("Usage: /league <leagueName> addDriver <username>");
                            return true;
                        }

                        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                        UUID uuid = target.getUniqueId();
                        league.addDriver(uuid.toString(), league.NoTeam);
                        player.sendMessage("Added driver to league (note: this happens automatically after racing).");
                    }

                    case "update" -> {
                        league.updateStandings();
                        player.sendMessage("Standings updated for league: " + leagueName);
                    }

                    case "addevent" -> {
                        if (args.length < 3) {
                            player.sendMessage("Usage: /league <leagueName> addEvent <eventId>");
                            return true;
                        }

                        String eventId = args[2];
                        if (EventResultsAPI.getEventResult(eventId) == null) {
                            player.sendMessage("Event does not exist: " + eventId);
                            return true;
                        }

                        league.addEvent(eventId);
                        player.sendMessage("Event added to league calendar: " + eventId);
                    }

                    case "calendar" -> {
                        StringBuilder builder = new StringBuilder("=== " + leagueName + " Calendar ===\n");
                        for (String event : league.getCalendar()) {
                            builder.append("- ").append(event).append("\n");
                        }
                        player.sendMessage(builder.toString());
                    }

                    case "updatewithheat" -> {
                        if (args.length < 4) {
                            player.sendMessage("Usage: /league <leagueName> updateWithHeat <eventId> <heatId>");
                            return true;
                        }

                        try {
                            DefaultStandingsUpdater updater = (DefaultStandingsUpdater) league.getUpdater();
                            HeatResult heat = updater.getHeatResults(args[2], args[3]);
                            updater.updateStandingsWithHeat(heat, league);
                            player.sendMessage("Standings updated from heat: " + args[3]);
                        } catch (Exception e) {
                            player.sendMessage("Failed to update from heat: " + e.getMessage());
                        }
                    }

                    case "standings" -> {
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

                            player.sendMessage("=== Team Standings (Page " + page + ") ===");
                            for (int i = start; i < Math.min(start + pageSize, standings.size()); i++) {
                                var entry = standings.get(i);
                                player.sendMessage((i + 1) + ". " + entry.getKey() + " - " + entry.getValue() + " pts");
                            }

                        } else {
                            var standings = league.getDriverStandings().entrySet().stream()
                                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                                    .toList();

                            if (start >= standings.size()) {
                                player.sendMessage("Page out of range.");
                                return true;
                            }

                            player.sendMessage("=== Driver Standings (Page " + page + ") ===");
                            for (int i = start; i < Math.min(start + pageSize, standings.size()); i++) {
                                var entry = standings.get(i);
                                OfflinePlayer p = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey()));
                                player.sendMessage((i + 1) + ". " + p.getName() + " - " + entry.getValue() + " pts");
                            }
                        }
                    }

                    case "holo" -> {
                        if (!(sender instanceof Player p)) {
                            sender.sendMessage("Only players can place holograms.");
                            return true;
                        }

                        if (args.length < 3) {
                            player.sendMessage("Usage: /league <leagueName> holo <driver|team> [page]");
                            return true;
                        }

                        boolean teamMode = args[2].equalsIgnoreCase("team");
                        int page = 1;
                        if (args.length >= 4) {
                            try {
                                page = Integer.parseInt(args[3]);
                            } catch (NumberFormatException ignored) {}
                        }

                        int pageSize = 15;
                        int start = (page - 1) * pageSize;
                        List<String> lines;

                        if (teamMode) {
                            var standings = league.getTeamStandings().entrySet().stream()
                                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                                    .toList();

                            if (start >= standings.size()) {
                                player.sendMessage("Page out of range.");
                                return true;
                            }

                            lines = standings.subList(start, Math.min(start + pageSize, standings.size())).stream()
                                    .map(entry -> {
                                        int index = standings.indexOf(entry) + 1;
                                        return "&e#" + index + ". &b" + entry.getKey() + " &7- &a" + entry.getValue() + " pts";
                                    }).toList();

                        } else {
                            var standings = league.getDriverStandings().entrySet().stream()
                                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                                    .toList();

                            if (start >= standings.size()) {
                                player.sendMessage("Page out of range.");
                                return true;
                            }

                            lines = standings.subList(start, Math.min(start + pageSize, standings.size())).stream()
                                    .map(entry -> {
                                        int index = standings.indexOf(entry) + 1;
                                        String name = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey())).getName();
                                        return "&e#" + index + ". &b" + (name != null ? name : "Unknown") + " &7- &a" + entry.getValue() + " pts";
                                    }).toList();
                        }

                        LeagueHologramManager manager = new LeagueHologramManager();
                        manager.createOrUpdateHologram(p.getLocation(), lines);
                        player.sendMessage("Leaderboard hologram placed.");
                    }



                    case "team" -> {
                        if (args.length < 3) {
                            player.sendMessage("Usage: /league <leagueName> team <create|color|add|remove|view> [arguments]");
                            return true;
                        }

                        String teamAction = args[2].toLowerCase();

                        switch (teamAction) {
                            case "create" -> {
                                if (args.length < 4) {
                                    player.sendMessage("Usage: /league <leagueName> team create <teamName> [hexColor]");
                                    return true;
                                }
                                String teamName = args[3];
                                String color = (args.length >= 5) ? args[4] : "FFFFFF";

                                if (league.getTeam(teamName) != null) {
                                    player.sendMessage("A team with that name already exists.");
                                    return true;
                                }

                                Team team = new Team(teamName, color, league);
                                league.addTeam(team);
                                player.sendMessage("Created team: " + teamName + " with color #" + color);
                            }

                            case "color" -> {
                                if (args.length < 5) {
                                    player.sendMessage("Usage: /league <leagueName> team color <teamName> <hexColor>");
                                    return true;
                                }

                                Team team = league.getTeam(args[3]);
                                if (team == null) {
                                    player.sendMessage("Team not found: " + args[3]);
                                    return true;
                                }

                                team.setColor(args[4]);
                                player.sendMessage("Updated team color to #" + args[4]);
                            }

                            case "add" -> {
                                if (args.length < 5) {
                                    player.sendMessage("Usage: /league <leagueName> team add <teamName> <playerName>");
                                    return true;
                                }

                                Team team = league.getTeam(args[3]);
                                if (team == null) {
                                    player.sendMessage("Team not found: " + args[3]);
                                    return true;
                                }

                                OfflinePlayer target = Bukkit.getOfflinePlayer(args[4]);
                                String uuid = target.getUniqueId().toString();

                                league.addDriver(uuid, team);
                                player.sendMessage("Added player to team: " + args[4]);
                            }

                            case "remove" -> {
                                if (args.length < 5) {
                                    player.sendMessage("Usage: /league <leagueName> team remove <teamName> <playerName>");
                                    return true;
                                }

                                Team team = league.getTeam(args[3]);
                                if (team == null) {
                                    player.sendMessage("Team not found: " + args[3]);
                                    return true;
                                }

                                OfflinePlayer target = Bukkit.getOfflinePlayer(args[4]);
                                String uuid = target.getUniqueId().toString();

                                if (!team.getMembers().contains(uuid)) {
                                    player.sendMessage("Player is not in that team.");
                                    return true;
                                }

                                league.addDriver(uuid, league.NoTeam); // move to NoTeam
                                player.sendMessage("Removed player from team: " + args[4]);
                            }

                            case "view" -> {
                                if (args.length < 4) {
                                    player.sendMessage("Usage: /league <leagueName> team view <teamName>");
                                    return true;
                                }

                                Team team = league.getTeam(args[3]);
                                if (team == null) {
                                    player.sendMessage("Team not found: " + args[3]);
                                    return true;
                                }

                                StringBuilder sb = new StringBuilder();
                                sb.append("=== ").append(team.getName()).append(" ===\n");
                                sb.append("Color: #").append(team.getColor()).append("\n");
                                sb.append("Members:\n");

                                for (String uuid : team.getMembers()) {
                                    OfflinePlayer member = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
                                    sb.append("- ").append(member.getName()).append("\n");
                                }

                                player.sendMessage(sb.toString());
                            }

                            default -> {
                                // Shorthand: /league <league> team <teamName>
                                Team team = league.getTeam(args[2]);
                                if (team == null) {
                                    player.sendMessage("Team not found: " + args[2]);
                                    return true;
                                }

                                StringBuilder sb = new StringBuilder();
                                sb.append("=== ").append(team.getName()).append(" ===\n");
                                sb.append("Color: #").append(team.getColor()).append("\n");
                                sb.append("Members:\n");

                                for (String uuid : team.getMembers()) {
                                    OfflinePlayer member = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
                                    sb.append("- ").append(member.getName()).append("\n");
                                }

                                player.sendMessage(sb.toString());
                            }
                        }
                    }


                    default -> player.sendMessage("Unknown subcommand for league '" + leagueName + "'. Try /league help");
                }
            }
        }

        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage("""
            === /league Command Help ===
            /league create <leagueName> - Create a new league
            /league <leagueName> addDriver <username> - Manually add a driver to the league
            /league <leagueName> addEvent <eventId> - Add an event to the league calendar
            /league <leagueName> calendar - View the league's event calendar
            /league <leagueName> update - Update standings using all league events
            /league <leagueName> updateWithHeat <eventId> <heatId> - Update standings using a specific heat
            /league <leagueName> team create <teamName> [hexColor] - Create a team with an optional hex color
            /league <leagueName> team color <teamName> <hexColor> - Change a team's color
            /league <leagueName> team add <teamName> <playerName> - Add a player to a team
            /league <leagueName> team remove <teamName> <playerName> - Remove a player from a team
            /league <leagueName> team view <teamName> - View a team and its members
            /league <leagueName> team <teamName> - Shorthand to view a team
            /league <leagueName> standings [page] - View driver standings with pagination
            /league <leagueName> standings teams [page] - View team standings with pagination
            /league help - Show this help message
            """);
    }

}
