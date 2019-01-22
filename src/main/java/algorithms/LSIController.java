package algorithms;

import data_preprocessing.TextPreprocessor;
import data_preprocessing.Vectorizer;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import postprocessing.QualityMeasureEnum;
import postprocessing.ResultEvaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class LSIController {
    private List<String> readyBooksLSI;
    private List<String> inProgressBooksLSI;
    private List<String> terms;
    private Vectorizer vectorizer;
    private List<RealVector> documentVectors;
    private Boolean isLSIdone = false;

    public LSIController() {
    }

    public void startLSIProcess(String[] pages) {
        documentVectors = new ArrayList<>();
        initWorkList(pages);
        initTerms(pages);
    }

    private void initWorkList(String[] pages) {
        readyBooksLSI = Arrays.stream(pages).collect(Collectors.toList());
        inProgressBooksLSI = new ArrayList<>();
    }

    private void initTerms(String[] pages) {
        TextPreprocessor textPreprocessor = new TextPreprocessor();
        List<String[]> listOfDocumentsTerms = createLisOfDocumentTerms(pages, textPreprocessor);
        vectorizer = new Vectorizer(listOfDocumentsTerms);
        terms = vectorizer.getTerms();
    }

    private List<String[]> createLisOfDocumentTerms(String[] pages, TextPreprocessor textPreprocessor) {
        List<String[]> listOfDocumentsTerms = new ArrayList<>();
        for (String document : pages) {
            String[] tokens = textPreprocessor.getPreparedTokens(document);
            listOfDocumentsTerms.add(tokens);
        }
        return listOfDocumentsTerms;
    }

    public void addDocumentVector(RealVector vector) {
        documentVectors.add(vector);
    }

    public void markoutJob(String book) {
        inProgressBooksLSI.remove(book);
    }

    public String pickRandomReadyBook() {
        Random rand = new Random();
        String book = readyBooksLSI.get(rand.nextInt(readyBooksLSI.size()));
        inProgressBooksLSI.add(book);
        readyBooksLSI.remove(book);
        return book;
    }

    public void completeLSIwork() {
        isLSIdone = true;
        RealMatrix countMatrix = vectorizer.getCountMatrix(documentVectors);
        LSI lsi = new LSI(countMatrix, 20);
        RealMatrix wordsMatrix = lsi.getWordsMatrix();
        ResultEvaluator ev = new ResultEvaluator(terms, wordsMatrix, null);
        ev.showAverage();
        ev.showEvaluationResults(QualityMeasureEnum.BAD);
        ev.showEvaluationResults(QualityMeasureEnum.GOOD);
        ev.showEvaluationResults(QualityMeasureEnum.GREAT);
        ev.getStandardDeviation();
    }

    public Boolean getLSIdone() {
        return isLSIdone;
    }

    public List<String> getTerms() {
        return terms;
    }

    public Boolean isEmptyReadyList() {
        return readyBooksLSI.isEmpty();
    }

    public Boolean isEmptyInProgressList() {
        return inProgressBooksLSI.isEmpty();
    }
}


