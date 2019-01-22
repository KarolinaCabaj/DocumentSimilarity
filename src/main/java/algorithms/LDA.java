package algorithms;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import java.util.*;

public class LDA {

	private class WordPair {
		public int wordId;
		public int topicId;
		
		public WordPair(int wordId, int topicId) {
			this.wordId = wordId;
			this.topicId = topicId;
		}
	}
	
	private class WordProbabilityPair implements Comparable<WordProbabilityPair> {
		public double probability;
		public int wordId;
		
		public WordProbabilityPair(double probability, int wordId) {
			this.probability = probability;
			this.wordId = wordId;
		}
		
		public int compareTo(WordProbabilityPair other)
		{
			if(other.probability > probability)
			{
				return 1;
			}
			else if(other.probability < probability)
			{
				return -1;
			}
			else
			{
				//to się prawie nigdy nie zdarzy przy doublach
				return 0;
			}
		}
	
	}
	
	/** Dokumenty, każdy to lista par identyfikatora słowa i tematu */
	List<List<WordPair>> documents;
	/** Tablica Słowa ↔ Tematy, liczebności danego słowa w danym temacie */
	private List<int[]> wordTopicsTable;
	/** Tablica Dokumenty ↔ Tematy, liczebności słów o danych tematach w dokumencie */
	private List<int[]> documentTopicsTable;
	/** Sumy słów w danym temacie */
	private int[] topicsSums;
	/** Sumy słów w danym dokumencie, minimalnie zmienna */
	private int[] documentsSums;
	/** Ilość tematów w dokumencie */
	private final int topicsPerDocument;
	/** Ilość obsługiwanych różnych słów */
	private final int histogramValuesCount;
	
	/** Ustaw temat słowa w dokumencie, słowo musi być wcześniej zdetematowane */
	private void markWordWithTopic(int documentIndex, int wordIndex, int topicId)
	{
		//ustaw w głównej tablicy
		List<WordPair> document = documents.get(documentIndex);
		WordPair record = document.get(wordIndex);
		record.topicId = topicId;
		int wordId = record.wordId;
		
		//zmodyfikuj tabele i sumy
		wordTopicsTable.get(wordId)[topicId] += 1;
		topicsSums[topicId] += 1;
		documentTopicsTable.get(documentIndex)[topicId] += 1;
		documentsSums[documentIndex] += 1;
	}
	
	/** Odepnij temat od dokumentu */
	private void unmarkWord(int documentIndex, int wordIndex)
	{
		//znajdź słowo
		List<WordPair> document = documents.get(documentIndex);
		WordPair record = document.get(wordIndex);
		int topicId = record.topicId;
		int wordId = record.wordId;
		record.topicId = -1;
		
		//zmodyfikuj tabele i sumy
		wordTopicsTable.get(wordId)[topicId] -= 1;
		topicsSums[topicId] -= 1;
		documentTopicsTable.get(documentIndex)[topicId] -= 1;
		documentsSums[documentIndex] -= 1;
	}

	/** Przeprowadź algorytm LDA 
	 *	@arg documentsHistograms Lista tablic, każdy wpis w liście to jeden dokument, tablica określa ilości słów o tym indeksie 
	 *	@arg topicsPerDocument Parametr ilości tematów per dokument, jakie ma znaleźć 
	 *	@arg algorithmSteps Ilość powtórzeń */
    public LDA(final List<int[]> documentsHistograms, final int topicsPerDocument, final int algorithmSteps) 
    {
		this.topicsPerDocument = topicsPerDocument;
		//sprawdź czy każda tablica zawiera taką samą ilość wartości
		this.histogramValuesCount = documentsHistograms.get(0).length;
		final int documentsCount = documentsHistograms.size();
		for(int[] histogram : documentsHistograms) 
		{
			assert(histogram.length == histogramValuesCount);
		}
    
		System.out.printf("Algorytm LDA\nDokumentów: %d\nIlość danych histogramu: %d\n", documentsCount, histogramValuesCount);
		
		//inicjalizuj dwie tabele operacyjne i dwa wektory sum
		this.wordTopicsTable = new ArrayList<>();
		for(int i = 0; i < histogramValuesCount; i++)
		{
			this.wordTopicsTable.add(new int[topicsPerDocument]);
		}
		this.documentTopicsTable = new ArrayList<>();
		for(int i = 0; i < documentsCount; i++)
		{
			this.documentTopicsTable.add(new int[topicsPerDocument]);
		}
		this.topicsSums = new int[topicsPerDocument];
		this.documentsSums = new int[documentsCount];
		
		//przepisz argumenty na tablicę i wypełnij tablice operacyjne
		
		//dla każdego dokumentu
		this.documents = new ArrayList<>();
		for(int documentIndex = 0; documentIndex < documentsHistograms.size(); documentIndex++) 
		{
			Random random = new Random(documentIndex);
			//dla każdego słowa w dokumencie
			int[] histogram = documentsHistograms.get(documentIndex);
			List<WordPair> document = new ArrayList<>();
			documents.add(document);
			int wordIndex = 0;
			for(int wordId = 0; wordId < histogram.length; wordId++)
			{
				int histogramValue = histogram[wordId];
				//dodaj słowa do dokumentu w liczbie z histogramu, zaczynając z losowym tematem
				for(int i = 0; i < histogramValue; i++) 
				{
					int topicId = random.nextInt(topicsPerDocument);
					document.add(new WordPair(wordId, topicId));
					markWordWithTopic(documentIndex, wordIndex, topicId);
					wordIndex++;
				}
			}
		}
		
		for(int i = 0; i < algorithmSteps; i++)
		{
			stepOnce();
		}
		
// 		printDocumentTopicsTable();
// 		printWordTopicsTable(null);

    }
    
    /** Przelicz tematy na wszystkich dokumentach */
    public void stepOnce()
    {
		//dla każdego dokumentu
		for(int documentId = 0; documentId < documents.size(); documentId++)
		{
			//dla każdego słowa
			List<WordPair> document = documents.get(documentId);
			for(int wordIndex = 0; wordIndex < document.size(); wordIndex++)
			{
				int wordId = document.get(wordIndex).wordId;
				int oldTopicId = document.get(wordIndex).topicId;
			
				//usuń klasę słowu
				unmarkWord(documentId, wordIndex);
				
				//oblicz prawdopodobieństwa tematów dla tego słowa w tym dokumencie
				double[] probabilities = new double[topicsPerDocument];
				double probabilitySum = 0;
				for(int topicId = 0; topicId < topicsPerDocument; topicId++)
				{
					double topicDocumentProbability = getTopicInDocumentProbability(documentId, topicId);
					double wordTopicProbability = getWordInTopicProbability(wordId, topicId);
					probabilities[topicId] = topicDocumentProbability * wordTopicProbability;
					probabilitySum += probabilities[topicId];
				}
				
				//wylosuj nowy temat na podstawie tych prawdopodobieństw
				int newTopicId = oldTopicId;
				Random random = new Random();
				double probabilityPoint = random.nextDouble() * probabilitySum;
				double skippedSum = 0;
				//dodajemy kolejne wartości z tablicy, aż przekroczymy wylosowaną wartość
				for(int topicId = 0; topicId < topicsPerDocument; topicId++)
				{
// 					System.out.printf("Punkt %f, ściana %f, granica %f\n", probabilityPoint, skippedSum, probabilitySum);
					skippedSum += probabilities[topicId];
					if(probabilityPoint < skippedSum)
					{
						newTopicId = topicId;
						break;
					}
				}
				
				//ustaw nowy temat
				markWordWithTopic(documentId, wordIndex, newTopicId);
				
// 				System.out.printf("Przydzielenie: %d → %d\n", oldTopicId, newTopicId);
			}
		}
    }
    
    /** Współczynnik dotyczenia dokumentu na dany temat */
    private double getTopicInDocumentProbability(int documentId, int topicId)
    {
		//liczba słów na dany temat w dokumencie
		//dzielona przez liczność wszystkich słów w dokumencie
		double topicWordsInDocument = documentTopicsTable.get(documentId)[topicId];
		double allWordsInDocument = documentsSums[documentId];
		return (topicWordsInDocument / allWordsInDocument);
    }
    
    /** Prawdopodobieństwo, że dane słowo dotyczny danego tematu */
    private double getWordInTopicProbability(int wordId, int topicId)
    {
		//liczba przyporządkowań słowa do tematu
		//podzielić przez liczność słów w temacie
		double wordsWithTopic = wordTopicsTable.get(wordId)[topicId];
		double topicSize = topicsSums[topicId];
		return (wordsWithTopic / topicSize);
    }
    
    /** Wydrukuj tabelę tematów w dokumentach */
    private void printDocumentTopicsTable()
    {
		System.out.printf("     Tematy / ");
		for(int topicId = 0; topicId < topicsPerDocument; topicId++)
		{
			System.out.printf("%3d     ", topicId);
		}
		System.out.printf("\n");
		for(int documentIndex = 0; documentIndex < documentTopicsTable.size(); documentIndex++)
		{
			System.out.printf("Dokument %2d | ", documentIndex);
			for(int topicId = 0; topicId < topicsPerDocument; topicId++)
			{
				System.out.printf("%5d | ", documentTopicsTable.get(documentIndex)[topicId]);
			}
			System.out.printf("Suma> %5d\n", documentsSums[documentIndex]);
		}
    }
    
    /** Wydrukuj tabelę słów w tematach, opcjonalnie ze słowami */
    public void printWordTopicsTable(List<String> terms)
    {
		System.out.printf("     Tematy / ");
		for(int topicId = 0; topicId < topicsPerDocument; topicId++)
		{
			System.out.printf("%3d     ", topicId);
		}
		System.out.printf("\n");
		for(int wordId = 0; wordId < wordTopicsTable.size(); wordId++)
		{
			System.out.printf("Słowo %5d | ", wordId);
			for(int topicId = 0; topicId < topicsPerDocument; topicId++)
			{
				System.out.printf("%5d | ", wordTopicsTable.get(wordId)[topicId]);
			}
			if(terms != null)
			{
				System.out.printf("%s", terms.get(wordId));
			}
			System.out.printf("\n");
		}
		System.out.printf("Sumy        \\ ");
		for(int topicId = 0; topicId < topicsPerDocument; topicId++)
		{
			System.out.printf("%5d   ", topicsSums[topicId]);
		}
		System.out.printf("\n");
    }
    
    /** Oblicz najlepsze słowa w każdym temacie */
    public List<int[]> getBestWordsInTopic()
    {
		List<int[]> response = new ArrayList<>();
		for(int topicId = 0; topicId < topicsPerDocument; topicId++)
		{
			response.add(new int[histogramValuesCount]); 
		}
		
		//wsadź prawdopodobieństwa do drzew
		List<Set<WordProbabilityPair>> setsInTopics = new ArrayList<>();
		for(int topicId = 0; topicId < topicsPerDocument; topicId++)
		{
			Set<WordProbabilityPair> set = new TreeSet<>();
			final double wordsInTopic = topicsSums[topicId];
			for(int wordId = 0; wordId < histogramValuesCount; wordId++)
			{
				double wordProbability = wordTopicsTable.get(wordId)[topicId] / wordsInTopic;
				set.add(new WordProbabilityPair(wordProbability, wordId));
			}
			setsInTopics.add(set);
		}
		//odczytaj
		for(int topicId = 0; topicId < topicsPerDocument; topicId++)
		{
			int index = 0;
			for(WordProbabilityPair pair : setsInTopics.get(topicId))
			{
				response.get(topicId)[index] = pair.wordId;
				index++;
			}
		}
		
		return response;
    }
    
    /** Macierz używana do porównywania, w wierszach są tematy, w kolumnach słowa */
    public RealMatrix getRealMatrix()
    {
		RealMatrix response = new Array2DRowRealMatrix(histogramValuesCount, topicsPerDocument);
		for(int topicId = 0; topicId < topicsPerDocument; topicId++)
		{
			for(int wordId = 0; wordId < histogramValuesCount; wordId++)
			{
				response.setEntry(wordId, topicId, (double)wordTopicsTable.get(wordId)[topicId] / (double)topicsSums[topicId]);
			}
		}
		
		return response;
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
