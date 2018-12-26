package algorithms;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import static java.lang.Math.min;

public class LSI {
    private int valueK;
    private SingularValueDecomposition svd;
    private RealMatrix matrix;
    private RealMatrix matrixU;
    private RealMatrix matrixS;
    private RealMatrix matrixTransposedT;

    public LSI(RealMatrix matrix, int valueK) {
        this.valueK = valueK;
        this.matrix = matrix;
        this.svd = new SingularValueDecomposition(this.matrix);
    }

    public void assign(){
        matrixU = svd.getU();
        matrixS = svd.getS();
        matrixTransposedT = svd.getVT();
    }

//    public RealMatrix reduceCovaranceMatrix(){
//        Integer rows = matrixS.getRowDimension();
//        Integer cols = matrixS.getColumnDimension();
//        Integer minSize = min(rows, cols);
//    }
}
