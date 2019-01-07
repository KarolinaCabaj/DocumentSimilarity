package actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import message.StartWorkMsg;
import message.WorkOrderMsg;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WorkManager extends AbstractActor {
    static public Props props(Integer amountOfWorkers) {
        return Props.create(WorkManager.class, () -> new WorkManager(amountOfWorkers));
    }

    private LoggingAdapter log;
    private final List<ActorRef> analystActors;

    private WorkManager(Integer amountOfWorkers) {
        log = Logging.getLogger(getContext().getSystem(), this);
        analystActors = IntStream
                .range(0, amountOfWorkers)
                .mapToObj(n -> getContext().actorOf(Analyst.props(), "worker" + n))
                .collect(Collectors.toList());
        log.info("Waiting for order");
    }

    private void startWork() {
        log.info("Starting work...");
        for (ActorRef actor : analystActors) {
            actor.tell(analystActors.indexOf(actor) % 2 == 0 ?
                            new WorkOrderMsg("file", "arg") :
                            new WorkOrderMsg("file2", "arg2"),
                    getSelf());
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartWorkMsg.class, msg -> startWork()
                )
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }
}
