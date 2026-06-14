package com.tekad.TimingLeague.API.dto;

import com.tekad.TimingLeague.CalendarEntry;

public class CalendarEntryDTO {
    public String eventName;

    public String categoryId;

    public String heatId;

    public CalendarEntryDTO(CalendarEntry calendarEntry){
        this.eventName = calendarEntry.getEventName();
        this.categoryId = calendarEntry.getCategoryId();
        this.heatId = calendarEntry.getHeatId();
    }
}
