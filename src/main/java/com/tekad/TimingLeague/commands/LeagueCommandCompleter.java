package com.tekad.TimingLeague.commands;

import com.tekad.TimingLeague.League;
import com.tekad.TimingLeague.TImingLeague;
import com.tekad.TimingLeague.Team;
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

        // ===== First arg: league name or admin commands =====
        if (args.length == 1) {
            if (isAdmin) {
                completions.add("create");
                completions.add("delete");
            }
            completions.add("help");
            completions.addAll(leagues.keySet()); // league names
        }

        // ===== Second arg: subcommands for a league =====
        else if (args.length == 2 && leagues.containsKey(args[0])) {
            completions.add("team");
            completions.add("standings");
            completions.add("help");

            if (isAdmin) {
                completions.add("addDriver");
                completions.add("removeDriver");
                completions.add("updatewithheat");
                completions.add("calendar");
                completions.add("scoring");
                completions.add("holo");
                completions.add("points");
            }
        }

        // ===== Third arg: team actions or other subs =====
        else if (args.length == 3 && leagues.containsKey(args[0])) {
            String sub = args[1].toLowerCase();

            if (isAdmin) {
                if (sub.equals("adddriver")) {
                    Bukkit.getOnlinePlayers().forEach(p -> {
                        if (p.getName() != null) completions.add(p.getName());
                    });
                } else if (sub.equals("removedriver")) {
                    for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
                        if (p.getName() != null) completions.add(p.getName());
                    }
                } else if (sub.equals("update") || sub.equals("calendar")) {
                    completions.add("<eventId>");
                } else if (sub.equals("holo")) {
                    completions.addAll(List.of("driver", "team", "update", "deleteClosest"));
                } else if (sub.equals("scoring")) {
                    completions.addAll(List.of("fc1", "fc2", "default"));
                } else if (sub.equals("points")) {
                    Bukkit.getOnlinePlayers().forEach(p -> {
                        if (p.getName() != null) completions.add(p.getName());
                    });
                } else if (sub.equals("updatewithheat")){
                    completions.add("<eventId>");
                }
            }

            if (sub.equals("team")) {
                completions.addAll(List.of("create", "color", "add", "remove", "view","invite","accept", "decline", "promote", "demote"));
                completions.addAll(leagues.get(args[0]).getTeamsString());
            } else if (sub.equals("standings")) {
                completions.addAll(List.of("teams"));
            }
        }

        // ===== Fourth arg: team sub-args =====
        else if (args.length == 4 && leagues.containsKey(args[0])) {

            if (args[1].equalsIgnoreCase("team")){
                String teamSub = args[2].toLowerCase();
                League league = leagues.get(args[0]);

                if (teamSub.equals("create")) {
                    completions.add("<teamName>");
                } else if (teamSub.equals("color") || teamSub.equals("setColor")) {
                    completions.addAll(league.getTeamsString());
                } else if (teamSub.equals("add") || teamSub.equals("remove") || teamSub.equals("view") || teamSub.equals("setName")) {
                    completions.addAll(league.getTeamsString());
                }
                else if (teamSub.equals("invite") || teamSub.equals("accept") || teamSub.equals("decline") || teamSub.equals("promote") || teamSub.equals("demote")) {
                    completions.addAll(league.getTeamsString());
                }
            }
            String subTwo = args[2].toLowerCase();
            if (args[1].equalsIgnoreCase("updatewithheat")){
                completions.add("<heatID>");
            }

        }

        // ===== Fifth arg: now supports role for 'add' =====
        else if (args.length == 5 && leagues.containsKey(args[0]) && args[1].equalsIgnoreCase("team")) {
            String teamSub = args[2].toLowerCase();

            if (teamSub.equals("color") || teamSub.equals("setColor")) {
                completions.add("#RRGGBB");
            }
            else if (teamSub.equals("add")) {
                completions.addAll(List.of("main", "reserve")); // <-- NEW role options
            }
            else if (teamSub.equals("remove")) {
                for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
                    if (p.getName() != null) completions.add(p.getName());
                }
            }
            else if (teamSub.equals("setName")) {
                completions.add("<newTeamName>");
            }
            else if (teamSub.equals("invite")) {
                for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
                    if (p.getName() != null) completions.add(p.getName());
                }
            }
            else if (teamSub.equals("add")) {
                for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
                    if (p.getName() != null) completions.add(p.getName());
                }
            }
        }

        return completions;
    }
}
