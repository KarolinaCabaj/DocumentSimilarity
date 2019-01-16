package actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import data_preprocessing.TermDictionary;
import data_preprocessing.TextPreprocessor;
import message.WorkOrderMsg;
import message.WorkResultMsg;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import java.util.HashMap;
import java.util.List;

public class Analyst extends AbstractActor {

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private Analyst() {
        log.info("Waiting for work");
    }

    static Props props() {
        return Props.create(Analyst.class, Analyst::new);
    }

    private WorkResultMsg analyzeText(WorkOrderMsg workOrderMsg) {
        if (workOrderMsg.getWorkType().equals(WorkOrderMsg.WorkType.LSI)) {
            log.info("working for LSI");
            //zwraca histogram słów w dokumencie
            return new WorkResultMsg(getVector(workOrderMsg), workOrderMsg);
        } else {
            log.info("working for LDA");
            //zwraca histogram słów w dokumencie
            return new WorkResultMsg(getVector(workOrderMsg), workOrderMsg);
        }
    }

    private RealVector getVector(WorkOrderMsg workOrderMsg) {
        TextPreprocessor textPreprocessor = new TextPreprocessor();
        //WARNING zamiast RealVector powinien być tutaj raczej IntVector (gdyby istniał)
        RealVector vector = createOccurrenceVector(
                textPreprocessor.getPreparedTokens(workOrderMsg.getDoc()),
                workOrderMsg.getTerms());
		//NOTE pierwszy argument to tablica powtarzalnych słów
		//NOTE drugi argument to zbiór niepowtarzalnych słów
		for(int i = 0; i < workOrderMsg.getTerms().size(); i++) {
			//drukuje histogram słów w dokumencie, szukając słów ze wszystkich dokumentów
// 			System.out.printf("Ilość słów: %s → %f\n", workOrderMsg.getTerms().get(i), vector.getEntry(i));
		}
		
        return vector;
    }

	/** Oblicza ilość występujących słów ze zbioru @arg terms, licząc słowa w tablicy @arg tokens 
		Zwraca wektor liczb, gdzie każda liczba jest ilością wystąpionych słów */
    private RealVector createOccurrenceVector(String[] tokens, List<String> terms) {
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
