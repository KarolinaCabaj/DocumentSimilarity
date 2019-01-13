package data_preprocessing;

import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import java.util.regex.Pattern;

public class TextPreprocessor {
    private static final Pattern UNDESIRABLES = Pattern.compile("[(){},.;!?<>%0-9“”’']");

    public String[] getPreparedTokens(String document) {
        String[] tokens = tokenize(document);
        StopWords stopWords = new StopWords();
        String[] tokensWithoutStopWords = stopWords.removeStopWords(tokens);
        Lemmatizer lemmatizer = new Lemmatizer();
        return lemmatizer.getLemma(tokensWithoutStopWords);
    }

    private String[] tokenize(String text) { //todo usunac tokeny bedacy znakami interpunkcyjnymi itp
        Tokenizer tokenizer = SimpleTokenizer.INSTANCE;
        String parsedText = removeUndesiredCharacters(text.toLowerCase());
        return tokenizer.tokenize(parsedText);
    }

    private static String removeUndesiredCharacters(String x) {
        return UNDESIRABLES.matcher(x).replaceAll(" ");
    }
}
