package postprocessing;

import java.util.HashMap;
import java.util.Map;

public class ResultMeasure {
    private Map<String, Float> semanticRelTemplates;
    private Map<String, Float> semanticRelGiven;
    private Map<String, QualityMeasure> resultEvaluation;

    public ResultMeasure(Map<String, Float> semanticRelTemplate, Map<String, Float> semanticRelGiven) {
        this.semanticRelTemplates = semanticRelTemplate;
        this.semanticRelGiven = semanticRelGiven;
        resultEvaluation = new HashMap<>();
    }

    private void evaluateResultEvaluation() {
        for (String key : semanticRelGiven.keySet()) {
            if (semanticRelTemplates.containsKey(key)) {
                Float template = semanticRelTemplates.get(key);
                Float given = semanticRelGiven.get(key);
                Float absValue = Math.abs(template - given);
                resultEvaluation.put(key, QualityMeasure.getQualityMeasure(absValue));
            }
        }
    }

    public Map<String, QualityMeasure> getResultEvaluation() {
        return resultEvaluation;
    }

    public void printResult() {
        System.out.println(resultEvaluation);
    }
}
