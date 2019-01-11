package downloader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WikiDownloader {

    private List<String> pages;
    private List<String> urls;
    private List<Document> docs;


    public WikiDownloader(List<String> urls) {
        this.urls = urls;
        docs = new ArrayList<>();
        pages = new ArrayList<>();
        downloadPages();
        parseToString();
    }

    private void downloadPages() {
        urls.parallelStream()
                .forEach(url -> {
                            try {
                                docs.add(Jsoup.connect(url).get());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                );
    }

    private void parseToString() {
        docs.parallelStream()
                .forEach(
                        doc -> pages.add(doc.text())
                );
    }

    public String[] getPages() {
        return pages.toArray(new String[0]);
    }
}
