package data_preprocessing;

import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.List;

public class Vectorizer {

    private final Dictionary dictionary;
    private final TextPreprocessor textPreprocessor;
    private final boolean isBinary;

    public Vectorizer(Dictionary dictionary, TextPreprocessor textPreprocessor, boolean isBinary) {
        this.dictionary = dictionary;
        this.textPreprocessor = textPreprocessor;
        this.isBinary = isBinary;
    }

//    public Vectorizer() {
//        this(new HashingDictionary(), new SimpleTokenizer(), false);
//    }

    public RealVector getCountVector(String document) {
        RealVector vector = new OpenMapRealVector(dictionary.getNumTerms());
        String[] tokens = textPreprocessor.getPreparedTerms(document);
        for (String token : tokens) {
            Integer index = dictionary.getTermIndex(token);
            if(index != null) {
                if(isBinary) {
                    vector.setEntry(index, 1);
                } else {
                    vector.addToEntry(index, 1); // increment !
                }
            }
        }
        return vector;
    }

    public RealMatrix getCountMatrix(List<String> documents) {
        int rowDimension = documents.size();
        int columnDimension = dictionary.getNumTerms();
        RealMatrix matrix = new OpenMapRealMatrix(rowDimension, columnDimension);
        int counter = 0;
        for (String document : documents) {
            matrix.setRowVector(counter++, getCountVector(document));
        }
        return matrix.transpose();
    }
}