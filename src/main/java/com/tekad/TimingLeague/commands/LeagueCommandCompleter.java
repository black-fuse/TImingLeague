package com.tekad.TimingLeague.commands;

import com.tekad.TimingLeague.League;
import com.tekad.TimingLeague.TImingLeague;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

// i made chatgpt do this as i am tired
public class LeagueCommandCompleter implements TabCompleter {

    private final Map<String, League> leagues = TImingLeague.getLeagueMap();

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("create");
            completions.add("help");
            completions.addAll(leagues.keySet()); // league names
        }

        else if (args.length == 2 && leagues.containsKey(args[0])) {
            completions.addAll(List.of(
                    "addDriver", "addEvent", "calendar", "update", "updateWithHeat",
                    "standings", "holo", "team"
            ));
        }

        else if (args.length == 3 && leagues.containsKey(args[0])) {
            String sub = args[1].toLowerCase();
            if (sub.equals("adddriver")) {
                for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
                    if (p.getName() != null) completions.add(p.getName());
                }
            } else if (sub.equals("addevent") || sub.equals("updatewithheat")) {
                completions.add("<eventId>");
            } else if (sub.equals("holo")) {
                completions.addAll(List.of("driver", "team", "update","deleteClosest"));
            } else if (sub.equals("team")) {
                completions.addAll(List.of("create", "color", "add", "remove", "view"));
                completions.addAll(leagues.get(args[0]).getTeamsString()); // team names
            } else if (sub.equals("scoring")){
                completions.addAll(List.of("fc1","fc2","default"));
            } else if (sub.equals("standings")){
                completions.addAll(List.of("teams"));
            }
        }

        else if (args.length == 4 && leagues.containsKey(args[0]) && args[1].equalsIgnoreCase("team")) {
            String teamSub = args[2].toLowerCase();
            if (teamSub.equals("create")) {
                completions.add("<teamName>");
            } else if (teamSub.equals("color")) {
                completions.addAll(leagues.get(args[0]).getTeamsString());
            } else if (teamSub.equals("add") || teamSub.equals("remove")) {
                completions.addAll(leagues.get(args[0]).getTeamsString());
            } else if (teamSub.equals("view")) {
                completions.addAll(leagues.get(args[0]).getTeamsString());
            }
        }

        else if (args.length == 5 && leagues.containsKey(args[0]) && args[1].equalsIgnoreCase("team")) {
            String teamSub = args[2].toLowerCase();
            if (teamSub.equals("color")) {
                completions.add("#RRGGBB");
            } else if (teamSub.equals("add") || teamSub.equals("remove")) {
                for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
                    if (p.getName() != null) completions.add(p.getName());
                }
            }
        }

        return completions;
    }
}
