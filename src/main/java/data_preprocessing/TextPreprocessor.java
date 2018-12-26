package data_preprocessing;

import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

public class TextPreprocessor {
    private static final Pattern UNDESIRABLES = Pattern.compile("[(){},.;!?<>%0-9“”’']");

    public TextPreprocessor() {
    }

    private String[] tokenize(String text) { //todo usunac tokeny bedacy znakami interpunkcyjnymi itp
        Tokenizer tokenizer = SimpleTokenizer.INSTANCE;
        String parsedText = removeUndesiredCharacters(text.toLowerCase());
        return tokenizer.tokenize(parsedText);
    }

    private static String removeUndesiredCharacters(String x) {
        return UNDESIRABLES.matcher(x).replaceAll(" ");
    }

    private String[] removeStopWords(String[] tokens) {
        ArrayList<String> removedStopWords = new ArrayList<String>();
        StopWords stopWords = new StopWords();

        for (String token : tokens) {
            if (!stopWords.isStopWord(token)) {
                removedStopWords.add(token);
            }
        }

        return ListToArray(removedStopWords);
    }

    private String[] ListToArray(ArrayList<String> removedStopWords) {
        String[] tokensWithoutStopWords = new String[removedStopWords.size()];
        removedStopWords.toArray(tokensWithoutStopWords);
        return tokensWithoutStopWords;
    }

    private String[] getLemma(String[] tokens) {
        String[] lemmas = new String[tokens.length];
        try {
            String[] tags = specifyTags(tokens);
            lemmas = specifyLemmas(tokens, tags);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lemmas;
    }

    private String[] specifyTags(String[] tokens) throws IOException {
        InputStream posModelIn = new FileInputStream("D:\\studia\\semestr4\\WEDT\\WordSimilarity\\src\\main\\java\\data_preprocessing\\en-pos-maxent.bin");
        POSModel posModel = new POSModel(posModelIn);
        POSTaggerME posTagger = new POSTaggerME(posModel);
        return posTagger.tag(tokens);
    }

    private String[] specifyLemmas(String[] tokens, String[] tags) throws IOException {
        InputStream dictLemmatizer = new FileInputStream("D:\\studia\\semestr4\\WEDT\\WordSimilarity\\src\\main\\java\\data_preprocessing\\en-lemmatizer.dict.txt");
        DictionaryLemmatizer lemmatizer = new DictionaryLemmatizer(dictLemmatizer);
        return lemmatizer.lemmatize(tokens, tags);
    }

    private TreeSet<String> specifyLemmas(String[] tokens) {
        String[] lemmas = getLemma(tokens);
        return new TreeSet<String>(Arrays.asList(lemmas));
    }

//    public TreeSet<String> getPreparedTerms(String document) {
    public String[] getPreparedTerms(String document) {
        String[] tokens = tokenize(document);
        String[] tokensWithoutStopWords = removeStopWords(tokens);
//        return specifyLemmas(tokensWithoutStopWords);
        return getLemma(tokensWithoutStopWords);
    }
}
