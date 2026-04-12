package com.tekad.TimingLeague;

import lombok.Getter;

/**
 * An entry in a league's calendar.
 *
 * Stores an event name + optional specific heatId to process.
 * If heatId is null, the updater falls back to scanning all final heats
 * in the event (backwards-compatible behaviour).
 *
 * categoryId == null  →  uncategorised; uses league default scoring / no mulligan pool.
 */
public class CalendarEntry {

    @Getter
    private final String eventName;

    @Getter
    private final String categoryId;   // nullable

    /**
     * The specific heat to score, e.g. "r1f1".
     * null = scan all final heats in the event (old behaviour).
     */
    @Getter
    private final String heatId;       // nullable

    // ── Full constructor ──────────────────────────────────────────────────────

    public CalendarEntry(String eventName, String categoryId, String heatId) {
        this.eventName  = eventName;
        this.categoryId = categoryId;
        this.heatId     = heatId;
    }

    // ── Convenience constructors ──────────────────────────────────────────────

    /** Event + category, no specific heat pinned. */
    public CalendarEntry(String eventName, String categoryId) {
        this(eventName, categoryId, null);
    }

    /** Uncategorised, no specific heat. */
    public CalendarEntry(String eventName) {
        this(eventName, null, null);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean hasCategory() {
        return categoryId != null && !categoryId.isEmpty();
    }

    public boolean hasPinnedHeat() {
        return heatId != null && !heatId.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CalendarEntry other)) return false;
        // Two entries for the same event are the same calendar slot.
        return eventName.equals(other.eventName);
    }

    @Override
    public int hashCode() {
        return eventName.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(eventName);
        if (hasPinnedHeat())  sb.append(" [heat:").append(heatId).append("]");
        if (hasCategory())    sb.append(" [").append(categoryId).append("]");
        return sb.toString();
    }
}
