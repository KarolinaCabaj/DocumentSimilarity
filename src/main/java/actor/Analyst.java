package actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import message.WorkOrderMsg;
import message.WorkResultMsg;
import org.apache.commons.math3.linear.ArrayRealVector;

public class Analyst extends AbstractActor {

    static Props props() {
        return Props.create(Analyst.class, Analyst::new);
    }

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private Analyst() {
        log.info("Waiting for work");
    }

    private WorkResultMsg analyzeText(WorkOrderMsg workOrderMsg) {
        log.info("working");

        return new WorkResultMsg(new ArrayRealVector(), workOrderMsg);
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(WorkOrderMsg.class, order -> {
                    WorkResultMsg resultMsg = analyzeText(order);
                    getSender().tell(resultMsg, getSelf());
                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }
}
