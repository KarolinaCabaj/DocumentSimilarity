package algorithms;

import java.util.*;

public class LDA {

	private class WordPair {
		public int wordId;
		public int themeId;
		
		public WordPair(int wordId, int themeId) {
			this.wordId = wordId;
			this.themeId = themeId;
		}
	}
	
	/** Dokumenty, każdy to lista par identyfikatora słowa i tematu */
	List<List<WordPair>> documents;
	/** Tablica Słowa ↔ Tematy, liczebności danego słowa w danym temacie */
	private List<int[]> wordThemesTable;
	/** Tablica Dokumenty ↔ Tematy, liczebności słów o danych tematach w dokumencie */
	private List<int[]> documentThemesTable;
	/** Sumy słów w danym temacie */
	private int[] themesSums;
	/** Sumy słów w danym dokumencie, minimalnie zmienna */
	private int[] documentsSums;
	/** Ilość tematów w dokumencie */
	private final int themesPerDocument;
	
	/** Ustaw temat słowa w dokumencie, słowo musi być wcześniej zdetematowane */
	private void markWordWithTheme(int documentIndex, int wordIndex, int themeId)
	{
		//ustaw w głównej tablicy
		List<WordPair> document = documents.get(documentIndex);
		WordPair record = document.get(wordIndex);
		record.themeId = themeId;
		int wordId = record.wordId;
		
		//zmodyfikuj tabele i sumy
		wordThemesTable.get(wordId)[themeId] += 1;
		themesSums[themeId] += 1;
		documentThemesTable.get(documentIndex)[themeId] += 1;
		documentsSums[documentIndex] += 1;
	}

	/** Przeprowadź algorytm LDA 
	 *	@arg documentsHistograms Lista tablic, każdy wpis w liście to jeden dokument, tablica określa ilości słów o tym indeksie 
	 *	@arg themesPerDocument Parametr ilości tematów per dokument, jakie ma znaleźć 
	 *	@arg algorithmSteps Ilość powtórzeń */
    public LDA(final List<int[]> documentsHistograms, final int themesPerDocument, final int algorithmSteps) 
    {
		this.themesPerDocument = themesPerDocument;
		//sprawdź czy każda tablica zawiera taką samą ilość wartości
		final int histogramValuesCount = documentsHistograms.get(0).length;
		final int documentsCount = documentsHistograms.size();
		for(int[] histogram : documentsHistograms) 
		{
			assert(histogram.length == histogramValuesCount);
		}
    
		System.out.printf("Algorytm LDA\nDokumentów: %d\nIlość danych histogramu: %d\n", documentsCount, histogramValuesCount);
		
		//inicjalizuj dwie tabele operacyjne i dwa wektory sum
		this.wordThemesTable = new ArrayList<>();
		for(int i = 0; i < histogramValuesCount; i++)
		{
			this.wordThemesTable.add(new int[themesPerDocument]);
		}
		this.documentThemesTable = new ArrayList<>();
		for(int i = 0; i < documentsCount; i++)
		{
			this.documentThemesTable.add(new int[themesPerDocument]);
		}
		this.themesSums = new int[themesPerDocument];
		this.documentsSums = new int[documentsCount];
		
		//przepisz argumenty na tablicę i wypełnij tablice operacyjne
		
		//dla każdego dokumentu
		this.documents = new ArrayList<>();
		for(int documentIndex = 0; documentIndex < documentsHistograms.size(); documentIndex++) 
		{
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
					Random random = new Random();
					int themeId = random.nextInt(themesPerDocument);
					document.add(new WordPair(wordId, themeId));
					markWordWithTheme(documentIndex, wordIndex, themeId);
					wordIndex++;
				}
			}
		}
		
		//DEBUG wydrukuj zmienne
// 		for(List<WordPair> document : documents) 
// 		{
// 			System.out.printf("Dokument\n");
// 			for(WordPair word : document) 
// 			{
// 				System.out.printf("Słowo: %d → temat %d\n", word.wordId, word.themeId);
// 			}
// 		}
		
		printDocumentThemesTable();
		printWordThemesTable();

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
				
			}
		}
    }
    
    /** Wydrukuj tabelę tematów w dokumentach */
    private void printDocumentThemesTable()
    {
		System.out.printf("     Tematy / ");
		for(int themeId = 0; themeId < themesPerDocument; themeId++)
		{
			System.out.printf("%3d     ", themeId);
		}
		System.out.printf("\n");
		for(int documentIndex = 0; documentIndex < documentThemesTable.size(); documentIndex++)
		{
			System.out.printf("Dokument %2d | ", documentIndex);
			for(int themeId = 0; themeId < themesPerDocument; themeId++)
			{
				System.out.printf("%5d | ", documentThemesTable.get(documentIndex)[themeId]);
			}
			System.out.printf("Suma> %5d\n", documentsSums[documentIndex]);
		}
    }
    
    /** Wydrukuj tabelę słów w dokumentach */
    private void printWordThemesTable()
    {
		System.out.printf("     Tematy / ");
		for(int themeId = 0; themeId < themesPerDocument; themeId++)
		{
			System.out.printf("%3d     ", themeId);
		}
		System.out.printf("\n");
		for(int wordId = 0; wordId < wordThemesTable.size(); wordId++)
		{
			System.out.printf("Słowo %5d | ", wordId);
			for(int themeId = 0; themeId < themesPerDocument; themeId++)
			{
				System.out.printf("%5d | ", wordThemesTable.get(wordId)[themeId]);
			}
			System.out.printf("\n");
		}
		System.out.printf("Sumy        \\ ");
		for(int themeId = 0; themeId < themesPerDocument; themeId++)
		{
			System.out.printf("%5d   ", themesSums[themeId]);
		}
		System.out.printf("\n");
    }
}
