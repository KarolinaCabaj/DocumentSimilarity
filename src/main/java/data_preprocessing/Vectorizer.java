package data_preprocessing;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.*;

public class Vectorizer {
    private ArrayList<String> terms;

    private List<String[]> listOfDocumentsTerms;

    public Vectorizer(List<String[]> listOfDocumentsTerms) {
        this.listOfDocumentsTerms = listOfDocumentsTerms;
        createSetOfAllTerms();
    }

    public ArrayList<String> getTerms() {
        return terms;
    }

    private void createSetOfAllTerms() {
        TreeSet<String> termsSet = new TreeSet<>();
        for (String[] documentTerms : listOfDocumentsTerms) {
            for (String term : documentTerms) {
                if (!term.equals("O"))
                    termsSet.add(term);
            }
        }
        terms = new ArrayList<>();
        terms.addAll(termsSet);
    }

    public RealVector createOccurrenceVector(String[] tokens) {
        RealVector vec = new ArrayRealVector(terms.size());
        TermDictionary termDictionary = new TermDictionary(tokens);
        HashMap<String, Integer> termsOccurrences = termDictionary.getOccurrencesOfTerms();

        for (int i = 0; i < terms.size(); i++) {
            if (termsOccurrences.containsKey(terms.get(i))) {
                String term = terms.get(i);
                vec.addToEntry(i, termsOccurrences.get(term));
            } else {
                vec.addToEntry(i, 0);
            }
        }
        return vec;
    }

    public RealMatrix getCountMatrix(List<RealVector> documentVectors) {
        int columnDimension = listOfDocumentsTerms.size();
        int rowDimension = terms.size();
        RealMatrix matrix = new Array2DRowRealMatrix(rowDimension, columnDimension);
        for (int i =0; i < documentVectors.size(); i++){
            matrix.setColumnVector(i, documentVectors.get(i));
        }
        return matrix;
    }

}