package algorithms;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.List;

/**
 * Zawiera odpowiedzi algorytmu LDA, dzięki czemu może być użyty do porównywania semantycznego wyrazów
 */
public class LDAResponse {
    final private int histogramValuesCount;
    final private int topicsPerDocument;
    final private double[] topicsSums;
    final private List<double[]> wordTopicsTable;

    public LDAResponse(int histogramValuesCount, int topicsPerDocument, double[] topicsSums, List<double[]> wordTopicsTable) {
        this.histogramValuesCount = histogramValuesCount;
        this.topicsPerDocument = topicsPerDocument;
        this.topicsSums = topicsSums;
        this.wordTopicsTable = wordTopicsTable;
    }

    /**
     * Podobieństwo semantyczne dwóch słów
     */
    public double calculateSimilarity(int wordId1, int wordId2) {
        if (wordId1 >= histogramValuesCount || wordId2 >= histogramValuesCount || wordId1 == -1 || wordId2 == -1) {
            return 0;
        }
        double importanceSum = 0;
        for (int topicId = 0; topicId < topicsPerDocument; topicId++) {
            double wordsSum = topicsSums[topicId];
            double word1Dist = wordTopicsTable.get(wordId1)[topicId] / wordsSum;
            double word2Dist = wordTopicsTable.get(wordId2)[topicId] / wordsSum;
            importanceSum += word1Dist * word2Dist;
        }
        return importanceSum;
    }

    /**
     * Macierz używana do porównywania sematycznego, w wierszach są tematy, w kolumnach słowa
     */
    public RealMatrix getRealMatrix() {
        RealMatrix response = new Array2DRowRealMatrix(histogramValuesCount, topicsPerDocument);
        for (int topicId = 0; topicId < topicsPerDocument; topicId++) {
            for (int wordId = 0; wordId < histogramValuesCount; wordId++) {
                response.setEntry(wordId, topicId, (double) wordTopicsTable.get(wordId)[topicId] / (double) topicsSums[topicId]);
            }
        }

        return response;
    }
}
