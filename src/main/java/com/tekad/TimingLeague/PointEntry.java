package com.tekad.TimingLeague;

import lombok.Getter;

@Getter
public class PointEntry {
    private final String source;      // "heat:eventId:heatId" or "manual:reason"
    private final String eventId;     // Which event this belongs to (null for manual)
    private final int points;
    private final long timestamp;

    public PointEntry(String source, String eventId, int points, long timestamp) {
        this.source = source;
        this.eventId = eventId;
        this.points = points;
        this.timestamp = timestamp;
    }
}
