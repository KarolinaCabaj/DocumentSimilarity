package data_preprocessing;

public interface Dictionary {
    Integer getTermOccurrences(String term);

    int getNumberOfTerms();
}