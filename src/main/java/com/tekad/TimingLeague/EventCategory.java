package com.tekad.TimingLeague;

import com.tekad.TimingLeague.ScoringSystems.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;


public class EventCategory {

    @Getter
    private final String id;           // internal key, e.g. "standard", "multiplier"

    @Getter @Setter
    private String displayName;        // shown in UI, e.g. "Standard Races"

    @Getter @Setter
    private ScoringSystem scoringSystem;

    @Getter @Setter
    private int mulliganCount;         // how many worst events to drop for this category

    // ── Constructor ──────────────────────────────────────────────────────────

    public EventCategory(String id, String displayName, ScoringSystem scoringSystem, int mulliganCount) {
        this.id = id;
        this.displayName = displayName;
        this.scoringSystem = scoringSystem;
        this.mulliganCount = mulliganCount;
    }


    public EventCategory(String id) {
        this(id, id, new BasicScoringSystem(), 0);
    }

    // ── Scoring system name helpers (for DB persistence) ─────────────────────

    public String getScoringSystemName() {
        return scoringSystemNameOf(scoringSystem);
    }


    public static ScoringSystem scoringSystemFromName(String name) {
        if (name == null) return new BasicScoringSystem();
        return switch (name.toLowerCase()) {
            case "fc1"        -> new FC1ScoringSystem();
            case "fc2"        -> new FC2ScoringSystem();
            case "wibrs"      -> new WIBRSScoringSystem();
            case "iec"        -> new IECScoringSystem();
            case "iecdouble"  -> new IECDouble();
            case "iecopener"  -> new IECOpenerSystem();
            case "linear"     -> new LinearScoringSystem(50,1);
            default           -> new BasicScoringSystem();
        };
    }

    public static String scoringSystemNameOf(ScoringSystem system) {
        if (system == null) return "basic";
        // CustomScoringSystem can't be round-tripped by name alone — callers
        // that need to persist custom scales handle that separately.
        return system.getName().toLowerCase().replace("scoringsystem", "");
    }
}
