package actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import algorithms.LDA;
import algorithms.LSI;
import message.StartWorkMsg;
import message.WorkOrderMsg;
import message.WorkResultMsg;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WorkManager extends AbstractActor {
    static public Props props(Integer amountOfWorkers) {
        return Props.create(WorkManager.class, () -> new WorkManager(amountOfWorkers));
    }

    private LoggingAdapter log;
    private final List<ActorRef> analystActors;
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
                .match(StartWorkMsg.class, msg -> startWork()
                )
                .match(WorkResultMsg.class, this::finishWork)
                .matchAny(o -> log.info("Received unknown message"))
                .build();
    }

    private void startWork() {
        log.info("Starting work...");
        receivedResult = 0;
        for (ActorRef actor : analystActors) {
            actor.tell(analystActors.indexOf(actor) % 2 == 0 ?
                            new WorkOrderMsg("file", new LSI()) :
                            new WorkOrderMsg("file2", new LDA()),
                    getSelf());
        }
    }

    private void finishWork(WorkResultMsg msg) {
        getSender().tell(akka.actor.PoisonPill.getInstance(), ActorRef.noSender());
        receivedResult++;
        if (receivedResult == 10)
            log.notifyInfo("Work has been done");
    }
}
