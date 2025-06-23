package com.tekad.TimingLeague;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import com.tekad.TimingLeague.commands.leagueCommand;

import java.sql.SQLException;
import java.util.Map;

public final class TImingLeague extends JavaPlugin {
    private DatabaseManager db;
    @Getter
    private static Map<String, League> leagueMap;

    @Override
    public void onEnable() {
        //database startup
        db = new DatabaseManager(this);
        try{
            db.connect();
            db.createTables();
            leagueMap = db.loadAllLeagues();
        }catch (SQLException e){
            getLogger().severe("[TimingLeague] failed to connect to the database: " + e);
        }

        // commands initialisation
        getCommand("league").setExecutor(new leagueCommand());
    }

    @Override
    public void onDisable() {
        // saving db's
        try {
            db.saveAllLeagues(leagueMap.values());
        } catch (SQLException e) {
            getLogger().severe("Failed to save leagues: " + e.getMessage());
        }
    }

    public DatabaseManager getDatabaseManager() {
        return db;
    }

    public void addLeagueToMap(League league){
        String name = league.getName();
        leagueMap.put(name, league);
    }
}
