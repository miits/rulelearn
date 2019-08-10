package org.rulelearn.measures;

import org.rulelearn.data.InformationTableWithDecisionDistributions;

public interface DistanceMeasure extends Measure {
    public InformationTableWithDecisionDistributions getData();

    public double measureDistance(int xIndex, int yIndex);
}
