package com.tekad.TimingLeague.commands.subcommands;

import com.tekad.TimingLeague.League;
import com.tekad.TimingLeague.Team;
import com.tekad.TimingLeague.TeamMode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class LeagueTeamCommands {

    private final Map<String, Map<String, Set<String>>> pendingInvites;

    public LeagueTeamCommands(Map<String, Map<String, Set<String>>> pendingInvites) {
        this.pendingInvites = pendingInvites;
    }

    public boolean handleTeamCommand(Player player, League league, String leagueName, String[] args) {
        if (args.length < 3) {
            player.sendMessage("Usage: /league <leagueName> team <create|color|add|remove|view|list> [arguments]");
            return true;
        }

        String teamAction = args[2].toLowerCase();

        // List teams
        if (teamAction.equals("list")) {
            return handleList(player, league);
        }

        switch (teamAction) {
            case "create" -> { return handleCreate(player, league, args); }
            case "color" -> { return handleColor(player, league, args); }
            case "add" -> { return handleAdd(player, league, args); }
            case "remove" -> { return handleRemove(player, league, args); }
            case "roster", "view" -> { return handleRoster(player, league, args); }
            case "setpriority" -> { return handleSetPriority(player, league, args); }
            case "removepriority" -> { return handleRemovePriority(player, league, args); }
            case "invite" -> { return handleInvite(player, league, leagueName, args); }
            case "accept" -> { return handleAccept(player, league, leagueName, args); }
            case "decline" -> { return handleDecline(player, league, leagueName, args); }
            case "leave" -> { return handleLeave(player, league, args); }
            case "promote" -> { return handlePromote(player, league, args); }
            case "demote" -> { return handleDemote(player, league, args); }
            default -> {
                // Shorthand view
                Team team = league.getTeam(args[2]);
                if (team == null) {
                    player.sendMessage("Team not found: " + args[2]);
                    return true;
                }
                sendTeamDetails(player, team, league);
                return true;
            }
        }
    }

    private boolean handleList(Player player, League league) {
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

    private boolean handleCreate(Player player, League league, String[] args) {
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
        team.setOwner(player.getUniqueId().toString());
        league.addTeam(team);
        player.sendMessage("Created team: " + teamName + " with color #" + color);
        return true;
    }

    private boolean handleColor(Player player, League league, String[] args) {
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
        return true;
    }

    private boolean handleAdd(Player player, League league, String[] args) {
        if (args.length < 6) {
            player.sendMessage("Usage: /league <leagueName> team add <teamName> <main|reserve> <playerName>");
            return true;
        }

        if (!player.hasPermission("timingleague.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
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
        }

        if (success) {
            player.sendMessage("Added " + target.getName() + " to team " + team.getName() + " as " + type + ".");
        } else {
            player.sendMessage("Could not add player (team full or already in team).");
        }
        return true;
    }

    private boolean handleRemove(Player player, League league, String[] args) {
        if (args.length < 5) {
            player.sendMessage("Usage: /league <leagueName> team remove <teamName> <playerName>");
            return true;
        }

        Team team = league.getTeam(args[3]);
        if (!((!player.hasPermission("timingleague.admin")) | (!team.getOwner().equals(player.getUniqueId().toString())))) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
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
        return true;
    }

    private boolean handleRoster(Player player, League league, String[] args) {
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
        return true;
    }

    private boolean handleSetPriority(Player player, League league, String[] args) {
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
        if (!player.hasPermission("timingleague.admin") &&
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
        return true;
    }

    private boolean handleRemovePriority(Player player, League league, String[] args) {
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
        if (!player.hasPermission("timingleague.admin") &&
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
        return true;
    }

    private boolean handleInvite(Player player, League league, String leagueName, String[] args) {
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
            target.getPlayer().sendMessage(ChatColor.GREEN + "You've been invited to join team " +
                    teamName + " in league " + leagueName +
                    ". Use /league " + leagueName + " team " + " accept " + teamName);
        }

        player.sendMessage("Invite sent to " + driverName + " for team " + teamName + ".");
        return true;
    }

    private boolean handleAccept(Player player, League league, String leagueName, String[] args) {
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
            if (addedReserveDriver) {
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
        return true;
    }

    private boolean handleDecline(Player player, League league, String leagueName, String[] args) {
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
            player.sendMessage(ChatColor.RED + "You don't have an invite to " + teamName + ".");
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
        return true;
    }

    private boolean handleLeave(Player player, League league, String[] args) {
        if (args.length < 4) {
            player.sendMessage("Usage: /league <leagueName> team leave");
            return true;
        }

        Team team = league.getTeamByDriver(player.getName());
        team.removeMember(player.getName());

        player.sendMessage("You have left the team " + team.getName());
        return true;
    }

    private boolean handlePromote(Player player, League league, String[] args) {
        if (args.length < 5) {
            player.sendMessage(ChatColor.RED + "Usage: /league <league> team promote <team> <player>");
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
        if (promoted) {
            player.sendMessage(ChatColor.GREEN + driverName + " promoted to main driver.");
        } else {
            player.sendMessage(ChatColor.RED + "Could not promote " + driverName + " (already main or team full?).");
        }
        return true;
    }

    private boolean handleDemote(Player player, League league, String[] args) {
        if (args.length < 5) {
            player.sendMessage(ChatColor.RED + "Usage: /league <league> team demote <team> <player>");
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
        if (demoted) {
            player.sendMessage(ChatColor.GREEN + driverName + " demoted to reserve driver.");
        } else {
            player.sendMessage(ChatColor.RED + "Could not demote " + driverName + " (already reserve or team full?).");
        }
        return true;
    }

    public static void sendTeamDetails(Player player, Team team, League league) {
        TeamMode teamMode = league.getTeamMode();

        if (teamMode == TeamMode.MAIN_RESERVE) {
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
        } else {
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
