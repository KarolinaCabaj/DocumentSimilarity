package data_preprocessing;

import java.util.Hashtable;


class StopWords {
    private Hashtable<String, Boolean> stopWords;

    StopWords() {
        stopWords = new Hashtable<String, Boolean>();
        FileLineReader fileLineReader = new FileLineReader();
        fileLineReader.read(stopWords);
    }

    boolean isStopWord(String s) {
        boolean result = stopWords.get(s) != null;
        if (s.length() == 1) result = true;
        return result;
    }

}