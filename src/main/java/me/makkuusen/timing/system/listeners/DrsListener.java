package me.makkuusen.timing.system.listeners;

import me.makkuusen.timing.system.PlayerRegionData;
import me.makkuusen.timing.system.database.EventDatabase;
import me.makkuusen.timing.system.drs.DrsManager;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.regions.TrackRegion;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

public class DrsListener implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() &&
            e.getFrom().getBlockY() == e.getTo().getBlockY() && 
            e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
            return;
        }

        Player player = e.getPlayer();

        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        Optional<Driver> maybeDriver = EventDatabase.getDriverFromRunningHeat(player.getUniqueId());
        if (maybeDriver.isPresent()) {
            if (maybeDriver.get().getHeat().getDrs() != null && maybeDriver.get().getHeat().getDrs()) {
                checkDrsRegions(player, maybeDriver.get().getHeat().getEvent().getTrack());
            }
            return;
        }
    }

    private void checkDrsRegions(Player player, Track track) {
        for (TrackRegion region : track.getTrackRegions().getRegions(TrackRegion.RegionType.DRSDETECT)) {
            Integer regionId = region.getId();
            boolean isInRegion = region.contains(player.getLocation());
            boolean wasInRegion = PlayerRegionData.instanceOf(player).getEntered().contains(regionId);

            if (isInRegion && !wasInRegion) {
                PlayerRegionData.instanceOf(player).getEntered().add(regionId);
                DrsManager.playerPassedDrsDetect(player, regionId);
            } else if (!isInRegion && wasInRegion) {
                PlayerRegionData.instanceOf(player).getEntered().remove(regionId);
            }
        }

        for (TrackRegion region : track.getTrackRegions().getRegions(TrackRegion.RegionType.DRSACTIVATE)) {
            Integer regionId = region.getId();
            boolean isInRegion = region.contains(player.getLocation());
            boolean wasInRegion = PlayerRegionData.instanceOf(player).getEntered().contains(regionId);

            if (isInRegion && !wasInRegion) {
                PlayerRegionData.instanceOf(player).getEntered().add(regionId);
                DrsManager.playerPassedDrsActivate(player, regionId);
            } else if (!isInRegion && wasInRegion) {
                PlayerRegionData.instanceOf(player).getEntered().remove(regionId);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        DrsManager.cleanupPlayer(event.getPlayer().getUniqueId());
    }
}
