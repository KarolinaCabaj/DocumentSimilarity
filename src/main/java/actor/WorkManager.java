package actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class WorkManager extends AbstractActor {
    static public Props props() {
        return Props.create(WorkManager.class, () -> new WorkManager());
    }
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public WorkManager() {
        log.info("Waiting for order");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().build();
    }
}
