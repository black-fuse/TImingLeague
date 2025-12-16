package me.makkuusen.timing.system.heat;

public enum CollisionMode {
    HIGH,     // Default for final heats - same as current non-loneliness heats
    LOW,      // Intermediate collision mode
    DISABLED  // Default for qualifying heats - same as current loneliness heats
}