package algorithms;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import static org.apache.commons.lang3.ObjectUtils.min;

public class LSI {
    private int valueK;
    private SingularValueDecomposition svd;
    private RealMatrix matrix;
    private RealMatrix leftSingularMatrix;
    private RealMatrix singularValueMatrix;
    private RealMatrix rightSingularMatrixTransposed;
    private RealMatrix wordsMatrix;

    public LSI(RealMatrix matrix, int valueK) {
		for(int y = 0; y < matrix.getColumnDimension(); y++) {
			for(int x = 0; x < matrix.getRowDimension(); x++) {
// 				System.out.printf("(%d,%d): %f\n", x, y, matrix.getEntry(x, y));
			}
		}
        this.valueK = valueK;
        this.matrix = matrix;
        this.svd = new SingularValueDecomposition(this.matrix);
        performSingularValueDecomposition();
        createWordRepresentation();
    }

    public RealMatrix getWordsMatrix() {
        return wordsMatrix;
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

    private void createWordRepresentation() {
        wordsMatrix = leftSingularMatrix.multiply(singularValueMatrix);
    }
}
