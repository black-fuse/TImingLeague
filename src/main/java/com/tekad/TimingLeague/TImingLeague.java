package com.tekad.TimingLeague;

import org.bukkit.plugin.java.JavaPlugin;
import com.tekad.TimingLeague.commands.leagueCommand;

public final class TImingLeague extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getCommand("leagueCommand").setExecutor(new leagueCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
