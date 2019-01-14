package message;

import java.util.List;

public class WorkOrderMsg {
    private String doc;
    private WorkType workType;
    private List<String> terms;

    public WorkOrderMsg(String doc, WorkType workType, List<String> terms) {
        this.doc = doc;
        this.workType = workType;
        this.terms = terms;
    }
    public WorkOrderMsg(String doc, WorkType workType){
        this.doc = doc;
        this.workType = workType;
    }

    public String getDoc() {
        return doc;
    }

    public WorkType getWorkType() {
        return workType;
    }

    public List<String> getTerms() {
        return terms;
    }

    public enum WorkType {
        LSI, LDA
    }
}
