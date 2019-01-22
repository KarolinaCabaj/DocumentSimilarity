package actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import message.SyncMsg;
import message.TerminateMsg;
import message.WorkOrderMsg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Pracownik algorytmu LDA
 */
public class LDAWorker extends AbstractActor {
    /**
     * Wątek działania
     */
    private class StepThread extends Thread {
        LDAWorker thisWorker;

        StepThread(LDAWorker thisWorker) {
            this.thisWorker = thisWorker;
        }

        public void run() {
            while (alive) {
                thisWorker.stepOnce();
            }
            log.info("Worker zakończył wewnętrzny wątek");
        }
    }

    /**
     * Logger
     */
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    /**
     * Dokument, lista par słowo ↔ temat
     */
    private List<int[]> document;
    /**
     * Tablica Słowa ↔ Tematy, liczebności danego słowa w danym temacie
     */
    private Map<Integer, double[]> wordTopicsTable;
    /**
     * Sumy słów w danym temacie
     */
    private double[] topicsSums;
    /**
     * Suma słów w dokumencie
     **/
    private int wordsInDocument;
    /**
     * Liczności słów danego tematu w dokumencie
     */
    private int[] documentTopicVector;
    /**
     * Aktor jest żywy i działa
     */
    private boolean alive;
    /**
     * Ilość tematów w dokumencie
     */
    private int numberOfTopics;
    /**
     * Rodzic z którym się komunikujemy
     */
    private ActorRef parent;
    /**
     * Ilość przeprowadzonych synchronizacji
     */
    private int synchronizationCount;
    /**
     * Mutez do synchronizacji
     */
    private Object mutex = new Object();

    /**
     * Konstruktor
     */
    private LDAWorker() {
        this.wordTopicsTable = new HashMap<>();
        log.info("Pracownik LDA zrodził się");
    }

    /**
     * Fabryka do konstrukcji z zewnątrz
     */
    public static Props props() {
        return Props.create(LDAWorker.class, LDAWorker::new);

    }

    /**
     * Ropocznij pracę
     **/
    private void startWork(int numberOfTopics, List<int[]> textOfIdsTopics, List<double[]> baseWordTopicsTable, double[] topicsSums) {
        log.info("Rozpoczęcie pracy na " + numberOfTopics + " tematów z tekstem długości " + textOfIdsTopics.size());
        this.document = textOfIdsTopics;
        this.topicsSums = topicsSums;
        this.documentTopicVector = new int[numberOfTopics];
        this.alive = true;
        this.numberOfTopics = numberOfTopics;

        //przepisz argumenty na tablicę i wypełnij tablice operacyjne
        //dla każdej pary w dokumencie
        for (int[] pair : textOfIdsTopics) {
            //dodaj wiersz tego słowa to tablicy
            int wordId = pair[0];
            int topicId = pair[1];
            this.wordTopicsTable.put(new Integer(wordId), baseWordTopicsTable.get(wordId));
            wordsInDocument++;
            documentTopicVector[topicId]++;
        }

        //rób wszystko w osobnym wątku
        //ponieważ i tak wszystko działa na zasadzie prawdopodobieństw, wyścigi nie bardzo mają wpływ (chyba)
        StepThread thread = new StepThread(this);
        thread.start();
    }

    /**
     * Odbierz wiadomość synchronizacyjną, połącz dane i wyślij swoje w odpowiedzi
     */
    private void synchronize(SyncMsg values, ActorRef actorToSend) {
        //upewnij się, że pętla przejdzie przynajmniej raz
        synchronized (mutex) {
            stepOnce();

            //pozwól na odbiór niepełnej odpowiedzi startowej, nie integruj takiej
            if (!values.isEmpty()) {
                for (Integer wordId : values.getWordTopicsTable().keySet()) {
                    double[] topicValues = values.getWordTopicsTable().get(wordId);
                    //dla przysłanych słów, zaintegruj je, licząc średnią arytmetyczną
                    for (int i = 0; i < topicValues.length; i++) {
                        wordTopicsTable.get(wordId)[i] = (wordTopicsTable.get(wordId)[i] + topicValues[i]) / 2.0;
                    }
                    for (int i = 0; i < numberOfTopics; i++) {
                        topicsSums[i] = (topicsSums[i] + values.getTopicSums()[i]) / 2.0;
                    }
                }
                synchronizationCount++;
            }

            //wyślij wiadomość z parametrami do rodzica
            SyncMsg syncMsg = new SyncMsg(wordTopicsTable, topicsSums);
            actorToSend.tell(syncMsg, getSelf());
        }
    }

    /**
     * Odbierz i wyślij wiadomość
     */
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(WorkOrderMsg.class, order ->
                {
                    parent = getSender();
                    startWork(order.getNumberOfTopics(), order.getTextOfIds(), order.getTopicTable(), order.getTopicSums());
                })
                .match(SyncMsg.class, sync ->
                {
                    synchronize(sync, getSender());
                })
                .match(TerminateMsg.class, msg ->
                {
                    alive = false;
                })
                .build();
    }

    /**
     * Odepnij temat od dokumentu
     */
    private void unmarkWord(int wordId, int topicId) {
        //zmodyfikuj tabele i sumy
        wordTopicsTable.get(wordId)[topicId] -= 1;
        topicsSums[topicId] -= 1;
        documentTopicVector[topicId] -= 1;
        wordsInDocument -= 1;
    }

    /**
     * Ustaw temat słowa w dokumencie, słowo musi być wcześniej zdetematowane
     */
    private void markWordWithTopic(int wordId, int topicId) {
        //zmodyfikuj tabele i sumy
        wordTopicsTable.get(wordId)[topicId] += 1;
        topicsSums[topicId] += 1;
        documentTopicVector[topicId] += 1;
        wordsInDocument += 1;
    }

    /**
     * Współczynnik dotyczenia dokumentu na dany temat
     */
    private double getTopicInDocumentProbability(int topicId) {
        //liczba słów na dany temat w dokumencie
        //dzielona przez liczność wszystkich słów w dokumencie
        return ((double) documentTopicVector[topicId] / (double) wordsInDocument);
    }

    /**
     * Prawdopodobieństwo, że dane słowo dotyczny danego tematu
     */
    private double getWordInTopicProbability(int wordId, int topicId) {
        //liczba przyporządkowań słowa do tematu
        //podzielić przez liczność słów w temacie
        double wordsWithTopic = wordTopicsTable.get(wordId)[topicId];
        double topicSize = topicsSums[topicId];
        return (wordsWithTopic / topicSize);
    }

    /**
     * Przelicz tematy na wszystkich dokumentach
     */
    public void stepOnce() {
        synchronized (mutex) {
            for (int[] pair : document) {
                int wordId = pair[0];
                int oldTopicId = pair[1];

                //usuń klasę słowu
                unmarkWord(wordId, oldTopicId);

                //oblicz prawdopodobieństwa tematów dla tego słowa w tym dokumencie
                double[] probabilities = new double[numberOfTopics];
                double probabilitySum = 0;
                for (int topicId = 0; topicId < numberOfTopics; topicId++) {
                    double topicDocumentProbability = getTopicInDocumentProbability(topicId);
                    double wordTopicProbability = getWordInTopicProbability(wordId, topicId);
                    probabilities[topicId] = topicDocumentProbability * wordTopicProbability;
                    probabilitySum += probabilities[topicId];
                }

                //wylosuj nowy temat na podstawie tych prawdopodobieństw
                int newTopicId = oldTopicId;
                Random random = new Random();
                double probabilityPoint = random.nextDouble() * probabilitySum;
                double skippedSum = 0;
                //dodajemy kolejne wartości z tablicy, aż przekroczymy wylosowaną wartość
                for (int topicId = 0; topicId < numberOfTopics; topicId++) {
                    skippedSum += probabilities[topicId];
                    if (probabilityPoint < skippedSum) {
                        newTopicId = topicId;
                        break;
                    }
                }

                //ustaw nowy temat
                pair[1] = newTopicId;
                markWordWithTopic(wordId, newTopicId);
            }
        }
    }

}

