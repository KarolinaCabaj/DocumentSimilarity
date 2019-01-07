package actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import algorithms.Algorithm;
import message.WorkOrderMsg;
import message.WorkResultMsg;

public class Analyst extends AbstractActor {

    static Props props() {
        return Props.create(Analyst.class, Analyst::new);
    }

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private Algorithm alg;
    private Analyst() {
        log.info("Waiting for work");
    }

    private WorkResultMsg analyzeText(WorkOrderMsg workOrderMsg) {
        log.info("working");
        alg = workOrderMsg.getAlg();
        preprocessText();
        doAlgorithm();
        return new WorkResultMsg("result");
    }

    private void preprocessText(){

    }

    private void doAlgorithm(){
        alg.run();
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
