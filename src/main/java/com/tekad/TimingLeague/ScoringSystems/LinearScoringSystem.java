package com.tekad.TimingLeague.ScoringSystems;

import java.util.ArrayList;
import java.util.List;

public class LinearScoringSystem implements ScoringSystem {

    private final int maxPoints;
    private final int minPoints;

    public LinearScoringSystem(int maxPoints, int minPoints) {
        if (maxPoints < minPoints) {
            throw new IllegalArgumentException("maxPoints must be >= minPoints");
        }
        this.maxPoints = maxPoints;
        this.minPoints = minPoints;
    }

    @Override
    public List<Integer> getPointsDistribution(int driverCount) {
        List<Integer> points = new ArrayList<>();
        if (driverCount <= 1) {
            points.add(maxPoints); // only one driver
            return points;
        }

        for (int i = 0; i < driverCount; i++) {
            // Linear interpolation formula
            double ratio = (double)i / (driverCount - 1); // 0 for 1st, 1 for last
            int point = (int)Math.round(maxPoints - ratio * (maxPoints - minPoints));
            points.add(Math.max(minPoints, point)); // ensure minPoints
        }

        return points;
    }

    @Override
    public int getPointsForPosition(int position, int driverCount) {
        List<Integer> distribution = getPointsDistribution(driverCount);
        if (position - 1 < distribution.size()) {
            return distribution.get(position - 1);
        }
        return minPoints; // fallback: minimum points
    }
}
