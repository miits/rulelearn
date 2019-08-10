package org.ordinalclassification.utils;

import it.unimi.dsi.fastutil.ints.IntSortedSet;
import org.ordinalclassification.classifiers.*;
import org.rulelearn.approximations.Union;
import org.rulelearn.approximations.UnionWithSingleLimitingDecision;
import org.rulelearn.data.*;
import org.rulelearn.measures.HVDM;
import org.ordinalclassification.types.AnalysisResult;
import org.ordinalclassification.types.LearningExampleType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class NeighbourhoodAnalyzer implements DatasetOperation {
    private String jsonPath;
    private String csvPath;
    private String resultsPath;
    private DataSubsetExtractor dataExtractor;
    private HVDM measure;
    private HashMap<String, AnalysisResult> resultsByName;
    private static String unionVsUnionKernelFilename = "union_vs_union_kernel";
    private static String unionVsUnionKNNFilename = "union_vs_union_knn";
    private static String unionVsUnionKernelMonotonicFilename = "union_vs_union_kernel_monotonic";
    private static String unionVsUnionKNNMonotonicFilename = "union_vs_union_knn_monotonic";
    private static String classVsUnionKernelFilename = "class_vs_union_kernel";
    private static String classVsUnionKNNFilename = "class_vs_union_knn";
    private static String classVsUnionKernelMonotonicFilename = "class_vs_union_kernel_monotonic";
    private static String classVsUnionKNNMonotonicFilename = "class_vs_union_knn_monotonic";

    public NeighbourhoodAnalyzer(String jsonPath, String csvPath, String resultsPath) {
        this.jsonPath = jsonPath;
        this.csvPath = csvPath;
        this.resultsPath = resultsPath;
    }

    public NeighbourhoodAnalyzer() {
    }

    @Override
    public void carryOut(String[] args) {
        loadArgs(args);
        runAnalysis();
    }

    private void loadArgs(String[] args) {
        try {
            checkArgs(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        jsonPath = args[0];
        csvPath = args[1];
        resultsPath = args[2];
    }

    private void checkArgs(String[] args) throws IllegalArgumentException{
        if (args.length < 3) {
            throw new IllegalArgumentException("Argument missing");
        }
    }

    public void runAnalysis() {
        try {
            loadData();
            analyze();
            saveResults();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void runAnalysisSilent() {
        try {
            loadData();
            analyze();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadData() throws IOException {
        InformationTableWithDecisionDistributions informationTable = new InformationTableWithDecisionDistributions(
                InformationTableBuilder.safelyBuildFromCSVFile(jsonPath, csvPath, false));
        dataExtractor = new DataSubsetExtractor(informationTable);
        measure = new HVDM(dataExtractor.getData());
        resultsByName = new HashMap<>();
    }

    private void analyze() {
        Union[] atLeastUnions = dataExtractor.getAtLeastUnions();
        Union[] atMostUnions = dataExtractor.getAtMostUnions();
        Collections.reverse(Arrays.asList(atLeastUnions));
        unionVsUnionAnalysis(atLeastUnions, atMostUnions);
        HashMap<Decision, int[]> classesByDecision = dataExtractor.getClassesByDecision();
        classVsUnionAnalysis(classesByDecision, atLeastUnions, atMostUnions);
    }

    private void unionVsUnionAnalysis(Union[] atLeastUnions, Union[] atMostUnions) {
        Iterator<Union> atLeastUnionIterator = Arrays.stream(atLeastUnions).iterator();
        Iterator<Union> atMostUnionIterator = Arrays.stream(atMostUnions).iterator();
        resultsByName.put(unionVsUnionKNNFilename, new AnalysisResult());
        resultsByName.put(unionVsUnionKernelFilename, new AnalysisResult());
        resultsByName.put(unionVsUnionKNNMonotonicFilename, new AnalysisResult());
        resultsByName.put(unionVsUnionKernelMonotonicFilename, new AnalysisResult());
        while (atLeastUnionIterator.hasNext() && atMostUnionIterator.hasNext()) {
            UnionWithSingleLimitingDecision atLeastUnion = (UnionWithSingleLimitingDecision) atLeastUnionIterator.next();
            UnionWithSingleLimitingDecision atMostUnion = (UnionWithSingleLimitingDecision) atMostUnionIterator.next();
            int[] atLeast = unionToArray(atLeastUnion);
            int[] atMost = unionToArray(atMostUnion);
            if (atLeast.length > atMost.length) {
                performKNearestAnalysis(atLeast, atMost, atLeastUnion.getLimitingDecision(), atMostUnion.getLimitingDecision(), unionVsUnionKNNFilename);
                performKernelAnalysis(atLeast, atMost, atLeastUnion.getLimitingDecision(), atMostUnion.getLimitingDecision(), unionVsUnionKernelFilename);
                performKNearestMonotonicAnalysis(atLeast, atMost, atLeastUnion.getLimitingDecision(), atMostUnion.getLimitingDecision(), unionVsUnionKNNMonotonicFilename);
                performKernelMonotonicAnalysis(atLeast, atMost, atLeastUnion.getLimitingDecision(), atMostUnion.getLimitingDecision(), unionVsUnionKernelMonotonicFilename);
            } else {
                performKNearestAnalysis(atMost, atLeast, atMostUnion.getLimitingDecision(), atLeastUnion.getLimitingDecision(), unionVsUnionKNNFilename);
                performKernelAnalysis(atMost, atLeast, atMostUnion.getLimitingDecision(), atLeastUnion.getLimitingDecision(), unionVsUnionKernelFilename);
                performKNearestMonotonicAnalysis(atMost, atLeast, atMostUnion.getLimitingDecision(), atLeastUnion.getLimitingDecision(), unionVsUnionKNNMonotonicFilename);
                performKernelMonotonicAnalysis(atMost, atLeast, atMostUnion.getLimitingDecision(), atLeastUnion.getLimitingDecision(), unionVsUnionKernelMonotonicFilename);
            }
        }
    }

    private int[] unionToArray(UnionWithSingleLimitingDecision union) {
        IntSortedSet objects = union.getObjects();
        int arr[] = new int[objects.size()];
        objects.toArray(arr);
        return arr;
    }

    private void performKNearestAnalysis(int[] majority, int[] minority, Decision majorityLimitingDecision, Decision minorityLimitingDecision, String resultsKey) {
        HashMap<Integer, LearningExampleType> kNearestResults = kNearestAnalysis(majority, minority);
        resultsByName.get(resultsKey).addResults(kNearestResults, minorityLimitingDecision, majorityLimitingDecision);
    }

    private void performKNearestMonotonicAnalysis(int[] majority, int[] minority, Decision majorityLimitingDecision, Decision minorityLimitingDecision, String resultsKey) {
        HashMap<Integer, LearningExampleType> kNearestMonotonicResults = kNearestMonotonicAnalysis(majority, minority);
        resultsByName.get(resultsKey).addResults(kNearestMonotonicResults, minorityLimitingDecision, majorityLimitingDecision);
    }

    private void performKernelAnalysis(int[] majority, int[] minority, Decision majorityLimitingDecision, Decision minorityLimitingDecision, String resultsKey) {
        HashMap<Integer, LearningExampleType> kernelResults = kernelAnalysis(majority, minority, majorityLimitingDecision, minorityLimitingDecision);
        resultsByName.get(resultsKey).addResults(kernelResults, minorityLimitingDecision, majorityLimitingDecision);
    }

    private void performKernelMonotonicAnalysis(int[] majority, int[] minority, Decision majorityLimitingDecision, Decision minorityLimitingDecision, String resultsKey) {
        HashMap<Integer, LearningExampleType> kernelResults = kernelMonotonicAnalysis(majority, minority, majorityLimitingDecision, minorityLimitingDecision);
        resultsByName.get(resultsKey).addResults(kernelResults, minorityLimitingDecision, majorityLimitingDecision);
    }

    private HashMap<Integer, LearningExampleType> kNearestAnalysis(int[] majorityIndices, int[] minorityIndices) {
        KNearestLabeler labeler = new KNearestLabeler(4, 2, 1);
        KNNAnalyzer analyzer = new KNNAnalyzer(measure, majorityIndices, minorityIndices, 5, labeler);
        analyzer.labelExamples();
        return analyzer.getLabelsAssignment();
    }

    private HashMap<Integer, LearningExampleType> kNearestMonotonicAnalysis(int[] majorityIndices, int[] minorityIndices) {
        KNearestLabeler labeler = new KNearestLabeler(4, 2, 1);
        MonotonicKNNAnalyzer analyzer = new MonotonicKNNAnalyzer(measure, majorityIndices, minorityIndices, 5, labeler);
        analyzer.labelExamples();
        return analyzer.getLabelsAssignment();
    }

    private HashMap<Integer, LearningExampleType> kernelAnalysis(int[] majorityIndices, int[] minorityIndices, Decision majorityLimitingDecision, Decision minorityLimitingDecision) {
        KernelLabeler labeler = new KernelLabeler(0.7, 0.3, 0.1);
        KernelAnalyzer analyzer = new KernelAnalyzer(measure, majorityIndices, minorityIndices, majorityLimitingDecision, minorityLimitingDecision, labeler);
        analyzer.labelExamples();
        return analyzer.getLabelsAssignment();
    }

    private HashMap<Integer, LearningExampleType> kernelMonotonicAnalysis(int[] majorityIndices, int[] minorityIndices, Decision majorityLimitingDecision, Decision minorityLimitingDecision) {
        KernelLabeler labeler = new KernelLabeler(0.7, 0.3, 0.1);
        MonotonicKernelAnalyzer analyzer = new MonotonicKernelAnalyzer(measure, majorityIndices, minorityIndices, majorityLimitingDecision, minorityLimitingDecision, labeler);
        analyzer.labelExamples();
        return analyzer.getLabelsAssignment();
    }

    private void classVsUnionAnalysis(HashMap<Decision, int[]> classesByDecision, Union[] atLeastUnions, Union[] atMostUnions) {
        resultsByName.put(classVsUnionKNNFilename, new AnalysisResult());
        resultsByName.put(classVsUnionKernelFilename, new AnalysisResult());
        resultsByName.put(classVsUnionKNNMonotonicFilename, new AnalysisResult());
        resultsByName.put(classVsUnionKernelMonotonicFilename, new AnalysisResult());
        for (int i = 0; i < classesByDecision.size(); i++) {
            if (i == 0) {
                Decision classDecision = ((UnionWithSingleLimitingDecision) atMostUnions[i]).getLimitingDecision();
                int[] classIndices = classesByDecision.get(classDecision);
                performClassVsUnion(classIndices, classDecision, (UnionWithSingleLimitingDecision) atLeastUnions[i]);
            } else if (i == classesByDecision.size() - 1){
                Decision classDecision = ((UnionWithSingleLimitingDecision) atLeastUnions[i - 1]).getLimitingDecision();
                int[] classIndices = classesByDecision.get(classDecision);
                performClassVsUnion(classIndices, classDecision, (UnionWithSingleLimitingDecision) atMostUnions[i - 1]);
            } else {
                Decision classDecision = ((UnionWithSingleLimitingDecision) atLeastUnions[i - 1]).getLimitingDecision();
                int[] classIndices = classesByDecision.get(classDecision);
                performClassVsUnion(classIndices, classDecision, (UnionWithSingleLimitingDecision) atMostUnions[i - 1]);
                performClassVsUnion(classIndices, classDecision, (UnionWithSingleLimitingDecision) atLeastUnions[i]);
            }
        }
    }

    private void performClassVsUnion(int[] classIndices, Decision classDecision, UnionWithSingleLimitingDecision union) {
        int[] unionIndices = unionToArray(union);
        int[] majority = classIndices.length > unionIndices.length ? classIndices : unionIndices;
        int[] minority = classIndices.length <= unionIndices.length ? classIndices : unionIndices;
        Decision majDecision = classIndices.length > unionIndices.length ? classDecision : union.getLimitingDecision();
        Decision minDecision = classIndices.length <= unionIndices.length ? classDecision : union.getLimitingDecision();
        performKNearestAnalysis(majority, minority, majDecision, minDecision, classVsUnionKNNFilename);
        performKernelAnalysis(majority, minority, majDecision, minDecision, classVsUnionKernelFilename);
        performKNearestMonotonicAnalysis(majority, minority, majDecision, minDecision, classVsUnionKNNMonotonicFilename);
        performKernelMonotonicAnalysis(majority, minority, majDecision, minDecision, classVsUnionKernelMonotonicFilename);
    }

    private void saveResults() throws IOException {
        createDirIfNotExists();
        for (Map.Entry<String, AnalysisResult> entry: resultsByName.entrySet()) {
            AnalysisResult result = entry.getValue();
            String filename = String.format("%s%s%s.csv", resultsPath, File.separator, entry.getKey());
            result.saveCsv(filename);
        }
    }

    private void createDirIfNotExists() {
        if (Files.notExists(Paths.get(resultsPath))) {
            new File(resultsPath).mkdirs();
        }
    }

    public HashMap<String, AnalysisResult> getResultsByName() {
        return resultsByName;
    }
}
