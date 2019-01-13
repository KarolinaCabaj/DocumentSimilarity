package data_preprocessing;

import java.util.ArrayList;
import java.util.Hashtable;


class StopWords {
    private Hashtable<String, Boolean> stopWords;

    StopWords() {
        stopWords = new Hashtable<>();
        FileLineReader fileLineReader = new FileLineReader();
        fileLineReader.read(stopWords);
    }

    String[] removeStopWords(String[] tokens) {
        ArrayList<String> removedStopWords = new ArrayList<String>();
        StopWords stopWords = new StopWords();

        for (String token : tokens) {
            if (!stopWords.isStopWord(token)) {
                removedStopWords.add(token);
            }
        }

        return ListToArray(removedStopWords);
    }

    private boolean isStopWord(String s) {
        boolean result = stopWords.get(s) != null;
        if (s.length() == 1) result = true;
        return result;
    }

    private String[] ListToArray(ArrayList<String> removedStopWords) {
        String[] tokensWithoutStopWords = new String[removedStopWords.size()];
        removedStopWords.toArray(tokensWithoutStopWords);
        return tokensWithoutStopWords;
    }

}