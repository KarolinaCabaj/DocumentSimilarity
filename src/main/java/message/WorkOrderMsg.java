package message;

import java.util.List;

public class WorkOrderMsg {
    private String doc;
    private WorkType workType;
    private List<String> terms;
    private int numberOfTopics;
    private int numberOfSyncs;

    public WorkOrderMsg(String doc, WorkType workType, List<String> terms) {
        this(doc, workType, terms, 0, 0);
    }
    public WorkOrderMsg(String doc, WorkType workType){
        this(doc, workType, null, 0, 0);
    }
    public WorkOrderMsg(String doc, WorkType workType, List<String> terms, int numberOfTopics, int numberOfSyncs) {
        this.doc = doc;
        this.workType = workType;
        this.terms = terms;
        this.numberOfSyncs = numberOfSyncs;
        this.numberOfTopics = numberOfTopics;
        //przy 0 ustaw domyślną wartość
        if(this.numberOfTopics == 0)
        {
			this.numberOfTopics = 20;
        }
        if(this.numberOfSyncs == 0)
        {
			this.numberOfSyncs = 100;
        }
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
    
    public int getNumberOfTopics()
    {
		return numberOfTopics;
    }
    
    public int getNumberOfSyncs()
    {
		return numberOfSyncs;
    }

    public enum WorkType {
        LSI, LDA
    }
}
