package data_preprocessing;

import java.util.HashMap;
import java.util.Map;

public class TermDictionary implements Dictionary {

    private final HashMap<String, Integer> occurrencesOfTerms;
    private final String[] terms;

    TermDictionary(String[] terms) {
        this.terms = terms;
        occurrencesOfTerms = new HashMap<String, Integer>();
        addTerms();
    }

    HashMap<String, Integer> getOccurrencesOfTerms() {
        return occurrencesOfTerms;
    }

    private void addTerm(String term) {
        if (occurrencesOfTerms.containsKey(term)) {
            occurrencesOfTerms.put(term, occurrencesOfTerms.get(term) + 1);
        } else {
            occurrencesOfTerms.put(term, 1);
        }
    }

    private void addTerms() {
        for (String term : terms) {
            addTerm(term);
        }
    }

    @Override
    public Integer getTermOccurrences(String term) {
        return occurrencesOfTerms.get(term);
    }

    @Override
    public int getNumberOfTerms() {
        return occurrencesOfTerms.size();
    }
}