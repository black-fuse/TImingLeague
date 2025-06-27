package com.tekad.TimingLeague.ScoringSystems;

import java.util.List;

public class CustomScoringSystem implements ScoringSystem {
    private final List<Integer> points;

    public CustomScoringSystem(List<Integer> points) {
        this.points = points;
    }

    @Override
    public List<Integer> getPointsDistribution(int driverCount) {
        return points;
    }

    @Override
    public int getPointsForPosition(int position, int driverCount) {
        if (position - 1 < points.size()) {
            return points.get(position - 1);
        }
        return 0;
    }
}