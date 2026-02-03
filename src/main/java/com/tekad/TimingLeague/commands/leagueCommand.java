package com.tekad.TimingLeague.commands;

import com.tekad.TimingLeague.*;
import com.tekad.TimingLeague.ScoringSystems.*;
import eu.decentsoftware.holograms.api.DHAPI;
import me.makkuusen.timing.system.api.EventResultsAPI;
import me.makkuusen.timing.system.api.event.HeatResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class leagueCommand implements CommandExecutor {

    private final Map<String, League> leagues = TImingLeague.getLeagueMap();
    private final List<String> holograms = TImingLeague.getHolograms();
    // leagueName -> (teamName -> set of driver UUIDs)
    private final Map<String, Map<String, Set<String>>> pendingInvites = new HashMap<>();



    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by players.");
            return true;
        }

        if (!sender.hasPermission("timingleague.view")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create" -> {
                if (!sender.hasPermission("timingleague.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to create leagues.");
                    return true;
                }

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

            case "delete" -> {
                if (!sender.hasPermission("timingleague.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to delete leagues.");
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage("Usage: /league delete <leagueName>");
                    return true;
                }

                String leagueName = args[1];
                if (!leagues.containsKey(leagueName)) {
                    player.sendMessage("A league with that name does not exist.");
                    return true;
                }

                leagues.remove(leagueName);
                player.sendMessage("Deleted league: " + leagueName);
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
                        if (!sender.hasPermission("timingleague.admin")) {
                            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
                            return true;
                        }

                        try{
                            league.updateStandings();

                        }catch (Exception e){
                            league.setScoringSystem(new BasicScoringSystem());
                        }
                        player.sendMessage("Standings updated for league: " + leagueName);
                    }

                    case "addevent" -> {
                        if (!sender.hasPermission("timingleague.admin")) {
                            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
                            return true;
                        }

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
                        if (!sender.hasPermission("timingleague.admin")) {
                            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
                            return true;
                        }

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

                        createOrUpdateHolograms(league, 1, player, "update");
                    }

                    case "scoring" ->{
                        if (!sender.hasPermission("timingleague.admin")) {
                            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
                            return true;
                        }

                        if (args[2].equalsIgnoreCase("fc1")) {
                            league.setScoringSystem(new FC1ScoringSystem());
                        } else if (args[2].equalsIgnoreCase("fc2")) {
                            league.setScoringSystem(new FC2ScoringSystem());
                        } else if (args[2].equalsIgnoreCase("default")) {
                            league.setScoringSystem(new BasicScoringSystem());
                        } else if (args[2].equalsIgnoreCase("WIBRS")) {
                            league.setScoringSystem(new WIBRSScoringSystem());
                        } else if (args[2].equalsIgnoreCase("IEC")) {
                            league.setScoringSystem(new IECScoringSystem());
                        }else if (args[2].equalsIgnoreCase("IECDouble")) {
                            league.setScoringSystem(new IECDouble());
                        } else if (args[2].equalsIgnoreCase("IECOpener")) {
                            league.setScoringSystem(new IECOpenerSystem());
                        } else {
                            player.sendMessage("not a valid scoring system");
                            player.sendMessage("Current scoring system: " + league.getScoringSystem().toString());
                        }
                    }

                    case "predicteddrivers" -> {
                        if (!sender.hasPermission("timingleague.admin")) {
                            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
                            return true;
                        }

                        ScoringSystem system = league.getScoringSystem();

                        if (args.length > 2) {
                            try {
                                int predictedDrivers = Integer.parseInt(args[2]);
                                league.setPredictedDrivers(predictedDrivers);
                            } catch (NumberFormatException e) {
                                sender.sendMessage(ChatColor.RED + "Please provide a valid number for predicted drivers.");
                            }
                        } else{
                            player.sendMessage("predicted Drivers: " + league.getPredictedDriverCount());
                        }
                    }

                    case "points" -> {
                        if (!sender.hasPermission("timingleague.admin")) {
                            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
                            return true;
                        }

                        if (args.length < 4) {
                            sender.sendMessage(ChatColor.RED + "Format: /league <leaguename> points <driver> <+/-num>");
                            return true;
                        }

                        int points;
                        try {
                            points = Integer.parseInt(args[3]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "Invalid number: " + args[3]);
                            return true;
                        }

                        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                        String uuid = target.getUniqueId().toString();

                        league.addPointsToDriver(uuid, points);
                        createOrUpdateHolograms(league, 1, player, "update");
                        updateHolograms(league,1, player);
                    }

                    case "teampoints" -> {
                        if (!sender.hasPermission("timingleague.admin")) {
                            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
                            return true;
                        }

                        if (args.length < 4) {
                            sender.sendMessage(ChatColor.RED + "Format: /league <leaguename> points <teamName> <+/-num>");
                            return true;
                        }

                        int points;
                        try {
                            points = Integer.parseInt(args[3]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "Invalid number: " + args[3]);
                            return true;
                        }

                        league.addPointsToTeam(args[2], points);
                        createOrUpdateHolograms(league, 1, player, "update");
                        updateHolograms(league,1, player);
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
                        if (!sender.hasPermission("timingleague.admin")) {
                            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
                            return true;
                        }

                        if (!(sender instanceof Player p)) {
                            sender.sendMessage("Only players can place holograms.");
                            return true;
                        }

                        if (args.length < 3) {
                            player.sendMessage("Usage: /league <leagueName> holo <driver|team> [page]");
                            return true;
                        }


                        int page = 1;
                        if (args.length >= 4) {
                            try {
                                page = Integer.parseInt(args[3]);
                            } catch (NumberFormatException ignored) {}
                        }

                        if (args[2].equalsIgnoreCase("deleteClosest")){
                            Location playerLocation = player.getLocation();
                            double closestDistance = Double.MAX_VALUE;
                            Hologram closest = null;

                            for (String holoName : holograms){
                                Hologram holo = DHAPI.getHologram(holoName);
                                if (!holo.getLocation().getWorld().equals(playerLocation.getWorld())) continue;

                                double distance = holo.getLocation().distanceSquared(playerLocation); // faster than .distance
                                if (distance < closestDistance) {
                                    closestDistance = distance;
                                    closest = holo;
                                }
                            }

                            if (closest != null){
                                closest.delete();
                                player.sendMessage("successfully deleted " + closest.getName());
                            }
                            else {
                                player.sendMessage("there is no hologram to delete");
                            }
                            holograms.remove(closest.getName());

                            return true;
                        }

                        // if holograms are broken, this is probably why
                        return createOrUpdateHolograms(league, page, player, args[2]);
                    }

                    case "teammode" -> {
                        if (!sender.hasPermission("timingleague.admin")) {
                            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
                            return true;
                        }

                        if (args.length < 3) {
                            player.sendMessage("Current team mode: " + league.getTeamMode());
                            player.sendMessage("Usage: /league <league> teammode <MAIN_RESERVE|PRIORITY|HIGHEST>");
                            return true;
                        }

                        try {
                            TeamMode mode = TeamMode.valueOf(args[2].toUpperCase());
                            league.setTeamMode(mode);
                            player.sendMessage("Team mode set to: " + mode);
                        } catch (IllegalArgumentException e) {
                            player.sendMessage(ChatColor.RED + "Invalid mode. Options: MAIN_RESERVE, PRIORITY, HIGHEST");
                        }
                    }

                    case "teamconfig" -> {
                        if (!sender.hasPermission("timingleague.admin")) {
                            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
                            return true;
                        }

                        if (args.length < 4) {
                            player.sendMessage("Current config - Max size: " + league.getTeamMaxSize() +
                                    ", Scoring count: " + league.getTeamScoringCount());
                            player.sendMessage("Usage: /league <league> teamconfig <maxSize> <scoringCount>");
                            return true;
                        }

                        try {
                            int maxSize = Integer.parseInt(args[2]);
                            int scoringCount = Integer.parseInt(args[3]);

                            league.setTeamMaxSize(maxSize);
                            league.setTeamScoringCount(scoringCount);

                            // Update all teams
                            for (Team team : league.getTeams()) {
                                team.setCountedPrioDrivers(scoringCount);
                            }

                            player.sendMessage("Team config updated - Max: " + maxSize + ", Scoring: " + scoringCount);
                        } catch (NumberFormatException e) {
                            player.sendMessage(ChatColor.RED + "Invalid numbers provided.");
                        }
                    }

                    case "team" -> {
                        if (args.length < 3) {
                            player.sendMessage("Usage: /league <leagueName> team <create|color|add|remove|view|list> [arguments]");
                            return true;
                        }

                        String teamAction = args[2].toLowerCase();

                        // ===== List Teams =====
                        if (teamAction.equals("list")) {
                            Set<Team> teamsList = league.getTeams();
                            if (teamsList.isEmpty()) {
                                player.sendMessage("§cThere are no teams in this league.");
                                return true;
                            }

                            StringBuilder toSend = new StringBuilder("§6== Teams List ==§r");
                            for (Team team : teamsList) {
                                String square = getColoredSquare(team.getColor());
                                toSend.append("\n").append(square).append(" ").append(team.getName());
                            }
                            player.sendMessage(toSend.toString());
                            return true;
                        }

                        switch (teamAction) {
                            // ===== Create =====
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
                                team.setOwner(((Player) sender).getUniqueId().toString());
                                league.addTeam(team);
                                player.sendMessage("Created team: " + teamName + " with color #" + color);
                            }

                            // ===== Change Color =====
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

                            // ===== Add Member =====
                            case "add" -> {
                                if (args.length < 6) {
                                    player.sendMessage("Usage: /league <leagueName> team add <teamName> <main|reserve> <playerName>");
                                    return true;
                                }

                                if (!sender.hasPermission("timingleague.admin")) {
                                    sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
                                    return true;
                                }

                                Team team = league.getTeam(args[3]);
                                if (team == null) {
                                    player.sendMessage("Team not found: " + args[3]);
                                    return true;
                                }

                                String type = args[4].toLowerCase();
                                OfflinePlayer target = Bukkit.getOfflinePlayer(args[5]);
                                String uuid = target.getUniqueId().toString();

                                boolean success;
                                if (type.equals("main")) {
                                    success = league.addMainDriverToTeam(uuid, team.getName());
                                } else if (type.equals("reserve")) {
                                    success = league.addReserveDriverToTeam(uuid, team.getName());
                                } else {
                                    player.sendMessage("Invalid type. Use 'main' or 'reserve'.");
                                    success = league.addDriverToTeam(uuid, team.getName(), 99999);
                                    //return true;
                                }

                                if (success) {
                                    player.sendMessage("Added " + target.getName() + " to team " + team.getName() + " as " + type + ".");
                                } else {
                                    player.sendMessage("Could not add player (team full or already in team).");
                                }
                            }

                            // ===== Remove Member =====
                            case "remove" -> {
                                if (args.length < 5) {
                                    player.sendMessage("Usage: /league <leagueName> team remove <teamName> <playerName>");
                                    return true;
                                }

                                Team team = league.getTeam(args[3]);
                                if (!((!sender.hasPermission("timingleague.admin")) | (!team.getOwner().equals(((Player) sender).getUniqueId().toString())))) {
                                    sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
                                    return true;
                                }


                                if (team == null) {
                                    player.sendMessage("Team not found: " + args[3]);
                                    return true;
                                }

                                OfflinePlayer target = Bukkit.getOfflinePlayer(args[4]);
                                String uuid = target.getUniqueId().toString();

                                if (!team.isMember(uuid)) {
                                    player.sendMessage("Player is not in that team.");
                                    return true;
                                }

                                team.removeMember(uuid);
                                player.sendMessage("Removed player " + target.getName() + " from team " + team.getName() + ".");
                            }

                            case "roster" -> {
                                if (args.length < 4) {
                                    player.sendMessage("Usage: /league <league> team roster <teamName>");
                                    return true;
                                }

                                Team team = league.getTeam(args[3]);
                                if (team == null) {
                                    player.sendMessage("Team not found: " + args[3]);
                                    return true;
                                }

                                sendTeamDetails(player, team, league);
                            }

                            case "setpriority" -> {
                                if (args.length < 6) {
                                    player.sendMessage("Usage: /league <league> team setpriority <teamName> <player> <position>");
                                    return true;
                                }

                                Team team = league.getTeam(args[3]);
                                if (team == null) {
                                    player.sendMessage("Team not found: " + args[3]);
                                    return true;
                                }

                                // Permission check: owner or admin
                                String owner = team.getOwner();
                                if (!sender.hasPermission("timingleague.admin") &&
                                        (owner == null || !owner.equals(player.getUniqueId().toString()))) {
                                    player.sendMessage(ChatColor.RED + "You do not have permission over this team.");
                                    return true;
                                }

                                // Must be in PRIORITY or HIGHEST mode
                                if (league.getTeamMode() != TeamMode.PRIORITY && league.getTeamMode() != TeamMode.HIGHEST) {
                                    player.sendMessage(ChatColor.RED + "This league is not using PRIORITY or HIGHEST mode.");
                                    return true;
                                }

                                OfflinePlayer target = Bukkit.getOfflinePlayer(args[4]);
                                String uuid = target.getUniqueId().toString();

                                try {
                                    int position = Integer.parseInt(args[5]) - 1; // Convert to 0-indexed

                                    if (position < 0) {
                                        player.sendMessage(ChatColor.RED + "Position must be 1 or higher.");
                                        return true;
                                    }

                                    boolean success = team.addPriorityDriver(uuid, position);
                                    league.addDriver(uuid, team); // Ensure driver is tracked

                                    if (success) {
                                        player.sendMessage(ChatColor.GREEN + target.getName() + " set to priority position " +
                                                (position + 1) + " on team " + team.getName());
                                    } else {
                                        player.sendMessage(ChatColor.RED + "Could not set priority (team might be full).");
                                    }
                                } catch (NumberFormatException e) {
                                    player.sendMessage(ChatColor.RED + "Invalid position number.");
                                }
                            }

                            case "removepriority" -> {
                                if (args.length < 5) {
                                    player.sendMessage("Usage: /league <league> team removepriority <teamName> <player>");
                                    return true;
                                }

                                Team team = league.getTeam(args[3]);
                                if (team == null) {
                                    player.sendMessage("Team not found: " + args[3]);
                                    return true;
                                }

                                // Permission check: owner or admin
                                String owner = team.getOwner();
                                if (!sender.hasPermission("timingleague.admin") &&
                                        (owner == null || !owner.equals(player.getUniqueId().toString()))) {
                                    player.sendMessage(ChatColor.RED + "You do not have permission over this team.");
                                    return true;
                                }

                                OfflinePlayer target = Bukkit.getOfflinePlayer(args[4]);
                                String uuid = target.getUniqueId().toString();

                                boolean removed = team.removeMember(uuid);

                                if (removed) {
                                    player.sendMessage(ChatColor.GREEN + target.getName() + " removed from team " + team.getName());
                                } else {
                                    player.sendMessage(ChatColor.RED + "Player is not on this team.");
                                }
                            }


                            case "invite" -> {
                                if (args.length < 5) {
                                    player.sendMessage("Usage: /league <leagueName> team invite <teamName> <driverName>");
                                    return true;
                                }

                                String teamName = args[3];
                                String driverName = args[4];

                                Team team = league.getTeam(teamName);
                                if (team == null) {
                                    player.sendMessage("Team not found: " + teamName);
                                    return true;
                                }

                                // Only owner or admin can invite
                                String owner = team.getOwner();
                                if (owner == null || !owner.equals(player.getUniqueId().toString())) {
                                    player.sendMessage(ChatColor.RED + "You do not have permission over this team.");
                                    return true;
                                }


                                OfflinePlayer target = Bukkit.getOfflinePlayer(driverName);
                                String driverUUID = target.getUniqueId().toString();

                                pendingInvites
                                        .computeIfAbsent(leagueName, k -> new HashMap<>())
                                        .computeIfAbsent(teamName, k -> new HashSet<>())
                                        .add(driverUUID);

                                if (target.isOnline()) {
                                    target.getPlayer().sendMessage(ChatColor.GREEN + "You’ve been invited to join team " +
                                            teamName + " in league " + leagueName +
                                            ". Use /league " + leagueName + " team " + " accept " + teamName);
                                }

                                player.sendMessage("Invite sent to " + driverName + " for team " + teamName + ".");
                            }

                            case "accept" -> {
                                if (args.length < 4) {
                                    player.sendMessage("Usage: /league <leagueName> team accept <teamName>");
                                    return true;
                                }

                                String teamName = args[3];
                                Team team = league.getTeam(teamName);
                                if (team == null) {
                                    player.sendMessage("Team not found: " + teamName);
                                    return true;
                                }

                                String playerUUID = player.getUniqueId().toString();

                                // Check if player was invited
                                Set<String> invites = pendingInvites
                                        .getOrDefault(leagueName, Collections.emptyMap())
                                        .getOrDefault(teamName, Collections.emptySet());

                                if (!invites.contains(playerUUID)) {
                                    player.sendMessage(ChatColor.RED + "You do not have an invite to join " + teamName + ".");
                                    return true;
                                }

                                // Remove from old team (if in one)
                                for (Team t : league.getTeams()) {
                                    if (t.isMember(playerUUID)) {
                                        t.removeMember(playerUUID);
                                        break;
                                    }
                                }

                                // Add to new team
                                boolean addedMainDriver = league.addDriverToTeam(playerUUID, team.getName(), 1);
                                if (addedMainDriver) {
                                    player.sendMessage(ChatColor.GREEN + "You joined team " + team.getName() + "!");
                                    String owner = team.getOwner();
                                    Player ownerPlayer = Bukkit.getPlayer(UUID.fromString(owner));
                                    if (ownerPlayer != null && ownerPlayer.isOnline()) {
                                        ownerPlayer.sendMessage(ChatColor.YELLOW + player.getName() + " has joined the team!");
                                    }

                                } else {
                                    boolean addedReserveDriver = team.addDriver(playerUUID, 99);
                                    if (addedReserveDriver){
                                        player.sendMessage(ChatColor.GREEN + "You joined team " + team.getName() + "!");
                                        String owner = team.getOwner();
                                        Player ownerPlayer = Bukkit.getPlayer(UUID.fromString(owner));
                                        if (ownerPlayer != null && ownerPlayer.isOnline()) {
                                            ownerPlayer.sendMessage(ChatColor.YELLOW + player.getName() + " has joined the team!");
                                        }
                                    } else {
                                        player.sendMessage(ChatColor.RED + "Could not join team (team full?).");
                                        return true;
                                    }

                                }

                                // Clear invite
                                pendingInvites.getOrDefault(leagueName, Collections.emptyMap())
                                        .getOrDefault(teamName, Collections.emptySet())
                                        .remove(playerUUID);
                            }

                            case "decline" -> {
                                if (args.length < 4) {
                                    player.sendMessage("Usage: /league <leagueName> team decline <teamName>");
                                    return true;
                                }

                                String teamName = args[3];
                                Team team = league.getTeam(teamName);
                                if (team == null) {
                                    player.sendMessage("Team not found: " + teamName);
                                    return true;
                                }

                                String playerUUID = player.getUniqueId().toString();

                                // Check if the player has an invite
                                Set<String> invites = pendingInvites
                                        .getOrDefault(leagueName, Collections.emptyMap())
                                        .getOrDefault(teamName, Collections.emptySet());

                                if (!invites.contains(playerUUID)) {
                                    player.sendMessage(ChatColor.RED + "You don’t have an invite to " + teamName + ".");
                                    return true;
                                }

                                // Remove invite
                                invites.remove(playerUUID);
                                player.sendMessage(ChatColor.YELLOW + "You declined the invite to join " + teamName + ".");

                                // Notify team owner if online
                                String owner = team.getOwner();
                                Player ownerPlayer = Bukkit.getPlayer(UUID.fromString(owner));
                                if (ownerPlayer != null && ownerPlayer.isOnline()) {
                                    ownerPlayer.sendMessage(ChatColor.RED + player.getName() + " declined the invite to join your team " + teamName + ".");
                                }
                            }

                            case "leave" ->{
                                if (args.length < 4) {
                                    player.sendMessage("Usage: /league <leagueName> team leave");
                                    return true;
                                }

                                Team team = league.getTeamByDriver(sender.getName());
                                team.removeMember(((Player) sender).getName());

                                player.sendMessage("you have left the team " + team.getName());
                            }

                            case "promote" -> {
                                if (args.length < 5){
                                    player.sendMessage(ChatColor.RED + "Usage: /league <league> team  promote <team> <player>");
                                    return true;
                                }

                                String teamName = args[3];
                                String driverName = args[4];
                                @NotNull OfflinePlayer driver = Bukkit.getOfflinePlayer(driverName);
                                Team team = league.getTeam(teamName);

                                String owner = team.getOwner();
                                if (owner == null || !owner.equals(player.getUniqueId().toString())) {
                                    player.sendMessage(ChatColor.RED + "You do not have permission over this team.");
                                    return true;
                                }

                                boolean promoted = team.promoteToMain(driver.getUniqueId().toString());
                                if (promoted) player.sendMessage(ChatColor.GREEN + driverName + " promoted to main driver.");
                                else player.sendMessage(ChatColor.RED + "Could not promote " + driverName + " (already main or team full?).");
                            }

                            case "demote" -> {
                                if (args.length < 5){
                                    player.sendMessage(ChatColor.RED + "Usage: /league <league> team  demote <team> <player>");
                                    return true;
                                }

                                String teamName = args[3];
                                String driverName = args[4];
                                @NotNull OfflinePlayer driver = Bukkit.getOfflinePlayer(driverName);
                                Team team = league.getTeam(teamName);

                                String owner = team.getOwner();
                                if (owner == null || !owner.equals(player.getUniqueId().toString())) {
                                    player.sendMessage(ChatColor.RED + "You do not have permission over this team.");
                                    return true;
                                }

                                boolean demoted = team.demoteToReserve(driver.getUniqueId().toString());
                                if (demoted) player.sendMessage(ChatColor.GREEN + driverName + " demoted to reserve driver.");
                                else player.sendMessage(ChatColor.RED + "Could not demote " + driverName + " (already reserve or team full?).");
                            }

                            // ===== View Team =====
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

                                sendTeamDetails(player, team, league);
                            }

                            // ===== Default: shorthand view =====
                            default -> {
                                Team team = league.getTeam(args[2]);
                                if (team == null) {
                                    player.sendMessage("Team not found: " + args[2]);
                                    return true;
                                }

                                sendTeamDetails(player, team, league);
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
        boolean isAdmin = player.hasPermission("timingleague.admin");

        List<String> lines = new ArrayList<>();

        lines.add("§6=== /league Command Help ===");

        // ===== General =====
        lines.add("§e/league help §7- Show this help menu");

        if (isAdmin) {
            lines.add("§e/league create <leagueName> §7- Create a league");
            lines.add("§e/league delete <leagueName> §7- Delete a league");
        }

        // ===== League basics =====
        lines.add("§6--- League ---");
        lines.add("§e/league <league> addDriver <player> §7- Manually add a driver");
        lines.add("§e/league <league> calendar §7- View event calendar");
        lines.add("§e/league <league> standings [page] §7- Driver standings");
        lines.add("§e/league <league> standings teams [page] §7- Team standings");

        // ===== Events / scoring (admin) =====
        if (isAdmin) {
            lines.add("§6--- Events & Scoring ---");
            lines.add("§e/league <league> addEvent <eventId> §7- Add event");
            lines.add("§e/league <league> update §7- Recalculate standings");
            lines.add("§e/league <league> updateWithHeat <eventId> <heatId> §7- Update from heat");
            lines.add("§e/league <league> scoring <fc1|fc2|default> §7- Set scoring system");
            lines.add("§e/league <league> predictedDrivers [number] §7- Set/view predicted drivers");
            lines.add("§e/league <league> points <player> <+/-num> §7- Adjust driver points");
            lines.add("§e/league <league> teamPoints <team> <+/-num> §7- Adjust team points");
        }

        // ===== Teams =====
        lines.add("§6--- Teams ---");
        lines.add("§e/league <league> team list §7- List teams");
        lines.add("§e/league <league> team view <team> §7- View team");
        lines.add("§e/league <league> team <team> §7- Shorthand view");
        lines.add("§e/league <league> team accept <team> §7- Accept invite");
        lines.add("§e/league <league> team decline <team> §7- Decline invite");

        if (isAdmin) {
            lines.add("§e/league <league> team create <team> [hex] §7- Create team");
            lines.add("§e/league <league> team color <team> <hex> §7- Set team color");
            lines.add("§e/league <league> team add <team> <main|reserve> <player>");
        }

        lines.add("§e/league <league> team remove <team> <player> §7- Remove member");
        lines.add("§e/league <league> team invite <team> <player> §7- Invite driver");

        // ===== MAIN_RESERVE Mode Commands =====
        lines.add("§6--- MAIN_RESERVE Mode ---");
        lines.add("§e/league <league> team promote <team> <player> §7- Promote reserve");
        lines.add("§e/league <league> team demote <team> <player> §7- Demote main");

        // ===== PRIORITY/HIGHEST Mode Commands =====
        lines.add("§6--- PRIORITY/HIGHEST Mode ---");
        lines.add("§e/league <league> team setpriority <team> <player> <pos>");
        lines.add("§e/league <league> team removepriority <team> <player>");
        lines.add("§e/league <league> team swap <team> <player1> <player2>");

        // ===== Holograms =====
        if (isAdmin) {
            lines.add("§6--- Holograms ---");
            lines.add("§e/league <league> holo <driver|team> [page]");
            lines.add("§e/league <league> holo deleteClosest");
            lines.add("§e/league <league> holo update");
        }

        // Send
        player.sendMessage(lines.toArray(new String[0]));
    }


    private void sendTeamDetails(Player player, Team team, League league) {
        TeamMode teamMode = league.getTeamMode();

        if (teamMode == TeamMode.MAIN_RESERVE){
            StringBuilder sb = new StringBuilder();
            sb.append("=== ").append(team.getName()).append(" ===\n");
            sb.append("Color: #").append(team.getColor()).append("\n");

            sb.append("Main Drivers:\n");
            for (String uuid : team.getMainDrivers()) {
                OfflinePlayer member = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
                sb.append("- ").append(member.getName()).append("\n");
            }

            sb.append("Reserve Drivers:\n");
            for (String uuid : team.getReserveDrivers()) {
                OfflinePlayer member = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
                sb.append("- ").append(member.getName()).append("\n");
            }

            if (team.getOwner() != null) {
                OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(team.getOwner()));
                sb.append("Owner: ").append(owner.getName()).append("\n");
            }

            player.sendMessage(sb.toString());
        }
        else{
            List<String> priorityDrivers = team.getPriorityDrivers();
            int scoringCount = team.getCountedPrioDrivers();

            player.sendMessage("§6=== " + team.getName() + " Roster ===");
            player.sendMessage("§7Mode: " + teamMode + " (Top " + scoringCount + " score)");

            if (priorityDrivers.isEmpty()) {
                player.sendMessage("§cNo drivers on this team.");
            }

            for (int i = 0; i < priorityDrivers.size(); i++) {
                String uuid = priorityDrivers.get(i);
                OfflinePlayer driver = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
                String name = driver.getName() != null ? driver.getName() : "Unknown";

                String prefix = (i < scoringCount) ? "§a" : "§7";
                String scoring = (i < scoringCount) ? " §e[SCORING]" : "";

                player.sendMessage(prefix + (i + 1) + ". " + name + scoring);
            }
        }
    }

    private boolean createOrUpdateHolograms(League league, int page, Player player, String argument) {
        boolean teamMode = argument.equalsIgnoreCase("team");
        boolean isUpdate = argument.equalsIgnoreCase("update");
        String leagueName = league.getName();
        int pageSize = 15;
        int start = (page - 1) * pageSize;
        Location placeLocation = player.getLocation().clone().add(0, 5, 0);

        LeagueHologramManager manager = new LeagueHologramManager();

        if (isUpdate) {
            String prefix = "league-holo-" + leagueName;

            // Collect matching holograms
            List<String> updateWithDriver = new ArrayList<>();
            List<String> updateWithTeam = new ArrayList<>();

            for (String hologram : holograms) {
                if (hologram.startsWith(prefix + "-false")) {
                    updateWithDriver.add(hologram);
                }
                if (hologram.startsWith(prefix + "-true")) {
                    updateWithTeam.add(hologram);
                }
            }

            // Generate lines ONCE for each type
            List<String> driverLines = generateDriverLines(league, leagueName, start, pageSize, player);
            List<String> teamLines = generateTeamLines(league, leagueName, start, pageSize, player);

            for (String hologram : updateWithDriver) {
                manager.updateExistingHologram(hologram, driverLines);
            }

            for (String hologram : updateWithTeam) {
                manager.updateExistingHologram(hologram, teamLines);
            }

        } else {
            List<String> lines;
            if (teamMode) {
                lines = generateTeamLines(league, leagueName, start, pageSize, player);
            } else {
                lines = generateDriverLines(league, leagueName, start, pageSize, player);
            }

            manager.createOrUpdateHologram(placeLocation, lines, leagueName, teamMode);
            player.sendMessage("Leaderboard hologram placed: " + manager.getHologramName());
            holograms.add(manager.getHologramName());
        }

        return true;
    }

    public boolean updateHolograms(League league, int page, @Nullable Player player) {
        String leagueName = league.getName();
        int pageSize = 15;
        int start = (page - 1) * pageSize;

        LeagueHologramManager manager = new LeagueHologramManager();

        String prefix = "league-holo-" + leagueName;

        // Collect matching holograms
        List<String> updateWithDriver = new ArrayList<>();
        List<String> updateWithTeam = new ArrayList<>();

        for (String hologram : holograms) {
            if (hologram.startsWith(prefix + "-false")) {
                updateWithDriver.add(hologram);
            }
            if (hologram.startsWith(prefix + "-true")) {
                updateWithTeam.add(hologram);
            }
        }

        // Generate lines ONCE for each type
        List<String> driverLines = generateDriverLines(league, leagueName, start, pageSize, player);
        List<String> teamLines = generateTeamLines(league, leagueName, start, pageSize, player);

        for (String hologram : updateWithDriver) {
            manager.updateExistingHologram(hologram, driverLines);
        }

        for (String hologram : updateWithTeam) {
            manager.updateExistingHologram(hologram, teamLines);
        }

        return true;
    }


    private List<String> generateDriverLines(League league, String leagueName, int start, int pageSize, @Nullable Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&c" + leagueName + " Driver Leaderboard");

        var standings = league.getDriverStandings().entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .toList();

        if (start >= standings.size()) {
            if (player != null) player.sendMessage("Page out of range.");
        }

        int end = Math.min(start + pageSize, standings.size());
        for (int i = start; i < end; i++) {
            var entry = standings.get(i);
            String name = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey())).getName();
            lines.add("&e#" + (i + 1) + ". &b" + (name != null ? name : "Unknown") + " &7- &a" + entry.getValue() + " pts");
        }

        while (lines.size() < pageSize + 1) {
            lines.add("&e#" + lines.size() + ". &7---");
        }

        return lines;
    }

    private List<String> generateTeamLines(League league, String leagueName, int start, int pageSize, @Nullable Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&c" + leagueName + " Team Leaderboard");

        var standings = league.getTeamStandings().entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .toList();

        if (start >= standings.size()) {
            if (player != null) player.sendMessage("Page out of range.");
        }

        int end = Math.min(start + pageSize, standings.size());
        for (int i = start; i < end; i++) {
            var entry = standings.get(i);
            lines.add("&e#" + (i + 1) + ". &b" + entry.getKey() + " &7- &a" + entry.getValue() + " pts");
        }

        while (lines.size() < pageSize + 1) {
            lines.add("&e#" + lines.size() + ". &7---");
        }

        return lines;
    }

    private String getColoredSquare(String colorInput) {
        // Normalize input
        if (!colorInput.startsWith("#")) {
            colorInput = "#" + colorInput;
        }

        // Validate format: must be exactly 7 characters and match hex pattern
        if (!colorInput.matches("^#[0-9a-fA-F]{6}$")) {
            return "§7■§r"; // fallback gray square
        }

        // Convert to §x§r§r§g§g§b§b format
        StringBuilder colored = new StringBuilder("§x");
        for (char c : colorInput.substring(1).toCharArray()) {
            colored.append("§").append(c);
        }

        return colored + "■§r";
    }
}
