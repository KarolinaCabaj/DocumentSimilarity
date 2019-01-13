package postprocessing;

import java.util.ArrayList;

public class ResultEvaluator {
    private ArrayList<QualityMeasureEnum> qualityResults = new ArrayList<>();

    public void showEvaluationResults(ArrayList<Double> diff, QualityMeasureEnum measureClass){
        evaluateQuality(diff);
        int numberOfElementInClass = countQualityClassElements(measureClass);
        System.out.println(numberOfElementInClass);

        double percentage = percentage(measureClass);
        System.out.println("Greats percentage: " + percentage + "%");
    }

    private void evaluateQuality(ArrayList<Double> differences) {
        for (Double diff : differences) {
            qualityResults.add(QualityMeasureEnum.getQualityMeasure(diff));
        }
    }
    private double percentage(QualityMeasureEnum qualityClass){
        long numberOf = countQualityClassElements(qualityClass);
        return (double)numberOf / qualityResults.size() * 100;
    }

    private int countQualityClassElements(QualityMeasureEnum qualityClass) {
        return (int)qualityResults.stream()
                .filter(result -> result == qualityClass)
                .count();
    }
}
