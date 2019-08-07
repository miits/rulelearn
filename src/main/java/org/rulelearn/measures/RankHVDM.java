package org.rulelearn.measures;

import org.rulelearn.data.*;
import org.rulelearn.types.*;
import org.ordinalclassification.types.FieldValueWrapper;
import org.ordinalclassification.utils.*;

import java.util.*;

public class RankHVDM implements Measure {
    private InformationTableWithDecisionDistributions data;
    private int numberOfAttributes;
    private boolean[] attributeIsNominal;
    private int[] nominalAttributeValuesNumber;
    private double[][] values;
    private boolean[][] missingValues;
    private AttributePreferenceType[] types;
    private HashMap<Double, Double>[] ranks;

    public RankHVDM(InformationTableWithDecisionDistributions data) {
        this.data = data;
        this.numberOfAttributes = data.getActiveConditionAttributeFields().getNumberOfAttributes();
        initNominalAttributesMarking();
        initPreferenceTypes();
        initValues();
        initRanks();
    }

    private void initNominalAttributesMarking() {
        this.attributeIsNominal = new boolean[numberOfAttributes];
        this.nominalAttributeValuesNumber = new int[numberOfAttributes];
        for (int i = 0; i < numberOfAttributes; i++) {
            Attribute attribute = data.getActiveConditionAttributeFields().getAttributes()[i];
            Field field = attribute.getValueType();
            if (field instanceof EnumerationField) {
                attributeIsNominal[i] = true;
                nominalAttributeValuesNumber[i] = ((EnumerationField) field).getElementList().getSize();
            } else {
                attributeIsNominal[i] = false;
            }
        }
    }

    private void initValues() {
        this.values = new double[data.getNumberOfObjects()][numberOfAttributes];
        this.missingValues = new boolean[data.getNumberOfObjects()][numberOfAttributes];
        for (int i = 0; i < data.getNumberOfObjects(); i++) {
            for (int j = 0; j < numberOfAttributes; j++) {
                FieldValueWrapper fieldValue = new FieldValueWrapper(data.getActiveConditionAttributeFields().getField(i, j));
                values[i][j] = fieldValue.getValue();
                missingValues[i][j] = fieldValue.isMissing();
            }
        }
    }

    private void initPreferenceTypes() {
        EvaluationAttribute[] attributes = data.getActiveConditionAttributeFields().getAttributes();
        types = new AttributePreferenceType[attributes.length];
        for (int i = 0; i < attributes.length; i++) {
            types[i] = attributes[i].getPreferenceType();
        }
    }

    private void initRanks() {
        ranks = new HashMap[numberOfAttributes];
        for (int i = 0; i < numberOfAttributes; i++) {
            ranks[i] = getRanksForAttribute(i);
        }
    }

    private HashMap<Double, Double> getRanksForAttribute(int attrIndex) {
        switch (types[attrIndex]) {
            case GAIN: {
                return rankGainValues(attrIndex);
            }
            case COST: {
                return rankCostValues(attrIndex);
            }
            default: {
                return rankNoPreferenceValues(attrIndex);
            }
        }
    }

    private HashMap<Double, Double> rankGainValues(int attrIndex) {
        return rankValues(attrIndex, true);
    }

    private HashMap<Double, Double> rankCostValues(int attrIndex) {
        return rankValues(attrIndex, false);
    }

    private HashMap<Double, Double> rankNoPreferenceValues(int attrIndex) {
        return rankValues(attrIndex, false);
    }

    private HashMap<Double, Double> rankValues(int attrIndex, boolean gain) {
        double[] sorted = DataSubsetExtractor.get2dArrayColumn(values, attrIndex).clone();
        Arrays.sort(sorted);
        if (gain) {
            Collections.reverse(Arrays.asList(sorted));
        }
        HashMap<Double, Double> ranksForAttribute = new HashMap<>(sorted.length);
        HashMap<Double, Double> avgRanks = getAverageRanks(sorted);
        for (int i = 0; i < sorted.length; i++) {
            if (avgRanks.containsKey(sorted[i])) {
                ranksForAttribute.put(sorted[i], avgRanks.get(sorted[i]));
            } else {
                ranksForAttribute.put(sorted[i], (double) i);
            }
        }
        return ranksForAttribute;
    }

    public static HashMap<Double, Double> getAverageRanks(double[] rankValues) {
        HashMap<Double, Double> avgRanksByValue = new HashMap<>();
        double previous = Double.NaN;
        double sum = 0.0;
        int count = 1;
        for (int i = 0; i < rankValues.length; i++) {
            if (rankValues[i] == previous) {
                sum += i;
                count++;
                if (i == rankValues.length - 1) {
                    double avgRank = sum / count;
                    avgRanksByValue.put(rankValues[i], avgRank);
                }
            } else {
                if (count > 1) {
                    double avgRank = sum / count;
                    count = 1;
                    avgRanksByValue.put(rankValues[i - 1], avgRank);
                }
                sum = i;
            }
            previous = rankValues[i];
        }
        return avgRanksByValue;
    }

    public double measureDistance(int xIndex, int yIndex) {
        double distance = 0;
        for (int i = 0; i < numberOfAttributes; i++) {
            distance += Math.pow(getDistance(i, xIndex, yIndex), 2.0);
        }
        return Math.sqrt(distance);
    }

    private double getDistance(int attributeIndex, int xIndex, int yIndex) {
        if (missingValues[xIndex][attributeIndex] || missingValues[yIndex][attributeIndex]) {
            return 1.0;
        }
        if (attributeIsNominal[attributeIndex]) {
            return 1.0;
        } else {
            return 1.0;
        }
    }

    public InformationTableWithDecisionDistributions getData() {
        return data;
    }

    @Override
    public MeasureType getType() {
        return MeasureType.COST;
    }
}

