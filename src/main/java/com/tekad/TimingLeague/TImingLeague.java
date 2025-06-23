package com.tekad.TimingLeague;

import com.tekad.TimingLeague.commands.LeagueCommandCompleter;
import com.tekad.TimingLeague.commands.leagueCommand;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import com.tekad.TimingLeague.commands.leagueCommand;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class TImingLeague extends JavaPlugin {

    private DatabaseManager db;

    @Getter
    private static final Map<String, League> leagueMap = new HashMap<>();

    @Override
    public void onEnable() {
        // Ensure data folder exists before database file creation
        getDataFolder().mkdirs();

        // Database startup
        db = new DatabaseManager(this);
        try {
            db.connect();
            db.createTables();
            leagueMap.putAll(db.loadAllLeagues());
        } catch (SQLException e) {
            getLogger().severe("[TimingLeague] Failed to connect to the database: " + e);
        }

        // Command registration
        if (getCommand("league") != null) {
            getCommand("league").setExecutor(new leagueCommand());
            getCommand("league").setTabCompleter(new LeagueCommandCompleter());
        } else {
            getLogger().severe("Command 'league' not found in plugin.yml!");
        }
    }


    @Override
    public void onDisable() {
        try {
            db.saveAllLeagues(leagueMap.values());
        } catch (SQLException e) {
            getLogger().severe("Failed to save leagues: " + e.getMessage());
        }
    }

    public DatabaseManager getDatabaseManager() {
        return db;
    }

    public void addLeagueToMap(League league) {
        leagueMap.put(league.getName(), league);
    }
}
