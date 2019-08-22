package org.ordinalclassification.main;

import org.ordinalclassification.utils.JsonCsvDatasetIterator;
import org.ordinalclassification.utils.NeighbourhoodAnalyzer;

public class Main {

    public static void main(String[] args) {
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
}
