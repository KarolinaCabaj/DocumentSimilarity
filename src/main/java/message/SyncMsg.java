package message;
import java.util.*;
public class SyncMsg
{
	/** Tablica słów ze współczynnikami liczności w tematach */
	final private Map<Integer, double[]> wordTopicsTable;
	/** Sumy słów w tematach */
	final private double[] topicsSums;
	/** Jest rozpoczynająca */
	final private boolean isEmpty;
	
	public SyncMsg()
	{
		this.isEmpty = true;
		topicsSums = null;
		wordTopicsTable = null;
	}
	
	public SyncMsg(Map<Integer, double[]> wordTopicsTable, double[] topicsSums)
	{
		this.wordTopicsTable = wordTopicsTable;
		this.topicsSums = topicsSums;
		this.isEmpty = false;
	}
	
	public Map<Integer, double[]> getWordTopicsTable()
	{
		return wordTopicsTable;
	}
	
	public double[] getTopicSums()
	{
		return topicsSums;
	}
	
	public boolean isEmpty()
	{
		return isEmpty;
	}

}
