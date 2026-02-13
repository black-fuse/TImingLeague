package com.tekad.TimingLeague.commands.subcommands;

import com.tekad.TimingLeague.League;
import com.tekad.TimingLeague.LeagueHologramManager;
import com.tekad.TimingLeague.TImingLeague;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class LeagueHologramCommands {

    private final List<String> holograms = TImingLeague.getHolograms();

    public boolean handleHolo(Player player, League league, String[] args) {
        if (!player.hasPermission("timingleague.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
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

        if (args[2].equalsIgnoreCase("deleteClosest")) {
            Location playerLocation = player.getLocation();
            double closestDistance = Double.MAX_VALUE;
            Hologram closest = null;

            for (String holoName : holograms) {
                Hologram holo = DHAPI.getHologram(holoName);
                if (!holo.getLocation().getWorld().equals(playerLocation.getWorld())) continue;

                double distance = holo.getLocation().distanceSquared(playerLocation);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = holo;
                }
            }

            if (closest != null) {
                closest.delete();
                player.sendMessage("Successfully deleted " + closest.getName());
                holograms.remove(closest.getName());
            } else {
                player.sendMessage("There is no hologram to delete");
            }
            return true;
        }

        return createOrUpdateHolograms(league, page, player, args[2]);
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

    public void updateHolograms(League league, int page, @Nullable Player player) {
        String leagueName = league.getName();
        int pageSize = 15;
        int start = (page - 1) * pageSize;

        LeagueHologramManager manager = new LeagueHologramManager();

        String prefix = "league-holo-" + leagueName;

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

        List<String> driverLines = generateDriverLines(league, leagueName, start, pageSize, player);
        List<String> teamLines = generateTeamLines(league, leagueName, start, pageSize, player);

        for (String hologram : updateWithDriver) {
            manager.updateExistingHologram(hologram, driverLines);
        }

        for (String hologram : updateWithTeam) {
            manager.updateExistingHologram(hologram, teamLines);
        }
    }

    private List<String> generateDriverLines(League league, String leagueName, int start, int pageSize, @Nullable Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&c" + leagueName + " Driver Leaderboard");

        var standings = league.getDriverStandings().entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .toList();

        if (start >= standings.size() && player != null) {
            player.sendMessage("Page out of range.");
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

        if (start >= standings.size() && player != null) {
            player.sendMessage("Page out of range.");
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
}
