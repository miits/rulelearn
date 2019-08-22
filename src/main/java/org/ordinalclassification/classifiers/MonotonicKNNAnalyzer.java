package org.ordinalclassification.classifiers;

import org.ordinalclassification.utils.KNearestLabeler;
import org.rulelearn.core.TernaryLogicValue;
import org.rulelearn.data.*;
import org.rulelearn.measures.DistanceMeasure;
import org.rulelearn.measures.HVDM;
import org.rulelearn.types.EvaluationField;

import java.util.ArrayList;
import java.util.HashMap;

public class MonotonicKNNAnalyzer extends NearestNeighborsAnalyzer {
    private int k;
    private KNearestLabeler labeler;
    private HashMap<Integer, int[][]> neighbourhoods;
    private Table<EvaluationAttribute, EvaluationField> evaluations;
    private int numberOfConditionAttributes;

    public MonotonicKNNAnalyzer(DistanceMeasure measure, int[] majorityIndices, int[] minorityIndices, int k, KNearestLabeler labeler) {
        super(measure, majorityIndices, minorityIndices);
        this.k = k;
        this.labeler = labeler;
        this.evaluations = measure.getData().getActiveConditionAttributeFields();
        this.numberOfConditionAttributes = measure.getData().getNumberOfAttributes() - 1;
        buildNeighbourhoods();
    }

    @Override
    public void labelExamples() {
        for (int index: minorityIndices) {
            int[][] divided = neighbourhoods.get(index); // divided[0][] - minority, divided[1][] - majority
            int[][] best = getBestDivide(index, divided[0], divided[1]);
            labelsAssignment.put(index, labeler.labelExampleWithRareCheckForMonotonicNeighbourhood(index, neighbourhoods, best));
        }
    }

    private int[][] getBestDivide(int objectIndex, int[] minNeighbours, int[] majNeighbours) {
        int[][] bestDivide = new int[2][];
        double bestRatio = 0.0;
        boolean set = false;
        for (int attrIndex = 0; attrIndex < numberOfConditionAttributes; attrIndex++) {
            int[][] atLeastDivide = getAtLeastAsGoodOnAttribute(objectIndex, attrIndex, minNeighbours, majNeighbours);
            int[][] atMostDivide = getAtMostAsGoodOnAttribute(objectIndex, attrIndex, minNeighbours, majNeighbours);
            double atLeastRatio = (double) atLeastDivide[0].length / (double) atLeastDivide[1].length;
            double atMostRatio = (double) atMostDivide[0].length / (double) atMostDivide[1].length;
            double ratio = atLeastRatio > atMostRatio ? atLeastRatio : atMostRatio;
            if (ratio > bestRatio) {
                bestRatio = ratio;
                bestDivide = atLeastRatio > atMostRatio ? atLeastDivide : atMostDivide;
                set = true;
            }
            if (attrIndex == numberOfConditionAttributes - 1 && !set) {
                bestDivide = atLeastDivide;
            }
        }
        return bestDivide;
    }

    private int[][] getAtLeastAsGoodOnAttribute(int objectIndex, int attrIndex, int[] minNeighbours, int[] majNeighbours) {
        EvaluationField mainEvaluation = evaluations.getField(objectIndex, attrIndex);
        ArrayList<Integer> minIndices = new ArrayList<>();
        ArrayList<Integer> majIndices = new ArrayList<>();
        int[][] divide = new int[2][];
        for (int minIndex: minNeighbours) {
            if (evaluations.getField(minIndex, attrIndex).isAtLeastAsGoodAs(mainEvaluation) == TernaryLogicValue.TRUE) {
                minIndices.add(minIndex);
            }
        }
        for (int majIndex: majNeighbours) {
            if (evaluations.getField(majIndex, attrIndex).isAtLeastAsGoodAs(mainEvaluation) == TernaryLogicValue.TRUE) {
                majIndices.add(majIndex);
            }
        }
        divide[0] = new int[minIndices.size()];
        for (int i = 0; i < minIndices.size(); i++) {
            divide[0][i] = minIndices.get(i);
        }
        divide[1] = new int[majIndices.size()];
        for (int i = 0; i < majIndices.size(); i++) {
            divide[1][i] = majIndices.get(i);
        }
        return divide;
    }

    private int[][] getAtMostAsGoodOnAttribute(int objectIndex, int attrIndex, int[] minNeighbours, int[] majNeighbours) {
        EvaluationField mainEvaluation = evaluations.getField(objectIndex, attrIndex);
        ArrayList<Integer> minIndices = new ArrayList<>();
        ArrayList<Integer> majIndices = new ArrayList<>();
        int[][] divide = new int[2][];
        for (int minIndex: minNeighbours) {
            if (evaluations.getField(minIndex, attrIndex).isAtMostAsGoodAs(mainEvaluation) == TernaryLogicValue.TRUE) {
                minIndices.add(minIndex);
            }
        }
        for (int majIndex: majNeighbours) {
            if (evaluations.getField(majIndex, attrIndex).isAtMostAsGoodAs(mainEvaluation) == TernaryLogicValue.TRUE) {
                majIndices.add(majIndex);
            }
        }
        divide[0] = new int[minIndices.size()];
        for (int i = 0; i < minIndices.size(); i++) {
            divide[0][i] = minIndices.get(i);
        }
        divide[1] = new int[majIndices.size()];
        for (int i = 0; i < majIndices.size(); i++) {
            divide[1][i] = majIndices.get(i);
        }
        return divide;
    }

    private void buildNeighbourhoods() {
        neighbourhoods = new HashMap<>();
        for (int minorityExampleIndex: minorityIndices) {
            int[] kNearest = getKNearestIndices(minorityExampleIndex);
            Decision[] decisions = getDecisions(kNearest);
            int[][] divided = divideNeighbours(kNearest, decisions, measure.getData().getDecision(minorityExampleIndex));
            neighbourhoods.put(minorityExampleIndex, divided);
        }
    }

    private int[] getKNearestIndices(int exampleIndex) {
        HashMap<Integer, Double> exampleDistances = distances.getExampleDistances(exampleIndex);
        int[] indices = getObjectsIndicesSortedByDistance(exampleDistances);
        int[] kNearesIndices = new int[k];
        for (int i = 0; i < k; i++) {
            kNearesIndices[i] = indices[i];
        }
        return kNearesIndices;
    }

    private Decision[] getDecisions(int[] objectIndices) {
        Decision[] decisions = new Decision[objectIndices.length];
        int i = 0;
        for (int index: objectIndices) {
            decisions[i] = measure.getData().getDecision(index);
            i++;
        }
        return decisions;
    }

    private int[][] divideNeighbours(int[] kNearest, Decision[] decisions, Decision minDecision) {
        ArrayList<Integer> minNeighbours = new ArrayList<>();
        ArrayList<Integer> majNeighbours = new ArrayList<>();
        for (int i = 0; i < kNearest.length; i++) {
            if (decisions[i].equals(minDecision)) {
                minNeighbours.add(kNearest[i]);
            } else {
                majNeighbours.add(kNearest[i]);
            }
        }
        int[][] divided = new int[2][];
        divided[0] = minNeighbours.stream().mapToInt(x -> x).toArray();
        divided[1] = majNeighbours.stream().mapToInt(x -> x).toArray();
        return divided;
    }
}
