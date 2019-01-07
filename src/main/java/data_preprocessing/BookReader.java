package data_preprocessing;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;

public class BookReader implements ChapterSplitter {
    private String fileName;
    private String bookContent;
    private Integer startPage;
    private String[] chapters;

    public BookReader(String fileName, Integer startPage){
        this.fileName = fileName;
        this.startPage = startPage;
    }

    public String[] getChapters() {
        return chapters;
    }

    public void readBook() {
        PDDocument document = null;
        try {
            document = PDDocument.load(new File(this.fileName));
            document.getNumberOfPages();
            if (!document.isEncrypted()) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(startPage);
                bookContent = stripper.getText(document);
            }
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void divideByChapters() { //todo: dla jednej ksiazki tak zrobione, rozne ksiazki rozny foramt, trzeba bedzie oddzielne parsowanie zrobic
        String splitter = "CHAPTER";
        chapters = bookContent.split(splitter); //todo usunac pierwszy pusty rozdzial i pierwsze slowa usunac
    }
}

