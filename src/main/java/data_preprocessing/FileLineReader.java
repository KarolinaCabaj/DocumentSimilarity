package data_preprocessing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;

class FileLineReader {

    void read(Hashtable<String, Boolean> stopWords) { //todo jakos zmienic nazwe, albo rozdzielic, bo czytamy i od razu dodajemy do zbioru
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("D:\\studia\\semestr4\\WEDT\\WordSimilarity\\src\\main\\java\\data_preprocessing\\stop_words_en.txt"));
            String line = reader.readLine();
            while (line != null) {
                stopWords.put(line, true);
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}