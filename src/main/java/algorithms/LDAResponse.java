package algorithms;
import java.util.*;

/** Zawiera odpowiedzi algorytmu LDA, dzięki czemu może być użyty do porównywania semantycznego wyrazów */
public class LDAResponse
{
	final private int histogramValuesCount;
	final private int topicsPerDocument;
	final private double[] topicsSums;
	final private List<double[]> wordTopicsTable;

	LDAResponse(int histogramValuesCount, int topicsPerDocument, double[] topicsSums, List<double[]> wordTopicsTable)
	{
		this.histogramValuesCount = histogramValuesCount;
		this.topicsPerDocument = topicsPerDocument;
		this.topicsSums = topicsSums;
		this.wordTopicsTable = wordTopicsTable;
	}

	/** Podobieństwo semantyczne dwóch słów */
    public double calculateSimilarity(int wordId1, int wordId2)
    {
		if(wordId1 >= histogramValuesCount || wordId2 >= histogramValuesCount || wordId1 == -1 || wordId2 == -1)
		{
			return 0;
		}
		double importanceSum = 0;
		for(int topicId = 0; topicId < topicsPerDocument; topicId++)
		{
			double wordsSum = topicsSums[topicId];
			double word1Dist = wordTopicsTable.get(wordId1)[topicId] / wordsSum;
			double word2Dist = wordTopicsTable.get(wordId2)[topicId] / wordsSum;
			importanceSum += word1Dist * word2Dist;
		}
		return importanceSum;
    }
}
