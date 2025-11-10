package me.makkuusen.timing.system.permissions;

import co.aikar.commands.CommandReplacements;

/**
 * Team management permissions following existing permission patterns
 */
public enum PermissionTeam implements Permissions {
    CREATE,
    DELETE,
    MANAGE,
    INFO,
    LIST;

    @Override
    public String getNode() {
        return "timingsystem.team." + this.toString().replace("_", ".").toLowerCase();
    }

    public static void init(CommandReplacements replacements) {
        for(PermissionTeam perm : values()) {
            Permissions.register(perm, replacements);
        }
    }
}