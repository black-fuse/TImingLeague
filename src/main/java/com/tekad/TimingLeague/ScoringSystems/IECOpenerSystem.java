package com.tekad.TimingLeague.ScoringSystems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IECOpenerSystem implements ScoringSystem {
    private static final List<Integer> DISTRIBUTION = Arrays.asList(
            45, 39, 35, 30, 26, 23, 20, 17, 14, 11, 9, 8, 6, 5, 3, 2, 2, 2
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
