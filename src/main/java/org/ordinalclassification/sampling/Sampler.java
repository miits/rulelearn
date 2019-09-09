package org.ordinalclassification.sampling;

import org.rulelearn.data.InformationTableWithDecisionDistributions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Sampler {
    private InformationTableWithDecisionDistributions data;
    private double[] weights;
    private Random randomGenerator;


    public Sampler(InformationTableWithDecisionDistributions data) {
        this.data = data;
        this.randomGenerator = new Random();
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
            int randomIndex = randomGenerator.nextInt(datasetSize);
            if (randomLowerThanWeight(randomIndex)) {
                sample[i] = randomIndex;
                i++;
            }
        }
        return sample;
    }

    private void initWeights() {
        int size = data.getNumberOfObjects();
        weights = new double[size];
        Arrays.fill(weights, 1.0);
    }

    private boolean randomLowerThanWeight(int index) {
        double rand = Math.random();
        return rand < weights[index];
    }
}
