package actor;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import data_preprocessing.TextPreprocessor;
import data_preprocessing.Vectorizer;
import downloader.UrlLoader;
import downloader.WikiDownloader;
import message.StartWorkMsg;
import message.WorkOrderMsg;
import message.WorkResultMsg;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WorkManager extends AbstractActor {
    static public Props props(Integer amountOfWorkers) {
        return Props.create(WorkManager.class, () -> new WorkManager(amountOfWorkers));
    }

    private LoggingAdapter log;
    private String filePath;
    private List<ActorRef> analystActors;
    private List<String> readyBooksLSI;
    private List<String> inProgressBooksLSI;
    private List<String> doneBooksLSI;
    private List<String> readyBooksLDA;
    private List<String> inProgressBooksLDA;
    private List<String> doneBooksLDA;
    private Map<String, WorkOrderMsg> workSchedule;
    private List<String> terms;

    private WorkManager(Integer amountOfWorkers) {
        log = Logging.getLogger(getContext().getSystem(), this);
        workSchedule = new HashMap<>();
        analystActors = IntStream
                .range(0, amountOfWorkers)
                .mapToObj(n -> getContext().actorOf(Analyst.props(), "worker" + n))
                .collect(Collectors.toList());
        log.info("Waiting for order");
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartWorkMsg.class, this::startWork)
                .match(WorkResultMsg.class, this::finishWork)
                .match(Terminated.class, this::showMustGoOn)
                .matchAny(o -> log.info("Received unknown message"))
                .build();
    }

    private void startWork(StartWorkMsg msg) {
        log.info("Starting work...");
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
        doneBooksLSI = new ArrayList<>();
        readyBooksLDA = Arrays.stream(pages).collect(Collectors.toList());
        inProgressBooksLDA = new ArrayList<>();
        doneBooksLDA = new ArrayList<>();
    }

    private void setTerms(String[] pages) {
        TextPreprocessor textPreprocessor = new TextPreprocessor();
        List<String[]> listOfDocumentsTerms = createLisOfDocumentTerms(pages, textPreprocessor);
        terms = new Vectorizer(listOfDocumentsTerms).getTerms();
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
//            WorkOrderMsg msg = new WorkOrderMsg(book, new LDA());
//            actor.tell(msg, getSelf());
//            workSchedule.put(actor.path().name(), msg);
//            inProgressBooksLDA.add(book);
//            readyBooksLDA.remove(book);
        }
    }

    private void finishWork(WorkResultMsg msg) {
        markOutWork(msg);
        if (!readyBooksLSI.isEmpty()) {
            sendLSI(getSender());
        } else if (!readyBooksLDA.isEmpty()) {
            sendLDA(getSender());
        }
        if (inProgressBooksLSI.isEmpty() && inProgressBooksLDA.isEmpty()) {
            log.notifyInfo("Work has been done");
            getContext().getChildren().forEach(this::sayGoodBay);
            context().system().terminate();
        }
    }

    private void markOutWork(WorkResultMsg msg) {
        if (msg.getWorkOrderMsg().getWorkType().equals(WorkOrderMsg.WorkType.LSI)) {
            doneBooksLSI.add(msg.getWorkOrderMsg().getDoc());
            inProgressBooksLSI.remove(msg.getWorkOrderMsg().getDoc());
        } else {
            doneBooksLDA.add(msg.getWorkOrderMsg().getDoc());
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
