package com.tekad.TimingLeague;

import java.util.List;

public interface ScoringSystem {
    List<Integer> getPointsDistribution(int driverCount); // List of points per placement
    int getPointsForPosition(int position, int driverCount);
}