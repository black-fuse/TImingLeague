package com.tekad.TimingLeague;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;

import java.util.List;
import java.util.UUID;

public class LeagueHologramManager {

    private Hologram hologram;

    public void createOrUpdateHologram(Location location, List<String> lines) {
        removeHologram();

        Location offset = location.clone();
        offset.setY(offset.getY() + 0.5);
        String name = "league-holo-" + UUID.randomUUID();

        hologram = DHAPI.createHologram(name, offset, false, lines);
    }

    public void removeHologram() {
        if (hologram != null) {
            hologram.delete();
            hologram = null;
        }
    }
}
