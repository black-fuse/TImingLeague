package com.tekad.TimingLeague.commands;

import com.tekad.TimingLeague.League;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class leagueCommand implements CommandExecutor {
    private Map<String, League> leagues = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        Player player =(Player) commandSender;

        if (args.length >= 3) {
            if (args[1].equalsIgnoreCase("create")){
                League thing = new League(args[2], 20);
                player.sendMessage("Created League: " + args[2]);
                leagues.put(args[2], thing);
            }

            for (Map.Entry<String, League> entry : leagues.entrySet()){
                String name = entry.getKey();
                League selectedLeague = entry.getValue();

                if (args[1].equalsIgnoreCase(name)){
                    if (args[3].equalsIgnoreCase("addDriver")){
                        String driverUsername = "";

                        try{
                            driverUsername = args[4];
                        } catch (Exception e){
                            player.sendMessage("you need to specify a drivers username");
                        }

                        OfflinePlayer target = Bukkit.getOfflinePlayer(driverUsername);
                        UUID uuid = target.getUniqueId();
                        String uuidString = uuid.toString();

                        selectedLeague.addDriver(uuidString, selectedLeague.NoTeam);
                    }
                    if (args[2].equalsIgnoreCase("updateStandings")){
                        selectedLeague.updateStandingsFromEvents();
                    }
                    if (args[2].equalsIgnoreCase("addEvent")){
                        // TODO: check if event exists before letting player enter it
                        selectedLeague.addEvent(args[3]);
                    }
                }
            }
        }
        else {
            player.sendMessage("Usage: /league create <leagueName>");
        }

        return false;
    }
}
