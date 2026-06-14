package com.tekad.TimingLeague.API.dto;

import com.tekad.TimingLeague.PointEntry;

public class PointHistoryDTO {
    public String source;

    public String eventId;

    public int points;

    public long timestamp;

    public PointHistoryDTO(PointEntry entry){
        this.source = entry.getSource();
        this.eventId = entry.getEventId();
        this.points = entry.getPoints();
    }
}

