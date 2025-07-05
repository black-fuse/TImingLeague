package com.tekad.TimingLeague.commands;

import com.tekad.TimingLeague.League;
import com.tekad.TimingLeague.TImingLeague;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LeagueCommandCompleter implements TabCompleter {

    private final Map<String, League> leagues = TImingLeague.getLeagueMap();

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        boolean isAdmin = sender.hasPermission("timingleague.admin");

        if (args.length == 1) {
            if (isAdmin) {
                completions.add("create");
                completions.add("delete");
            }
            completions.add("help");
            completions.addAll(leagues.keySet()); // league names
        }

        else if (args.length == 2 && leagues.containsKey(args[0])) {
            // All users can access these
            completions.add("team");
            completions.add("standings");
            completions.add("help");
            completions.add("team standings");

            if (isAdmin) {
                // Admin only commands
                completions.add("addDriver");
                completions.add("removeDriver");
                completions.add("update");
                completions.add("calendar");
                completions.add("scoring");
                completions.add("holo"); // hologram commands
                completions.add("team delete");
                completions.add("team create");
                completions.add("team setColor");
                completions.add("team setName");
            } else {
                // For non-admins, include only allowed team subcommands here
                completions.add("team create");
                completions.add("team delete");
                completions.add("team setColor");
                completions.add("team setName");
            }
        }

        else if (args.length == 3 && leagues.containsKey(args[0])) {
            String sub = args[1].toLowerCase();

            if (isAdmin) {
                if (sub.equals("adddriver")) {
                    for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
                        if (p.getName() != null) completions.add(p.getName());
                    }
                } else if (sub.equals("removeDriver")) {
                    for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
                        if (p.getName() != null) completions.add(p.getName());
                    }
                } else if (sub.equals("update") || sub.equals("calendar")) {
                    completions.add("<eventId>");
                } else if (sub.equals("holo")) {
                    completions.addAll(List.of("driver", "team", "update", "deleteClosest"));
                } else if (sub.equals("scoring")) {
                    completions.addAll(List.of("fc1", "fc2", "default"));
                }
            }

            if (sub.equals("team")) {
                completions.addAll(List.of("create", "color", "add", "remove", "view"));
                completions.addAll(leagues.get(args[0]).getTeamsString());
            } else if (sub.equals("standings")) {
                completions.addAll(List.of("teams"));
            }
        }

        else if (args.length == 4 && leagues.containsKey(args[0]) && args[1].equalsIgnoreCase("team")) {
            String teamSub = args[2].toLowerCase();
            if (teamSub.equals("create")) {
                completions.add("<teamName>");
            } else if (teamSub.equals("color") || teamSub.equals("setColor")) {
                completions.addAll(leagues.get(args[0]).getTeamsString());
            } else if (teamSub.equals("add") || teamSub.equals("remove")) {
                completions.addAll(leagues.get(args[0]).getTeamsString());
            } else if (teamSub.equals("view")) {
                completions.addAll(leagues.get(args[0]).getTeamsString());
            } else if (teamSub.equals("setName")) {
                completions.addAll(leagues.get(args[0]).getTeamsString());
            }
        }

        else if (args.length == 5 && leagues.containsKey(args[0]) && args[1].equalsIgnoreCase("team")) {
            String teamSub = args[2].toLowerCase();
            if (teamSub.equals("color") || teamSub.equals("setColor")) {
                completions.add("#RRGGBB");
            } else if (teamSub.equals("add") || teamSub.equals("remove")) {
                for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
                    if (p.getName() != null) completions.add(p.getName());
                }
            } else if (teamSub.equals("setName")) {
                completions.add("<newTeamName>");
            }
        }

        return completions;
    }
}
