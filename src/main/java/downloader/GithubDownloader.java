package downloader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GithubDownloader {
    private Map<String, Float> exemplificatoryWords;
    private Document page;


    public GithubDownloader() {
        exemplificatoryWords = new HashMap<>();
        loadPage();
    }

    private void loadPage() {
        try {
            page = Jsoup.connect("https://github.com/dkpro/dkpro-similarity/blob/master/dkpro-similarity-experiments-wordpairs-asl/src/main/resources/datasets/wordpairs/en/finkelstein353.gold.pos.txt").get();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void extractData() {
        String rawData = page.select("tbody").text();
        List<String> splitData = Arrays.stream(rawData.split("\\s+"))
                .filter(s -> !s.contains("#"))
                .collect(Collectors.toList());
        pushToMap(splitData);
    }

    private void pushToMap(List<String> splitData) {
        splitData.forEach(
                s -> {
                    String[] elements = s.split(":");
                    String key = elements[0] + ":" + elements[1];
                    Float value = new Float(elements[2]);
                    exemplificatoryWords.put(key, value);
                }
        );
    }

    public Map<String, Float> getExemplificatoryWords() {
        if (exemplificatoryWords.isEmpty())
            extractData();
        return exemplificatoryWords;
    }
}
