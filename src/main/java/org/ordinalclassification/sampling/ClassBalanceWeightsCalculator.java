package org.ordinalclassification.sampling;

import org.rulelearn.data.Decision;
import org.rulelearn.data.DecisionDistribution;
import org.rulelearn.data.InformationTableWithDecisionDistributions;

import java.util.Set;

public class ClassBalanceWeightsCalculator {
    public static double[] getWeights(InformationTableWithDecisionDistributions data) {
        double[] weights = new double[data.getNumberOfObjects()];
        DecisionDistribution decisionDistribution = data.getDecisionDistribution();
        int minCount = getMinCount(decisionDistribution);
        for (int i = 0; i < data.getNumberOfObjects(); i++) {
            Decision decision = data.getDecision(i);
            weights[i] = (double) minCount / (double) decisionDistribution.getCount(decision);
        }
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
