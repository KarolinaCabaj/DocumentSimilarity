package actor;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import algorithms.LSI;
import algorithms.LDA;
import data_preprocessing.TextPreprocessor;
import data_preprocessing.Vectorizer;
import downloader.UrlLoader;
import downloader.WikiDownloader;
import message.StartWorkMsg;
import message.WorkOrderMsg;
import message.WorkResultMsg;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import postprocessing.QualityMeasureEnum;
import postprocessing.ResultEvaluator;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WorkManager extends AbstractActor {
    private LoggingAdapter log;
    private String filePath;
    private List<ActorRef> analystActors;
    private List<String> readyBooksLSI;
    private List<String> inProgressBooksLSI;
    private List<String> readyBooksLDA;
    private List<String> inProgressBooksLDA;
    private Map<String, WorkOrderMsg> workSchedule;
    private List<String> terms;
    private Vectorizer vectorizer;
    private List<RealVector> documentVectors;
    private List<int[]> ldaDocumentVectors;
    private Boolean isLSIdone = false;
    private Boolean isLDAdone = false;

    private WorkManager(Integer amountOfWorkers) {
        log = Logging.getLogger(getContext().getSystem(), this);
        workSchedule = new HashMap<>();
        analystActors = IntStream
                .range(0, amountOfWorkers)
                .mapToObj(n -> getContext().actorOf(Analyst.props(), "worker" + n))
                .collect(Collectors.toList());
        log.info("Waiting for order");
    }

    static public Props props(Integer amountOfWorkers) {
        return Props.create(WorkManager.class, () -> new WorkManager(amountOfWorkers));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartWorkMsg.class, this::startWork)
                .match(WorkResultMsg.class, this::finishPrimaryWork)
                .match(Terminated.class, this::showMustGoOn)
                .matchAny(o -> log.info("Received unknown message"))
                .build();
    }

    private void startWork(StartWorkMsg msg) {
        log.info("Starting work...");
        documentVectors = new ArrayList<>();
        ldaDocumentVectors = new ArrayList<>();
        filePath = msg.getPath();
        initBookList();
        watchAnalysts();
        for (ActorRef actor : analystActors) {
            if (analystActors.indexOf(actor) % 2 == 0) {
                sendLSI(actor);
            } else {
                sendLDA(actor);
            }
        }
    }

    private void watchAnalysts() {
        analystActors.forEach(actor -> context().watch(actor));
    }

    private void initBookList() {
        UrlLoader ul = new UrlLoader(filePath);
        String[] pages = new WikiDownloader(ul.getUrls()).getPages();
        setTerms(pages);
        readyBooksLSI = Arrays.stream(pages).collect(Collectors.toList());
        inProgressBooksLSI = new ArrayList<>();
        readyBooksLDA = Arrays.stream(pages).collect(Collectors.toList());
        inProgressBooksLDA = new ArrayList<>();
    }

    /** Ustaw zbiór niepowtarzalnych słów z dokumentów **/
    private void setTerms(String[] pages) {
        TextPreprocessor textPreprocessor = new TextPreprocessor();
        List<String[]> listOfDocumentsTerms = createLisOfDocumentTerms(pages, textPreprocessor);
        //NOTE listOfDocumentsTerms to lista tablic słów z dokumentów oraz słowa O
        vectorizer = new Vectorizer(listOfDocumentsTerms);
        terms = vectorizer.getTerms();
        //NOTE terms to zbiór słów, bez powtórzeń, złączony ze wszystkich dokumentów
    }

    /** Oblicz listę tablic, gdzie tablica zawiera słowa z dokumentu */
    private List<String[]> createLisOfDocumentTerms(String[] pages, TextPreprocessor textPreprocessor) {
        List<String[]> listOfDocumentsTerms = new ArrayList<>();
        for (String document : pages) {
            String[] tokens = textPreprocessor.getPreparedTokens(document);
            //NOTE tokens to tablica stringów z dokumentów oraz liter O
            for(String token : tokens) {
// 				System.out.printf("Termowanie: %s\n", token);
            }
            listOfDocumentsTerms.add(tokens);
        }
        return listOfDocumentsTerms;
    }

    private void sendLSI(ActorRef actor) {
        Random rand = new Random();
        String book = readyBooksLSI.get(rand.nextInt(readyBooksLSI.size()));
        if (book != null) {
            WorkOrderMsg msg = new WorkOrderMsg(book, WorkOrderMsg.WorkType.LSI, terms);
            actor.tell(msg, getSelf());
            workSchedule.put(actor.path().name(), msg);
            inProgressBooksLSI.add(book);
            readyBooksLSI.remove(book);
        }
    }

    private void sendLDA(ActorRef actor) {
        Random rand = new Random();
        String book = readyBooksLDA.get(rand.nextInt(readyBooksLDA.size()));
        if (book != null) {
            WorkOrderMsg msg = new WorkOrderMsg(book, WorkOrderMsg.WorkType.LDA, terms);
            actor.tell(msg, getSelf());
            workSchedule.put(actor.path().name(), msg);
            inProgressBooksLDA.add(book);
            readyBooksLDA.remove(book);
        }
    }

    private void finishPrimaryWork(WorkResultMsg msg) {
        markOutWork(msg);
        if (!readyBooksLSI.isEmpty()) {
            sendLSI(getSender());
        } else if (!readyBooksLDA.isEmpty()) {
            sendLDA(getSender());
        }
        if (inProgressBooksLSI.isEmpty() && readyBooksLSI.isEmpty()&& !isLSIdone) {
            isLSIdone = true;
            doLsi();
        } else if (readyBooksLDA.isEmpty() && inProgressBooksLDA.isEmpty() && !isLDAdone) {
            isLDAdone = true;
            doLda();
        }
        if (isLSIdone && isLDAdone) {
            getContext().getChildren().forEach(this::sayGoodBay);
            log.notifyInfo("Work has been done");
            context().system().terminate();
        }
    }
    
    private void doLda() {
		LDA lda = new LDA(ldaDocumentVectors, 50, 100);
// 		lda.printWordTopicsTable(terms);
		List<int[]> bestWords = lda.getBestWordsInTopic();
		//wydrukuj
		System.out.printf("Najlepsze słowa: \n");
		for(int[] words : bestWords)
		{
			for(int i = 0; i < 10; i++)
			{
				System.out.printf("%s ", terms.get(words[i]));
			}
			System.out.printf("\n");
		}
		
		RealMatrix wordsMatrix = lda.getRealMatrix();
        ResultEvaluator ev = new ResultEvaluator(terms, wordsMatrix, lda);
        ev.showAverage();
        ev.showEvaluationResults(QualityMeasureEnum.BAD);
        ev.showEvaluationResults(QualityMeasureEnum.GOOD);
        ev.showEvaluationResults(QualityMeasureEnum.GREAT);
        ev.getStandardDeviation();
		
    }

    private void doLsi() {
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

    private void markOutWork(WorkResultMsg msg) {
		//do listy, której rekordy to histogramy słów w jednym dokumencie, dodaj nowy histogram obliczony przez aktora
        if (msg.getWorkOrderMsg().getWorkType().equals(WorkOrderMsg.WorkType.LSI)) {
            documentVectors.add(msg.getResult());
            inProgressBooksLSI.remove(msg.getWorkOrderMsg().getDoc());
        } else {
			//przepisz tablicę double na tablicę int i dodaj to zbioru histogramów
			int[] resultArray = new int[msg.getResult().getDimension()];
			for(int i = 0; i < msg.getResult().getDimension(); i++) {
				double histogramValue = msg.getResult().getEntry(i);
				resultArray[i] = (int)histogramValue;
				if(resultArray[i] != 0)
				{
// 					System.out.printf("Dodawanie: %d(%s) → %d\n", i, terms.get(i), resultArray[i]);
				}
			}
			ldaDocumentVectors.add(resultArray);
            inProgressBooksLDA.remove(msg.getWorkOrderMsg().getDoc());
        }
    }

    private void sayGoodBay(ActorRef actor) {
        getContext().unwatch(actor);
        actor.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    private void showMustGoOn(Terminated terminated) {
        String name = terminated.actor().path().name();
        backToWork(name);
    }

    private void backToWork(String name) {
        analystActors = analystActors.stream()
                .filter(actor -> !actor.path().name().equals(name))
                .collect(Collectors.toList());
        ActorRef actor = getContext().actorOf(Analyst.props(), name);
        context().watch(actor);
        analystActors.add(actor);
        actor.tell(workSchedule.get(name), getSelf());
    }
}
