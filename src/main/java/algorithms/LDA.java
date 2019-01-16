package algorithms;

import java.util.*;

public class LDA {

	/** Przeprowadź algorytm LDA 
		@arg documentsHistograms Lista tablic, każdy wpis w liście to jeden dokument, tablica określa ilości słów o tym indeksie */
    public LDA(List<int[]> documentsHistograms) {
		//sprawdź czy każda tablica zawiera taką samą ilość wartości
		final int histogramValuesCount = documentsHistograms.get(0).length;
		final int documentsCount = documentsHistograms.size();
		for(int[] histogram : documentsHistograms) {
			assert(histogram.length == histogramValuesCount);
		}
    
		System.out.printf("Algorytm LDA\nDokumentów: %d\nIlość danych histogramu: %d\n", documentsCount, histogramValuesCount);
		//DEBUG wydrukuj wartości argumentu
		for(int documentIndex = 0; documentIndex < documentsHistograms.size(); documentIndex++) {
			
		}

    }
}
