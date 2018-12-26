package data_preprocessing;

public interface Dictionary {
    Integer getTermIndex(String term);
    int getNumTerms();
}