package algorithms;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

public class LSI implements Algorithm {
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

    //todo usunać na koniec - na potrzeby działania szkieletu aktorów
    public LSI(){}

    public void assign() {
        matrixU = svd.getU();
        matrixS = svd.getS();
        matrixTransposedT = svd.getVT();
    }

    @Override
    public void run() {

    }

//    public RealMatrix reduceCovaranceMatrix(){
//        Integer rows = matrixS.getRowDimension();
//        Integer cols = matrixS.getColumnDimension();
//        Integer minSize = min(rows, cols);
//    }
}
