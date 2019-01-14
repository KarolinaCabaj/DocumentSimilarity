package postprocessing;

import data_preprocessing.TestingWordsConverter;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.List;

public class ResultEvaluator {
    private ArrayList<QualityMeasureEnum> qualityResults = new ArrayList<>();
    private ArrayList<Double> differences;
    private List<String> terms;
    private RealMatrix wordsMatrix;


    public ResultEvaluator(List<String> terms, RealMatrix wordsMatrix) {
        this.terms = terms;
        this.wordsMatrix = wordsMatrix;
        TestingWordsConverter testingWordsConverter = new TestingWordsConverter();
        List<String[]> testingData = testingWordsConverter.getParsedTestingData();
        this.differences = getDifferences(testingData);
        evaluateQuality();
    }

    private ArrayList<Double> getDifferences(List<String[]> testingData) {
        differences = new ArrayList<>();
        for (String[] data : testingData) {
            double similarity = getCosineSimilarity(data[0], data[1]);
            if (similarity != -1) {
                Double difference = computeDifference(similarity, data[2]);
                differences.add(difference);
            }
        }
        return differences;
    }

    private Double computeDifference(double computedValue, String testingValue) {
        Double testing = Double.parseDouble(testingValue);
        return Math.abs(computedValue - testing);
    }

    private double getCosineSimilarity(String firstTerm, String secondTerm) {
        if (matrixContainsTerms(firstTerm, secondTerm)) {
            return computeCosineSimilarity(firstTerm, secondTerm);
        } else
            return -1;
    }

    private boolean matrixContainsTerms(String firstTerm, String secondTerm) {
        return terms.contains(firstTerm) && terms.contains(secondTerm);
    }

    private double computeCosineSimilarity(String firstTerm, String secondTerm) {
        RealVector firstTermVector = getVector(firstTerm);
        RealVector secondTermVector = getVector(secondTerm);
        return firstTermVector.cosine(secondTermVector) * 10;
    }

    private RealVector getVector(String term) {
        int termIndex = terms.indexOf(term);
        return wordsMatrix.getRowVector(termIndex);
    }

    public void showEvaluationResults(QualityMeasureEnum measureClass) {

        int numberOfElementInClass = countQualityClassElements(measureClass);
        System.out.println(measureClass.toString() + " contains " + numberOfElementInClass + " elements.");

        double percentage = percentage(measureClass);
        System.out.println(measureClass.toString() + " percentage: " + percentage + "%");
    }

    private void evaluateQuality() {
        for (Double diff : differences) {
            qualityResults.add(QualityMeasureEnum.getQualityMeasure(diff));
        }
    }

    private double percentage(QualityMeasureEnum qualityClass) {
        long numberOf = countQualityClassElements(qualityClass);
        return (double) numberOf / qualityResults.size() * 100;
    }

    private int countQualityClassElements(QualityMeasureEnum qualityClass) {
        return (int) qualityResults.stream()
                .filter(result -> result == qualityClass)
                .count();
    }

    public void showAverage() {
        Double average = differences.stream().mapToDouble(val -> val).average().orElse(0.0);
        System.out.println("Average : " + average);
    }
}
