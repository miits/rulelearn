package org.ordinalclassification.classifiers;

import org.ordinalclassification.types.LearningExampleType;
import org.ordinalclassification.utils.KernelLabeler;
import org.rulelearn.core.TernaryLogicValue;
import org.rulelearn.data.Decision;
import org.rulelearn.data.EvaluationAttribute;
import org.rulelearn.data.Table;
import org.rulelearn.measures.HVDM;
import org.rulelearn.types.EvaluationField;

import java.util.ArrayList;
import java.util.HashMap;

public class MonotonicKernelAnalyzer extends NearestNeighborsAnalyzer {
    private Decision majorityLimitingDecision;
    private Decision minorityLimitingDecision;
    private KernelLabeler labeler;
    private double kernelWidth;
    private Table<EvaluationAttribute, EvaluationField> evaluations;
    private int numberOfConditionAttributes;

    public MonotonicKernelAnalyzer(HVDM measure, int[] majorityIndices, int[] minorityIndices, Decision majorityLimitingDecision, Decision minorityLimitingDecision, KernelLabeler labeler) {
        super(measure, majorityIndices, minorityIndices);
        this.majorityLimitingDecision = majorityLimitingDecision;
        this.minorityLimitingDecision = minorityLimitingDecision;
        this.labeler = labeler;
        this.evaluations = measure.getData().getActiveConditionAttributeFields();
        this.numberOfConditionAttributes = measure.getData().getNumberOfAttributes() - 1;
        setKernelWidth();
    }

    private void setKernelWidth() {
        double sum = 0.0;
        for (int i: minorityIndices) {
            for (int j = 1; j <= 5; j++) {
                sum += getDistanceToNthNeighbour(i, j);
            }
        }
        kernelWidth = sum / (double) (minorityIndices.length * 5);
    }

    private double getDistanceToNthNeighbour(int exampleIndex, int n) {
        HashMap<Integer, Double> exampleDistances = distances.getExampleDistances(exampleIndex);
        int[] objectsIndicesSorted = getObjectsIndicesSortedByDistance(exampleDistances);
        return distances.getDistance(exampleIndex, objectsIndicesSorted[n - 1]);
    }

    @Override
    protected LearningExampleType labelExample(int exampleIndex) {
        int[] inKernel = getIndicesOfObjectsInKernel(exampleIndex);
        double ratio = getBestRatio(inKernel, exampleIndex);
        return labeler.labelExample(ratio);
    }

    private int[] getIndicesOfObjectsInKernel(int exampleIndex) {
        HashMap<Integer, Double> exampleDistances = distances.getExampleDistances(exampleIndex);
        int[] objectsIndicesSorted = getObjectsIndicesSortedByDistance(exampleDistances);
        ArrayList<Integer> inKernelWindow = new ArrayList<>();
        for (int i: objectsIndicesSorted) {
            if (exampleDistances.get(i) >= kernelWidth) {
                break;
            }
            inKernelWindow.add(i);
        }
        int[] inKernelWindowArr = new int[inKernelWindow.size()];
        int i = 0;
        for (int index: inKernelWindow) {
            inKernelWindowArr[i] = index;
            i++;
        }
        return inKernelWindowArr;
    }

    private double getBestRatio(int[] inKernel, int exampleIndex) {
        double bestRatio = 0.0;
        for (int attrIndex = 0; attrIndex < numberOfConditionAttributes; attrIndex++) {
            EvaluationField mainEvaluation = evaluations.getField(exampleIndex, attrIndex);
            int[] atLeastIndices = getAtLeastAsGoodFromKernel(inKernel, attrIndex, mainEvaluation);
            int[] atMostIndices = getAtMostAsGoodFromKernel(inKernel, attrIndex, mainEvaluation);
            double atLeastRatio = calcRatio(atLeastIndices, exampleIndex);
            double atMostRatio = calcRatio(atMostIndices, exampleIndex);
            double ratio = atLeastRatio > atMostRatio ? atLeastRatio : atMostRatio;
            if (ratio > bestRatio) {
                bestRatio = ratio;
            }
        }
        return bestRatio;
    }

    private int[] getAtLeastAsGoodFromKernel(int[] inKernel, int attrIndex, EvaluationField mainEvaluation) {
        ArrayList<Integer> indices = new ArrayList<>();
        for (int neighbourIndex: inKernel) {
            boolean isAtLeastAsGood = evaluations.getField(neighbourIndex, attrIndex).isAtLeastAsGoodAs(mainEvaluation) == TernaryLogicValue.TRUE;
            if (isAtLeastAsGood) {
                indices.add(neighbourIndex);
            }
        }
        int[] atLeastIndices = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            atLeastIndices[i] = indices.get(i);
        }
        return atLeastIndices;
    }

    private int[] getAtMostAsGoodFromKernel(int[] inKernel, int attrIndex, EvaluationField mainEvaluation) {
        ArrayList<Integer> indices = new ArrayList<>();
        for (int neighbourIndex: inKernel) {
            boolean isAtMostAsGood = evaluations.getField(neighbourIndex, attrIndex).isAtMostAsGoodAs(mainEvaluation) == TernaryLogicValue.TRUE;
            if (isAtMostAsGood) {
                indices.add(neighbourIndex);
            }
        }
        int[] atMostIndices = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            atMostIndices[i] = indices.get(i);
        }
        return atMostIndices;
    }

    private double calcRatio(int[] indices, int exampleIndex) {
        double minorityDecisionWeightedSum = weightedSumForDecision(indices, exampleIndex, minorityLimitingDecision);
        double majorityDecisionWeightedSum = weightedSumForDecision(indices, exampleIndex, majorityLimitingDecision);
        double ratio = minorityDecisionWeightedSum / (minorityDecisionWeightedSum + majorityDecisionWeightedSum);
        return ratio;
    }

    private double weightedSumForDecision(int[] inKernel, int exampleIndex, Decision decision) {
        double sum = 0.0;
        for (int index: inKernel) {
            if (measure.getData().getDecision(index).equals(decision)) {
                sum += getEpanechnikov(distances.getDistance(exampleIndex, index));
            }
        }
        return sum;
    }

    private double getEpanechnikov(double distance) {
        double u = Math.abs(distance);
        return 3.0 / 4.0 * ((kernelWidth - Math.pow(u, 2.0)) / kernelWidth);
    }
}
