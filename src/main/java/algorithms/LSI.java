package algorithms;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.min;

public class LSI implements Algorithm {
    private int valueK;
    private ArrayList<String> terms;
    private SingularValueDecomposition svd;
    private RealMatrix matrix;
    private RealMatrix leftSingularMatrix;
    private RealMatrix singularValueMatrix;
    private RealMatrix rightSingularMatrixTransposed;
    private RealMatrix wordsVectors;

    public LSI(RealMatrix matrix, int valueK, ArrayList<String> terms) {
        this.valueK = valueK;
        this.terms = terms;
        this.matrix = matrix;
        this.svd = new SingularValueDecomposition(this.matrix);
    }

    //todo usunać na koniec - na potrzeby działania szkieletu aktorów
    public LSI() {
    }

    public void performSingularValueDecomposition() {
        leftSingularMatrix = svd.getU();
        singularValueMatrix = svd.getS();
        rightSingularMatrixTransposed = svd.getVT();

        if (valueK < matrix.getColumnDimension() && valueK < matrix.getRowDimension()) {
            reduceMatrices();
        } else {
            valueK = min(matrix.getColumnDimension(), matrix.getRowDimension());
            reduceMatrices();
        }
    }

    @Override
    public void run() {

    }

    private void reduceMatrices() {
        for (int r = 0; r < leftSingularMatrix.getRowDimension(); r++) {
            for (int c = valueK; c < leftSingularMatrix.getColumnDimension(); c++)
                leftSingularMatrix.setEntry(r, c, 0);
        }

        for (int r = 0; r < singularValueMatrix.getRowDimension(); r++) {
            for (int c = valueK; c < singularValueMatrix.getColumnDimension(); c++)
                singularValueMatrix.setEntry(r, c, 0);
        }

        for (int r = valueK; r < rightSingularMatrixTransposed.getRowDimension(); r++) {
            for (int c = 0; c < rightSingularMatrixTransposed.getColumnDimension(); c++)
                rightSingularMatrixTransposed.setEntry(r, c, 0);
        }
    }

    public void wordRepresentation() {
        wordsVectors = leftSingularMatrix.multiply(singularValueMatrix);
    }

    public ArrayList<Double> getDifferences(List<String[]> testingData){
        ArrayList<Double> differences = new ArrayList<>();
        for(String[] data: testingData){
            double similarity = getCosineSimilarity(data[0], data[1]);
            if(similarity != -1)
            {
                Double difference = computeDifference(similarity, data[2]);
                differences.add(difference);
            }
        }
        return differences;
    }

    private Double computeDifference(double computedValue, String testingValue){
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
        return wordsVectors.getRowVector(termIndex);
    }
}
