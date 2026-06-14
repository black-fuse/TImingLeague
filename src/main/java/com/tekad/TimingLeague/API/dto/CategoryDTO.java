package com.tekad.TimingLeague.API.dto;

import com.tekad.TimingLeague.EventCategory;

import java.util.Locale;

public class CategoryDTO {
    public String id;

    public String displayName;

    public String scoringSystem;

    public int mulliganCount;

    public CategoryDTO(EventCategory category){
        this.id = category.getId();
        this.displayName = category.getDisplayName();
        this.scoringSystem = category.getScoringSystemName();
        this.mulliganCount = category.getMulliganCount();
    }
}
