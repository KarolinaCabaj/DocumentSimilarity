package message;

public class WorkOrderMsg {
    private String fileName;
    private String alg;

    public WorkOrderMsg(String fileName, String alg) {
        this.fileName = fileName;
        this.alg = alg;
    }

    public String getFileName() {
        return fileName;
    }

    public String getAlg() {
        return alg;
    }
}
