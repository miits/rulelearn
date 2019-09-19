package org.ordinalclassification.main;

import org.ordinalclassification.sampling.SampleAnalyzer;
import org.ordinalclassification.sampling.SamplerTest;
import org.ordinalclassification.utils.JsonCsvDatasetIterator;
import org.ordinalclassification.utils.NeighbourhoodAnalyzer;

public class Main {

    public static void main(String[] args) {
//        neighbourhoodAnalysis(args);
//        imbalancedSampling(args);
        samplingAnalysis(args);
    }

    private static void neighbourhoodAnalysis(String[] args) {
        if (args.length < 4) {
            throw new IllegalArgumentException("Argument missing");
        }
        String measureName = "";
        if (args.length > 4) {
            measureName = args[4];
        }
        JsonCsvDatasetIterator iterator = new JsonCsvDatasetIterator(args[0], args[1], args[2], Integer.parseInt(args[3]), measureName);
        NeighbourhoodAnalyzer analyzer = new NeighbourhoodAnalyzer();
        iterator.iterate(analyzer);
    }

    private static void imbalancedSampling(String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("Argument missing");
        }
        JsonCsvDatasetIterator iterator = new JsonCsvDatasetIterator(args[0], args[1], args[2], 5, "");
        SamplerTest samplerTest = new SamplerTest();
        iterator.iterate(samplerTest);
    }

    private static void samplingAnalysis(String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("Argument missing");
        }
        String measureName = "";
        String weightsMode = "";
        String sampleSizeMode = "";
        int nSamples;
        if (args.length > 7) {
            measureName = args[4];
            weightsMode = args[5];
            sampleSizeMode = args[6];
            nSamples = Integer.parseInt(args[7]);
        } else {
            weightsMode = args[4];
            sampleSizeMode = args[5];
            nSamples = Integer.parseInt(args[6]);
        }
        JsonCsvDatasetIterator iterator = new JsonCsvDatasetIterator(args[0], args[1], args[2], Integer.parseInt(args[3]), measureName);
        SampleAnalyzer analyzer = new SampleAnalyzer(weightsMode, sampleSizeMode, nSamples);
        iterator.iterate(analyzer);
    }

    private static void checkArgs(String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("Argument missing");
        }
    }
}
