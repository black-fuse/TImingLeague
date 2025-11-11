package me.makkuusen.timing.system.api.events.driver;

import lombok.Getter;
import me.makkuusen.timing.system.heat.TeamHeatEntry;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
public class DriverSwapEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final TeamHeatEntry teamHeatEntry;
    private final UUID oldDriverUUID;
    private final UUID newDriverUUID;
    private final SwapType swapType;

    public DriverSwapEvent(TeamHeatEntry teamHeatEntry, UUID oldDriverUUID, UUID newDriverUUID, SwapType swapType) {
        this.teamHeatEntry = teamHeatEntry;
        this.oldDriverUUID = oldDriverUUID;
        this.newDriverUUID = newDriverUUID;
        this.swapType = swapType;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public enum SwapType {
        RIGHT_CLICK,
        OFFLINE_REPLACEMENT
    }
}
