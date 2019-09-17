package org.ordinalclassification.sampling;

import org.rulelearn.data.Decision;
import org.rulelearn.data.DecisionDistribution;
import org.rulelearn.data.InformationTableWithDecisionDistributions;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class WeightsCalculator {
    public static double[] getClassCountWeights(InformationTableWithDecisionDistributions data) {
        double[] weights = new double[data.getNumberOfObjects()];
        DecisionDistribution decisionDistribution = data.getDecisionDistribution();
        for (int i = 0; i < data.getNumberOfObjects(); i++) {
            Decision decision = data.getDecision(i);
            weights[i] = decisionDistribution.getCount(decision);
        }
        return weights;
    }

    public static double[] getInverseClassCountWeights(InformationTableWithDecisionDistributions data) {
        double[] weights = getClassCountWeights(data);
        weights = Arrays.stream(weights).map(x -> 1.0 / x).toArray();
        return weights;
    }

    public static int getMinCount(DecisionDistribution distribution) {
        Set<Decision> decisions = distribution.getDecisions();
        int minCount = Integer.MAX_VALUE;
        for (Decision decision: decisions) {
            int count = distribution.getCount(decision);
            if (count < minCount) {
                minCount = count;
            }
        }
        return minCount;
    }
}
