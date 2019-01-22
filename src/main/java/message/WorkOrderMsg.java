package message;

import java.util.List;

public class WorkOrderMsg {
    private String doc;
    private WorkType workType;
    private List<String> terms;
    private int numberOfTopics;
    private int numberOfSyncs;
	private List<int[]> histograms;
	private List<int[]> textOfIds;
	private List<double[]> wordTopicsTable;
	private double[] topicsSums;

    public WorkOrderMsg(String doc, WorkType workType, List<String> terms) {
        this(doc, workType, terms, 0, 0, null);
    }
    public WorkOrderMsg(String doc, WorkType workType){
        this(doc, workType, null, 0, 0, null);
    }
    public WorkOrderMsg(int numberOfTopics, List<int[]> textOfIds, List<double[]> wordTopicsTable, double[] topicsSums) {
		this(null, WorkType.LDA, null, numberOfTopics, 0, null);
		this.textOfIds = textOfIds;
		this.wordTopicsTable = wordTopicsTable;
		this.topicsSums = topicsSums;
    }
    public WorkOrderMsg(String doc, WorkType workType, List<String> terms, int numberOfTopics, int numberOfSyncs, List<int[]> histograms) {
        this.doc = doc;
        this.workType = workType;
        this.terms = terms;
        this.numberOfSyncs = numberOfSyncs;
        this.numberOfTopics = numberOfTopics;
        this.histograms = histograms;
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
    
    public List<double[]> getTopicTable()
    {
		return wordTopicsTable;
    }
    
    public double[] getTopicSums()
    {
		return topicsSums;
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
    
    public List<int[]> getHistograms()
    {
		return histograms;
    }
    
    public List<int[]> getTextOfIds()
    {
		return textOfIds;
    }

    public enum WorkType {
        LSI, LDA
    }
}
