package com.tekad.TimingLeague.ScoringSystems;

import java.util.ArrayList;
import java.util.List;

public class BasicScoringSystem implements ScoringSystem {

    @Override
    public List<Integer> getPointsDistribution(int driverCount) {
        int topN = Math.max(10, driverCount / 3);
        List<Integer> points = new ArrayList<>();

        for (int i = 0; i < topN; i++) {
            points.add(Math.max(1, (topN - i) * 2));
        }

        return points;
    }

    @Override
    public int getPointsForPosition(int position, int driverCount) {
        List<Integer> distribution = getPointsDistribution(driverCount);
        if (position - 1 < distribution.size()) {
            return distribution.get(position - 1);
        }
        return 0;
    }
}

