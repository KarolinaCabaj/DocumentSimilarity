package message;

import algorithms.Algorithm;

public class WorkOrderMsg {
    private String fileName;
    private Algorithm alg;

    public WorkOrderMsg(String fileName, Algorithm alg) {
        this.fileName = fileName;
        this.alg = alg;
    }

    public String getFileName() {
        return fileName;
    }

    public Algorithm getAlg() {
        return alg;
    }
}
