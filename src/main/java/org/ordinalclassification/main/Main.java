package org.ordinalclassification.main;

import org.ordinalclassification.sampling.SamplerTest;
import org.ordinalclassification.utils.JsonCsvDatasetIterator;
import org.ordinalclassification.utils.NeighbourhoodAnalyzer;

public class Main {

    public static void main(String[] args) {
//        neighbourhoodAnalysis(args);
        imbalancedSampling(args);
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
        String jsonPath = args[0];
        String csvPath = args[1];
        String resultsPath = args[2];
        JsonCsvDatasetIterator iterator = new JsonCsvDatasetIterator(args[0], args[1], args[2], 5, "");
        SamplerTest samplerTest = new SamplerTest();
        iterator.iterate(samplerTest);
    }
}
