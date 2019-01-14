package data_preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestingWordsConverter {
    private String fileName;

    public TestingWordsConverter() {
        String relativeFilePath = "src/main/java/data_preprocessing/testing_words_pairs.txt";
        fileName = new File(relativeFilePath).getAbsolutePath();
    }


    public List<String[]> getParsedTestingData() {
        List<String[]> parsedTestingData = new ArrayList<>();
        File file = new File(fileName);

        parseData(parsedTestingData, file);

        return parsedTestingData;
    }

    private void parseData(List<String[]> parsedTestingData, File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String text;

            while ((text = reader.readLine()) != null) {
                parsedTestingData.add(parseLine(text));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String[] parseLine(String line) {
        String[] parts = line.split(":");
        return Arrays.copyOfRange(parts, 0, 3);
    }
}
