package com.tekad.TimingLeague.ScoringSystems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IECScoringSystem implements ScoringSystem {
    private static final List<Integer> DISTRIBUTION = Arrays.asList(
            30, 26, 23, 20, 17, 15, 13, 11, 9, 7, 6, 5, 4, 3, 2, 1
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
