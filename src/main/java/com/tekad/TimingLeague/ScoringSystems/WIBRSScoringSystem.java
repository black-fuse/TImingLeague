package com.tekad.TimingLeague.ScoringSystems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WIBRSScoringSystem implements ScoringSystem {
    private static final List<Integer> DISTRIBUTION = Arrays.asList(
            50, 47, 44, 41, 38, 36, 34, 32, 30, 28,
            27, 26, 25, 24, 23, 22, 21, 20, 19, 18,
            17, 16, 15, 14, 13, 12, 11, 10, 9, 8,
            7, 6, 5, 4, 3, 2, 2, 1, 1
    );

    @Override
    public List<Integer> getPointsDistribution(int driverCount) {
        return DISTRIBUTION;
    }

    @Override
    public int getPointsForPosition(int position, int driverCount) {
        List<Integer> distribution = getPointsDistribution(driverCount);

        // Ensure everyone at least 1 point
        if (position - 1 < distribution.size()) {
            return Math.max(1, distribution.get(position - 1));
        }
        return 1; // fallback: always at least 1
    }
}
