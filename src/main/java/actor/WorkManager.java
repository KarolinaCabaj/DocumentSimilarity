package actor;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import algorithms.LSIController;
import downloader.UrlLoader;
import downloader.WikiDownloader;
import message.*;
import postprocessing.ResultEvaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WorkManager extends AbstractActor {
    private LoggingAdapter log;
    private String filePath;
    private LSIController lsiController;
    private List<ActorRef> analystActors;
    private Map<String, WorkOrderMsg> workSchedule;
    private List<int[]> ldaDocumentVectors;
    private Boolean isLDAdone = false;
    private Integer numberOfTopics;
    private ActorRef ldaManager;

    private WorkManager(Integer amountOfWorkers) {
        log = Logging.getLogger(getContext().getSystem(), this);
        workSchedule = new HashMap<>();
        ldaDocumentVectors = new ArrayList<>();
        ldaManager = getContext().actorOf(LDAManager.props(), "lda-manager");
        analystActors = IntStream
                .range(0, amountOfWorkers)
                .mapToObj(n -> getContext().actorOf(LSIAnalyst.props(), "worker" + n))
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
                .match(TerminateMsg.class, this::finish)
                .match(FinishMsg.class, this::finishLda)
                .matchAny(o -> log.info("Received unknown message"))
                .build();
    }

    private void finishLda(FinishMsg finishMsg) {
        //wydrukuj wynik
        System.out.printf("Najlepsze słowa: \n");
        for (int[] words : finishMsg.getBestWords()) {
            for (int i = 0; i < 10; i++) {
                System.out.printf("%s ", lsiController.getTerms().get(words[i]));
            }
            System.out.printf("\n");
        }
        //przeprowadź test podobieństwa semantycznego
        ResultEvaluator ev = new ResultEvaluator(lsiController.getTerms(), finishMsg.getLdaResponse().getRealMatrix(), finishMsg.getLdaResponse());
        ev.evaluate();

        //zabij LDA managera
        log.info("Zabijanie LDA Managera");
        getContext().unwatch(ldaManager);
        ldaManager.tell(PoisonPill.getInstance(), ActorRef.noSender());

        isLDAdone = true;
        //wyjdź z programu
        finish(new TerminateMsg());

    }

    private void startWork(StartWorkMsg msg) {
        log.info("Starting work...");
        lsiController = new LSIController();
        filePath = msg.getPath();
        initBookList();
        watchAnalysts();
        for (ActorRef actor : analystActors) {
            sendLSI(actor);
        }
        analystActors.add(ldaManager);
    }

    private void watchAnalysts() {
        analystActors.forEach(actor -> context().watch(actor));
    }

    private void initBookList() { // done
        UrlLoader ul = new UrlLoader(filePath);
        String[] pages = new WikiDownloader(ul.getUrls()).getPages();
        lsiController.startLSIProcess(pages);
    }


    private void sendLSI(ActorRef actor) { // TODO spróbować całką metodę dać do wysłania w controller
        String book = lsiController.pickRandomReadyBook();
        if (book != null) {
            WorkOrderMsg msg = new WorkOrderMsg(book, WorkOrderMsg.WorkType.LSI, lsiController.getTerms());
            actor.tell(msg, getSelf());
            workSchedule.put(actor.path().name(), msg);
        }
    }


    private void finishPrimaryWork(WorkResultMsg msg) {
        markOutWork(msg);
        if (!lsiController.isEmptyReadyList()) {
            sendLSI(getSender());
        }
        if (lsiController.isEmptyInProgressList() && lsiController.isEmptyReadyList() && !lsiController.getLSIdone() && !isLDAdone) {
            lsiController.completeLSIwork();
            //uruchom LDA
            doLda();
        }
        if (lsiController.getLSIdone() && isLDAdone) {
            getContext().getChildren().forEach(this::sayGoodBay);
            log.notifyInfo("Work has been done");
            context().system().terminate();
        }
    }

    private void finish(TerminateMsg terminated) {
        getContext().getChildren().forEach(this::sayGoodBay);
        log.notifyInfo("Work has been done");
        context().system().terminate();
    }

    /**
     * Uruchom aktora LDA Manager, który zwóci nam wynik, jak skończy
     */
    private void doLda() {
        //ldaDocumentVectors → histogramy do pracy LDA

        //wyślij polecenie startu
        WorkOrderMsg msg = new WorkOrderMsg(null, WorkOrderMsg.WorkType.LDA, null, 30, 200, ldaDocumentVectors);
        ldaManager.tell(msg, getSelf());
        // zapisanie jaka prace przydzielono
        workSchedule.put(ldaManager.path().name(), msg);
        //obserwuj jakby się popsuło
        context().watch(ldaManager);

    }


    private void markOutWork(WorkResultMsg msg) {
        //do listy, której rekordy to histogramy słów w jednym dokumencie, dodaj nowy histogram obliczony przez aktora
        if (msg.getWorkOrderMsg().getWorkType().equals(WorkOrderMsg.WorkType.LSI)) {
            lsiController.addDocumentVector(msg.getResult());
            lsiController.markoutJob(msg.getWorkOrderMsg().getDoc());

            //przepisz tablicę double na tablicę int i dodaj to zbioru histogramów
            int[] resultArray = new int[msg.getResult().getDimension()];
            for (int i = 0; i < msg.getResult().getDimension(); i++) {
                double histogramValue = msg.getResult().getEntry(i);
                resultArray[i] = (int) histogramValue;
            }
            ldaDocumentVectors.add(resultArray);
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
        ActorRef actorRef;
        if (!name.equals("lda-manager")) {
            analystActors = analystActors.stream()
                    .filter(actor -> !actor.path().name().equals(name))
                    .collect(Collectors.toList());
            actorRef = getContext().actorOf(LSIAnalyst.props(), name);
        } else {
            actorRef = getContext().actorOf(LDAManager.props(), "ActorRef actor");
        }
        context().watch(actorRef);
        analystActors.add(actorRef);
        actorRef.tell(workSchedule.get(name), getSelf());
    }
}
