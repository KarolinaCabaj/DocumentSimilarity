package downloader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class UrlLoader {
    private String absoluteFilePath;
    private List<String> urls;

    public UrlLoader(String relativeFilePath) {
        this.absoluteFilePath = new File(relativeFilePath).getAbsolutePath();
        loadFile();
    }

    private void loadFile() {
        try {
            String data = new String(Files.readAllBytes(Paths.get(absoluteFilePath)));
            getLinks(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<String> getLinks(String data) {
        return urls = Arrays.asList(data.split("\\r?\\n"));
    }

    public List<String> getUrls() {
        return urls;
    }
}
