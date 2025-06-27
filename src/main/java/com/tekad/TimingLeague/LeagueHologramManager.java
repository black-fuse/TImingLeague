package com.tekad.TimingLeague;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;

import java.util.List;
import java.util.UUID;

public class LeagueHologramManager {
    private Hologram hologram;
    private String hologramName;

    public void createOrUpdateHologram(Location location, List<String> lines, String leagueName, boolean teamMode) {
        removeHologram();

        Location offset = location.clone();
        offset.setY(offset.getY() + 0.5);
        this.hologramName = "league-holo-" + leagueName + "-" + teamMode + "-" + UUID.randomUUID();


        hologram = DHAPI.createHologram(hologramName, offset, false, lines);
    }

    public void removeHologram() {
        if (hologram != null) {
            hologram.delete();
            hologram = null;
        }
    }

    public String getHologramName(){
        return hologramName;
    }

    public boolean updateExistingHologram(String name, List<String> lines){
        Hologram existing = DHAPI.getHologram(name);
        if (existing != null){
            DHAPI.setHologramLines(existing, lines);
            return true;
        }
        return false;
    }
}
