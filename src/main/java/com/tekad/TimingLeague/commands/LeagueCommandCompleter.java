package com.tekad.TimingLeague.commands;

import com.tekad.TimingLeague.League;
import com.tekad.TimingLeague.TImingLeague;
import com.tekad.TimingLeague.commands.subcommands.LeagueCategoryCommands;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LeagueCommandCompleter implements TabCompleter {

    private static final List<String> SCORING_SYSTEMS =
            List.of("fc1", "fc2", "default", "wibrs", "iec", "iecdouble", "iecopener", "linear");

    private final Map<String, League> leagues = TImingLeague.getLeagueMap();

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        boolean isAdmin = sender.hasPermission("timingleague.admin");

        // ── arg 1: league name or top-level commands ──────────────────────────
        if (args.length == 1) {
            if (isAdmin) { completions.add("create"); completions.add("delete"); }
            completions.add("help");
            completions.addAll(leagues.keySet());

        // ── arg 2: subcommands for a league ──────────────────────────────────
        } else if (args.length == 2 && leagues.containsKey(args[0])) {
            completions.addAll(List.of("team", "standings", "calendar", "help", "breakdown"));
            if (isAdmin) {
                completions.addAll(List.of(
                        "addDriver", "updatewithheat", "addevent", "removeevent",
                        "seteventcategory", "seteventheat",
                        "scoring", "holo", "points", "teamPoints", "teammode", "teamconfig",
                        "customscale", "mulligans", "category", "update", "predicteddrivers"
                ));
            }

        // ── arg 3 ─────────────────────────────────────────────────────────────
        } else if (args.length == 3 && leagues.containsKey(args[0])) {
            League league = leagues.get(args[0]);
            String sub = args[1].toLowerCase();

            switch (sub) {
                case "standings" -> {
                    completions.add("teams");
                    if (isAdmin) { completions.add("drivers"); }
                }
                case "team" -> completions.addAll(
                        List.of("create","color","add","remove","view","list","invite","accept",
                                "decline","promote","demote","leave","setpriority","removepriority",
                                "roster","setowner"));
                case "scoring" -> completions.addAll(SCORING_SYSTEMS);
                case "mulligans" -> { completions.addAll(List.of("0","1","2","3")); completions.add("team"); }
                case "category" -> completions.addAll(List.of("create","delete","list","info","scoring","mulligans","rename"));
                case "customscale" -> completions.addAll(List.of("set","list","use","clear"));
                case "holo" -> completions.addAll(List.of("driver","team","update","deleteClosest"));
                case "addevent" -> completions.add("<eventId>");
                case "removeevent" -> {
                    // complete with events already in the calendar
                    league.getCalendar().forEach(completions::add);
                }
                case "seteventcategory" -> {
                    league.getCalendar().forEach(completions::add);
                }
                case "seteventheat" -> {
                    league.getCalendar().forEach(completions::add);
                }
                case "breakdown" -> Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
                case "adddriver" -> Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
                case "points" -> Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
                case "teampoints" -> completions.addAll(league.getTeamsString());
                case "teammode" -> completions.addAll(List.of("MAIN_RESERVE","PRIORITY","HIGHEST"));
                case "updatewithheat" -> completions.add("<eventId>");
            }

        // ── arg 4 ─────────────────────────────────────────────────────────────
        } else if (args.length == 4 && leagues.containsKey(args[0])) {
            League league = leagues.get(args[0]);
            String sub  = args[1].toLowerCase();
            String sub2 = args[2].toLowerCase();

            switch (sub) {
                case "team" -> {
                    switch (sub2) {
                        case "color","setcolor","add","remove","view","setname","roster","setowner",
                             "invite","accept","decline","promote","demote" ->
                                completions.addAll(league.getTeamsString());
                        case "create" -> completions.add("<teamName>");
                    }
                }
                case "standings" -> {
                    if (isAdmin && (sub2.equals("drivers") || sub2.equals("teams"))) {
                        completions.addAll(List.of("true","false"));
                    } else {
                        // page numbers
                        completions.add("1");
                    }
                }
                case "mulligans" -> {
                    if (sub2.equals("team")) completions.addAll(List.of("true","false"));
                }
                case "category" -> {
                    // 4th arg is category id for info/scoring/mulligans/delete/rename
                    switch (sub2) {
                        case "info","scoring","mulligans","delete","rename" ->
                                completions.addAll(LeagueCategoryCommands.getCategoryIds(league));
                        case "create" -> completions.add("<categoryId>");
                    }
                }
                case "seteventcategory" -> {
                    completions.addAll(LeagueCategoryCommands.getCategoryIds(league));
                }
                case "seteventheat" -> {
                    // 4th arg is the heat id — suggest format hints + clear
                    completions.addAll(List.of("r1f1", "r1f2", "r2f1", "clear"));
                }
                case "addevent" -> {
                    // optional 4th arg: category id
                    completions.addAll(LeagueCategoryCommands.getCategoryIds(league));
                }
                case "updatewithheat" -> completions.add("<heatId>");
            }

        // ── arg 5 ─────────────────────────────────────────────────────────────
        } else if (args.length == 5 && leagues.containsKey(args[0])) {
            League league = leagues.get(args[0]);
            String sub  = args[1].toLowerCase();
            String sub2 = args[2].toLowerCase();
            String sub3 = args[3].toLowerCase();

            if (sub.equals("team")) {
                switch (sub2) {
                    case "color","setcolor" -> completions.add("#RRGGBB");
                    case "add" -> completions.addAll(List.of("main","reserve"));
                    case "remove" -> Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
                    case "setname" -> completions.add("<newTeamName>");
                    case "invite","setowner","promote","demote" ->
                            Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
                }
            }

            if (sub.equals("category")) {
                // 5th arg for scoring is the system name
                if (sub2.equals("scoring")) completions.addAll(SCORING_SYSTEMS);
                // 5th arg for mulligans is the count
                if (sub2.equals("mulligans")) completions.addAll(List.of("0","1","2","3"));
                // 5th arg for rename is the new display name
                if (sub2.equals("rename")) completions.add("<newDisplayName>");
            }
        }

        return completions;
    }
}
