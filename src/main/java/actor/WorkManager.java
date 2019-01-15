package actor;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import algorithms.LSIController;
import downloader.UrlLoader;
import downloader.WikiDownloader;
import message.StartWorkMsg;
import message.WorkOrderMsg;
import message.WorkResultMsg;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WorkManager extends AbstractActor {
    private LoggingAdapter log;
    private String filePath;
    private LSIController lsiController;
    private List<ActorRef> analystActors;
    private List<String> readyBooksLDA;
    private List<String> inProgressBooksLDA;
    private Map<String, WorkOrderMsg> workSchedule;
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
        lsiController = new LSIController();
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

    private void initBookList() { // done
        UrlLoader ul = new UrlLoader(filePath);
        String[] pages = new WikiDownloader(ul.getUrls()).getPages();
        lsiController.startLSIProcess(pages);
        readyBooksLDA = Arrays.stream(pages).collect(Collectors.toList());
        inProgressBooksLDA = new ArrayList<>();
    }


    private void sendLSI(ActorRef actor) { // TODO spróbować całką metodę dać do wysłania w controller
        String book = lsiController.pickRandomReadyBook();
        if (book != null) {
            WorkOrderMsg msg = new WorkOrderMsg(book, WorkOrderMsg.WorkType.LSI, lsiController.getTerms());
            actor.tell(msg, getSelf());
            workSchedule.put(actor.path().name(), msg);
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
        if (!lsiController.isEmptyReadyList()) {
            sendLSI(getSender());
        } else if (!readyBooksLDA.isEmpty()) {
            sendLDA(getSender());
        }
        if (lsiController.isEmptyInProgressList() && lsiController.isEmptyReadyList() && !lsiController.getLSIdone()) {
            lsiController.completeLSIwork();
        } else if (readyBooksLDA.isEmpty() && inProgressBooksLDA.isEmpty() && !isLDAdone) {
            isLDAdone = true;
            log.info("LDA STAFF");
        }
        if (lsiController.getLSIdone() && isLDAdone) {
            getContext().getChildren().forEach(this::sayGoodBay);
            log.notifyInfo("Work has been done");
            context().system().terminate();
        }
    }


    private void markOutWork(WorkResultMsg msg) {
        if (msg.getWorkOrderMsg().getWorkType().equals(WorkOrderMsg.WorkType.LSI)) {
            lsiController.addDocumentVector(msg.getResult());
            lsiController.markoutJob(msg.getWorkOrderMsg().getDoc());
        } else {
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
