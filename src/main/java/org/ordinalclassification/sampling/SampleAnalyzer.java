package org.ordinalclassification.sampling;

import org.apache.commons.io.FilenameUtils;
import org.ordinalclassification.utils.DatasetOperation;
import org.ordinalclassification.utils.NeighbourhoodAnalyzer;
import org.rulelearn.data.DecisionDistribution;
import org.rulelearn.data.InformationTableBuilder;
import org.rulelearn.data.InformationTableWithDecisionDistributions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SampleAnalyzer implements DatasetOperation{
    private String jsonPath;
    private String csvPath;
    private String resultsPath;
    private String weightsMode;
    private String sampleSizeMode;
    private int nSamples;
    private int k;
    private String measure;
    private String datasetName;
    private HashMap<String, ArrayList<int[]>> resultsByDatasetName;
    private InformationTableWithDecisionDistributions informationTable;
    private String csvSeparator = ",";

    public SampleAnalyzer(String weightsMode, String sampleSizeMode, int nSamples) {
        this.weightsMode = weightsMode;
        this.sampleSizeMode = sampleSizeMode;
        this.nSamples = nSamples;
    }

    @Override
    public void carryOut(String[] args) {
        try {
            loadArgs(args);
            loadData();
            runSampler();
            runNeighbourhoodAnalyzer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadArgs(String[] args) {
        try {
            checkArgs(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        jsonPath = args[0];
        csvPath = args[1];
        resultsPath = Paths.get(args[2]).getParent().toString();
        k = Integer.parseInt(args[3]);
        measure = args[4];
        setDatasetName();
    }

    private void setDatasetName() {
        File f = new File(jsonPath);
        datasetName = FilenameUtils.removeExtension(f.getName());
    }

    private void checkArgs(String[] args) throws IllegalArgumentException {
        if (args.length < 3) {
            throw new IllegalArgumentException("Argument missing");
        }
    }

    private void loadData() throws IOException {
        informationTable = new InformationTableWithDecisionDistributions(
                InformationTableBuilder.safelyBuildFromCSVFile(jsonPath, csvPath, false));
        resultsByDatasetName = new HashMap<>();
    }

    private void runSampler() {
        System.out.println("[SampleAnalyzer] Generating samples");
        Sampler sampler = new Sampler(informationTable);
        setWeights(sampler);
        int size = getSampleSize();
        ArrayList<int[]> results = sampler.getWeightedRandomSamples(nSamples, size);
        resultsByDatasetName.put(datasetName, results);
    }

    private void setWeights(Sampler sampler) {
        if (weightsMode.equals("ones")) {
            sampler.setOnesWeights();
        } else if (weightsMode.equals("inv_class_count")) {
            double[] weights = WeightsCalculator.getInverseClassCountWeights(informationTable);
            sampler.setWeights(weights);
        }
    }

    private int getSampleSize() {
        if (sampleSizeMode.equals("dataset")) {
            return informationTable.getNumberOfObjects();
        } else if (sampleSizeMode.equals("undersampling")) {
            DecisionDistribution decisionDistribution = informationTable.getDecisionDistribution();
            int minCount = WeightsCalculator.getMinCount(decisionDistribution);
            int classesNumber = decisionDistribution.getDecisions().size();
            int size = minCount * classesNumber;
            return size;
        }
        return informationTable.getNumberOfObjects();
    }

    private void runNeighbourhoodAnalyzer() throws IOException {
        for (Map.Entry<String, ArrayList<int[]>> entry: resultsByDatasetName.entrySet()) {
            ArrayList<int[]> samples = entry.getValue();
            int i = 0;
            for (int[] sampleIndices: samples) {
                i++;
                System.out.println(String.format("[SampleAnalyzer] Analyzing sample %d/%d", i, nSamples));
                InformationTableWithDecisionDistributions sample = new InformationTableWithDecisionDistributions(informationTable.select(sampleIndices));
                String sampleResultsPath = String.format("%s/%s/%s_%d", resultsPath, datasetName, "sample", i);
                NeighbourhoodAnalyzer analyzer = new NeighbourhoodAnalyzer(csvPath, jsonPath, sampleResultsPath, k, measure);
                analyzer.loadData(sample);
                analyzer.analyze();
                analyzer.saveResults();
            }
        }
    }

    private void createResultsDirIfNotExists() {
        if (Files.notExists(Paths.get(resultsPath))) {
            new File(resultsPath).mkdirs();
        }
    }

    private void saveIndicesSetToCsv(String filename, ArrayList<int[]> indicesSet) throws IOException {
        String filepath = Paths.get(resultsPath, filename + ".csv").toString();
        FileWriter fw = new FileWriter(filepath, true);
        BufferedWriter bw = new BufferedWriter(fw);
        for (int[] indices : indicesSet) {
            String indicesSeparated = Arrays.stream(indices)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(csvSeparator));
            bw.write(indicesSeparated);
            bw.newLine();
        }
        bw.flush();
        bw.close();
    }
}

