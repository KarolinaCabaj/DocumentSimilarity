package data_preprocessing;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

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

    public RealMatrix getCountMatrix(List<RealVector> documentVectors) {
        int columnDimension = listOfDocumentsTerms.size();
        int rowDimension = terms.size();
        RealMatrix matrix = new Array2DRowRealMatrix(rowDimension, columnDimension);
        for (int i = 0; i < documentVectors.size(); i++) {
            matrix.setColumnVector(i, documentVectors.get(i));
        }
        return matrix;
    }

}