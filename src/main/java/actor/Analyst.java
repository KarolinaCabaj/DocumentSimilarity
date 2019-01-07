package actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Analyst extends AbstractActor {

    static public Props props() {
        return Props.create(Analyst.class, Analyst::new);
    }
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public Analyst() {
        log.info("Waiting for work");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().build();
    }
}
