package actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import data_preprocessing.TermDictionary;
import message.WorkOrderMsg;
import message.WorkResultMsg;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import java.util.HashMap;
import java.util.List;

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

    public RealVector createOccurrenceVector(String[] tokens, List<String> terms) {
        RealVector vec = new ArrayRealVector(terms.size());
        TermDictionary termDictionary = new TermDictionary(tokens);
        HashMap<String, Integer> termsOccurrences = termDictionary.getOccurrencesOfTerms();

        for (int i = 0; i < terms.size(); i++) {
            if (termsOccurrences.containsKey(terms.get(i))) {
                String term = terms.get(i);
                vec.addToEntry(i, termsOccurrences.get(term));
            } else {
                vec.addToEntry(i, 0);
            }
        }
        return vec;
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
