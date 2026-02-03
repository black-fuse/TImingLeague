package com.tekad.TimingLeague.ScoringSystems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IECDouble implements ScoringSystem {
    private static final List<Integer> DISTRIBUTION = Arrays.asList(
            60, 52, 46, 40, 34, 30, 26, 22, 18, 14, 12, 10, 8, 6, 4, 2
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
