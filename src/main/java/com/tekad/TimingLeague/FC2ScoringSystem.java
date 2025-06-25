package com.tekad.TimingLeague;

import java.util.Arrays;
import java.util.List;

public class FC2ScoringSystem implements ScoringSystem {

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
        if (position >= 1 && position <= DISTRIBUTION.size()) {
            return DISTRIBUTION.get(position - 1);
        }
        return 0;
    }
}
