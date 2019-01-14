package actor;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import algorithms.LSI;
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

    private void setTerms(String[] pages) {
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
            WorkOrderMsg msg = new WorkOrderMsg(book, WorkOrderMsg.WorkType.LDA);
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
            log.info("LDA STAFF");
        }
        if (isLSIdone && isLDAdone) {
            getContext().getChildren().forEach(this::sayGoodBay);
            log.notifyInfo("Work has been done");
            context().system().terminate();
        }
    }

    private void doLsi() {
        RealMatrix countMatrix = vectorizer.getCountMatrix(documentVectors);
        LSI lsi = new LSI(countMatrix, 15);
        RealMatrix wordsMatrix = lsi.getWordsMatrix();
        ResultEvaluator ev = new ResultEvaluator(terms, wordsMatrix);
        ev.showAverage();
        ev.showEvaluationResults(QualityMeasureEnum.BAD);
        ev.showEvaluationResults(QualityMeasureEnum.GOOD);
        ev.showEvaluationResults(QualityMeasureEnum.GREAT);
    }

    private void markOutWork(WorkResultMsg msg) {
        if (msg.getWorkOrderMsg().getWorkType().equals(WorkOrderMsg.WorkType.LSI)) {
            documentVectors.add(msg.getResult());
            inProgressBooksLSI.remove(msg.getWorkOrderMsg().getDoc());
        } else {
            inProgressBooksLDA.remove(msg.getWorkOrderMsg().getDoc());
        }
    }

    private void sayGoodBay(ActorRef actor) {
        getContext().unwatch(actor);
        actor.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    private void showMustGoOn(Terminated terminated) {
        System.out.println("fuck");
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
