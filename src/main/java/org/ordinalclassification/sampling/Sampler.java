package org.ordinalclassification.sampling;

import org.rulelearn.data.InformationTableWithDecisionDistributions;

import java.util.ArrayList;
import java.util.Arrays;

public class Sampler {
    private InformationTableWithDecisionDistributions data;
    private double[] weights;

    public Sampler(InformationTableWithDecisionDistributions data) {
        this.data = data;
    }

    public Sampler(InformationTableWithDecisionDistributions data, double[] weights) {
        this.data = data;
        this.weights = weights;
    }

    public double[] getWeights() {
        return weights;
    }

    public void setWeights(double[] weights) {
        this.weights = weights;
    }

    public void setOnesWeights() {
        initWeights();
    }

    public void setUniformDistributionWeights() {
        int size = data.getNumberOfObjects();
        weights = new double[size];
        Arrays.fill(weights, 1.0 / (double) size);
    }

    public ArrayList<int[]> getWeightedRandomSamples(int nSamples, int size) {
        ArrayList<int[]> samples = new ArrayList<>();
        for (int i = 0; i < nSamples; i++) {
            int[] sample = getWeightedBootstrap(size);
            samples.add(sample);
        }
        return samples;
    }

    public int[] getWeightedBootstrap(int size) {
        int datasetSize = data.getNumberOfObjects();
        int[] sample = new int[size];
        int i = 0;
        while (i < size) {
            int randomIndex = randomIndexFromRange(0, datasetSize - 1);
            if (randomLowerThanWeight(randomIndex)) {
                sample[i] = randomIndex;
                i++;
            }
        }
        return sample;
    }

    int randomIndexFromRange(int min, int max)
    {
        int range = (max - min) + 1;
        return (int)(Math.random() * range) + min;
    }

    private boolean randomLowerThanWeight(int index) {
        double rand = Math.random();
        return rand < weights[index];
    }

    public int[] getWeightedRandomSample() {
        int[] sample = new int[data.getNumberOfObjects()];
        for (int i = 0; i < data.getNumberOfObjects(); i++) {
            sample[i] = getRandomIndex();
        }
        return sample;
    }

    private int getRandomIndex() {
        double[] cumulativeWeights = getWeightsCumulativeSum();
        double rand = Math.random();
        for (int i = 0; i < cumulativeWeights.length; i++) {
            if (rand < cumulativeWeights[i]) {
                return i;
            }
        }
        return cumulativeWeights.length - 1;
    }

    private double[] getWeightsCumulativeSum() {
        double[] cumSum = new double[weights.length];
        double total = 0.0;
        for (int i = 0; i < weights.length; i++) {
            total += weights[i];
            cumSum[i] = total;
        }
        return cumSum;
    }

    private void initWeights() {
        int size = data.getNumberOfObjects();
        weights = new double[size];
        Arrays.fill(weights, 1.0);
    }
}
