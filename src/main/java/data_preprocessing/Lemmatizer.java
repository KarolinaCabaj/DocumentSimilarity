package data_preprocessing;

import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

import java.io.*;

class Lemmatizer {

    String[] getLemma(String[] tokens) {
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
        String relativeFilePath = "src/main/java/data_preprocessing/en-pos-maxent.bin";
        String absoluteFilePath = new File(relativeFilePath).getAbsolutePath();
        InputStream posModelIn = new FileInputStream(absoluteFilePath);
        POSModel posModel = new POSModel(posModelIn);
        POSTaggerME posTagger = new POSTaggerME(posModel);
        return posTagger.tag(tokens);
    }

    private String[] specifyLemmas(String[] tokens, String[] tags) throws IOException {
        String relativeFilePath = "src/main/java/data_preprocessing/en-lemmatizer.dict.txt";
        String absoluteFilePath = new File(relativeFilePath).getAbsolutePath();
        InputStream dictLemmatizer = new FileInputStream(absoluteFilePath);
        DictionaryLemmatizer lemmatizer = new DictionaryLemmatizer(dictLemmatizer);
        return lemmatizer.lemmatize(tokens, tags);
    }
}
