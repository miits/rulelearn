package org.ordinalclassification.utils;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

public class JsonCsvDatasetIterator {
    private String jsonPath;
    private String csvPath;
    private String resultsPath;
    private int k;
    private String measure;

    public JsonCsvDatasetIterator(String jsonPath, String csvPath, String resultsPath, int k, String measure) {
        this.jsonPath = jsonPath;
        this.csvPath = csvPath;
        this.resultsPath = resultsPath;
        this.k = k;
        this.measure = measure;
    }

    public void iterate(DatasetOperation operation) {
        File[] jsonFiles = getFiles(this.jsonPath);
        File[] csvFiles = getFiles(this.csvPath);
        Iterator<File> jsonIterator = Arrays.stream(jsonFiles).sorted().iterator();
        Iterator<File> csvIterator = Arrays.stream(csvFiles).sorted().iterator();
        int datasetsCount = jsonFiles.length;
        int counter = 0;
        while (jsonIterator.hasNext() && csvIterator.hasNext()) {
            File json = jsonIterator.next();
            File csv = csvIterator.next();
            String datasetName = FilenameUtils.removeExtension(json.getName());
            String resultsPath = String.format("%s%s%s", this.resultsPath, File.separator, datasetName);
            String[] args = {json.getPath(), csv.getPath(), resultsPath, String.valueOf(this.k), this.measure};
            System.out.println(String.format("[JsonCsvDatasetIterator] Processing dataset: %s (%d/%d)", datasetName, ++counter, datasetsCount));
            operation.carryOut(args);
        }
    }

    private File[] getFiles(String directoryPath) {
        File file = new File(directoryPath);
        File[] files = null;
        if (file.isDirectory()) {
            files = file.listFiles();
        } else if (file.isFile()) {
            files = new File[] {file};
        }
        return files;
    }
}
