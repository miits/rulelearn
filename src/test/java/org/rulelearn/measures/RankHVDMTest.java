package org.rulelearn.measures;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;


class RankHVDMTest {

    @Test
    void getAverageRanks() {
        HashMap<Double, Double> averageRanks = RankHVDM.getAverageRanks(new double[]{100.0, 50.0, 50.0, 20.0, 10.5, 10.5});
        assertEquals(2, averageRanks.size());
        double val = averageRanks.get(50.0);
        assertEquals(1.5, val);
        val = averageRanks.get(10.5);
        assertEquals(4.5, val);
    }
}