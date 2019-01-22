package message;
import algorithms.LDAResponse;
import java.util.*;

/** Wiadomość z wynikami z algorytmu LDA */
public class FinishMsg
{
	private List<int[]> bestWords;
	private LDAResponse ldaResponse;

	public FinishMsg(List<int[]> bestWords, LDAResponse ldaResponse)
	{
		this.bestWords = bestWords;
		this.ldaResponse = ldaResponse;
	}

	public List<int[]> getBestWords()
	{
		return bestWords;
	}

	public LDAResponse getLdaResponse()
	{
		return ldaResponse;
	}
}
