package actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import algorithms.LDA;
import algorithms.LSI;
import data_preprocessing.BookReader;
import message.StartWorkMsg;
import message.WorkOrderMsg;
import message.WorkResultMsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WorkManager extends AbstractActor {
    static public Props props(Integer amountOfWorkers) {
        return Props.create(WorkManager.class, () -> new WorkManager(amountOfWorkers));
    }

    private LoggingAdapter log;
    private final List<ActorRef> analystActors;
    private List<String> readyBooksLSI;
    private List<String> inProgressBooksLSI;
    private List<String> doneBooksLSI;
    private List<String> readyBooksLDA;
    private List<String> inProgressBooksLDA;
    private List<String> doneBooksLDA;
    private Integer receivedResult;

    private WorkManager(Integer amountOfWorkers) {
        log = Logging.getLogger(getContext().getSystem(), this);
        analystActors = IntStream
                .range(0, amountOfWorkers)
                .mapToObj(n -> getContext().actorOf(Analyst.props(), "worker" + n))
                .collect(Collectors.toList());
        log.info("Waiting for order");
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartWorkMsg.class, msg -> startWork())
                .match(WorkResultMsg.class, this::finishWork)
                .matchAny(o -> log.info("Received unknown message"))
                .build();
    }

    private void startWork() {
        log.info("Starting work...");
        receivedResult = 0;
        initBookList();
        for (ActorRef actor : analystActors) {
            if (analystActors.indexOf(actor) % 2 == 0) {
                sendLSI(actor);
            } else {
                sendLDA(actor);
            }
        }
    }

    private void initBookList() {
        String path = "src/main/java/ksiazki/The Fault in Our Stars ( PDFDrive.com ).pdf";
        BookReader bookReader = new BookReader(path, 10);
        readyBooksLSI = Arrays.stream(bookReader.getChapters()).collect(Collectors.toList());
        inProgressBooksLSI = new ArrayList<>();
        doneBooksLSI = new ArrayList<>();
        readyBooksLDA = Arrays.stream(bookReader.getChapters()).collect(Collectors.toList());
        inProgressBooksLDA = new ArrayList<>();
        doneBooksLDA = new ArrayList<>();
    }

    private void sendLSI(ActorRef actor) {
        Random rand = new Random();
        String book = readyBooksLSI.get(rand.nextInt(readyBooksLSI.size()));
        if (book != null) {
            actor.tell(new WorkOrderMsg(book, new LSI()), getSelf());
            inProgressBooksLSI.add(book);
            readyBooksLSI.remove(book);
        }
    }

    private void sendLDA(ActorRef actor) {
        Random rand = new Random();
        String book = readyBooksLDA.get(rand.nextInt(readyBooksLDA.size()));
        if (book != null) {
            actor.tell(new WorkOrderMsg(book, new LDA()), getSelf());
            inProgressBooksLDA.add(book);
            readyBooksLDA.remove(book);
        }
    }

    private void finishWork(WorkResultMsg msg) {
        markOutWork(msg);
        if (!readyBooksLSI.isEmpty()) {
            sendLSI(getSender());
        } else if (!readyBooksLDA.isEmpty()) {
            sendLDA(getSender());
        } else {
            getSender().tell(akka.actor.PoisonPill.getInstance(), ActorRef.noSender());
        }
        if (inProgressBooksLSI.isEmpty() && inProgressBooksLDA.isEmpty()) {
            log.notifyInfo("Work has been done");
        }
    }

    private void markOutWork(WorkResultMsg msg) {
        if (msg.getWorkOrderMsg().getAlg() instanceof LSI) {
            doneBooksLSI.add(msg.getWorkOrderMsg().getFileName());
            inProgressBooksLSI.remove(msg.getWorkOrderMsg().getFileName());
        } else {
            doneBooksLDA.add(msg.getWorkOrderMsg().getFileName());
            inProgressBooksLDA.remove(msg.getWorkOrderMsg().getFileName());
        }
    }
}
