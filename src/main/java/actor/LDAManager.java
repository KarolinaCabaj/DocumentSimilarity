package actor;
import akka.actor.*;
import actor.LDAWorker;
import akka.actor.AbstractActor;
import akka.event.LoggingAdapter;
import akka.actor.Props;
import message.WorkOrderMsg;
import message.SyncMsg;
import message.TerminateMsg;
import message.FinishMsg;
import akka.event.Logging;
import algorithms.LDAResponse;
import java.util.*;
import java.lang.*;
import java.lang.IllegalArgumentException;

/** Zarządza i rozdaje aktorów algorytmu LDA */
public class LDAManager extends AbstractActor
{
	/** Porównywalna para prawdopodobieństwa u identyfikatora słowa */
	private class WordProbabilityPair implements Comparable<WordProbabilityPair> {
		public double probability;
		public int wordId;
		
		public WordProbabilityPair(double probability, int wordId) {
			this.probability = probability;
			this.wordId = wordId;
		}
		
		public int compareTo(WordProbabilityPair other)
		{
			if(other.probability > probability)
			{
				return 1;
			}
			else if(other.probability < probability)
			{
				return -1;
			}
			else
			{
				//to się prawie nigdy nie zdarzy przy doublach
				return 0;
			}
		}
	
	}

	/** Logger */
	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	/** Lista aktorów */
	private List<ActorRef> workersList;
	
	/** Tablica Słowa ↔ Tematy, liczebności danego słowa w danym temacie */
	private List<double[]> wordTopicsTable;
	
	/** Sumy słów w danym temacie */
	private double[] topicsSums;
	
	/** Ilości synchronizacji */
	private int[] synchronizations;

	/** Ilość szukanych tematów */
	private int numberOfTopics;
	
	/** Ilość synchronizacji z każdym, potrzebna do zakończenia programu */
	private int numberOfSyncs;
	 
	/** Czy już nie odbiera wiadomości */
	boolean isFinished;
	
	/** Obiekt do synchronizacji */
	Object mutex = new Object();
	
	/** Aktor, do którego zwracamy wynik */
	ActorRef parent;
	
	Map<ActorRef, WorkOrderMsg> workerOrders;
	
	/** Konstruktor */
	private LDAManager()
	{
		log.info("Manager LDA zrodził się");
		workersList = new ArrayList<>();
		workerOrders = new HashMap<>();
	}
	
	/** Fabryka do konstrukcji z zewnątrz */
	public static Props props()
	{
		return Props.create(LDAManager.class, LDAManager::new);
	}
	
	/** Rozstaw pracowników **/
	private void dispatchSubworkers(int numberOfTopics, int numberOfSyncs, List<int[]> histograms)
	{
		if(histograms == null)
		{
			throw new IllegalArgumentException("Podane histogramy są null");
		}
		final int numberOfWorkers = histograms.size();
		this.synchronizations = new int[numberOfWorkers];
		this.numberOfTopics = numberOfTopics;
		this.numberOfSyncs = numberOfSyncs;
		log.info("Rozstawianie " + numberOfWorkers + " pracowników na " + numberOfTopics + " tematów przy " + numberOfSyncs + " synchronizajach");
		
		if(this.numberOfTopics <= 0)
		{
			throw new IllegalArgumentException("Ilość pracowników jest nieprawidłowa: " + this.numberOfTopics);
		}
		else if(this.numberOfSyncs <= 0)
		{
			throw new IllegalArgumentException("Ilość synchronizacji jest nieprawidłowa: " + this.numberOfSyncs);
		}
		else if(numberOfWorkers <= 0)
		{
			throw new IllegalArgumentException("Histogram nie zawiera żadnych danych");
		}
		
		//tablica słowa ↔ tematy
		this.wordTopicsTable = new ArrayList<>();
		for(int i = 0; i < histograms.get(0).length; i++)
		{
			this.wordTopicsTable.add(new double[numberOfTopics]);
		}
		//liczności tematów
		this.topicsSums = new double[numberOfTopics];
		
		//uruchamianie pracowników
		for(int i = 0; i < numberOfWorkers; i++)
		{
			ActorRef worker = getContext().actorOf(LDAWorker.props(), "lda-worker-" + i);
			workersList.add(worker);

		}
		//wylosuj tematy i wypełnij tabele
		log.info("Przydzielanie wstępnych tematów");
		List<List<int[]>> textsOfIdsWithTopics = new ArrayList<>();
		//dla każdego tekstu
		for(int textIndex = 0; textIndex < numberOfWorkers; textIndex++)
		{
			List<int[]> textOfIdsWithTopics = new ArrayList<>();
			//dla każdego słowa w tekście
			for(int wordIndex = 0; wordIndex < histograms.get(textIndex).length; wordIndex++)
			{
				for(int i = 0; i < histograms.get(textIndex)[wordIndex]; i++)
				{
					int[] pair = new int[2];
					pair[0] = wordIndex;
					Random random = new Random();
					pair[1] = random.nextInt(numberOfTopics);
					textOfIdsWithTopics.add(pair);
					
					//dodaj do tabeli
					wordTopicsTable.get(pair[0])[pair[1]] += 1;
					topicsSums[pair[1]]++;
				}
			}
			textsOfIdsWithTopics.add(textOfIdsWithTopics);
		}
		
		log.info("Uruchamianie pracowników");
		for(ActorRef actor : workersList)
		{
			List<int[]> textOfIdsWithTopics = textsOfIdsWithTopics.get(workersList.indexOf(actor));
			WorkOrderMsg msg = new WorkOrderMsg(numberOfTopics, textOfIdsWithTopics, wordTopicsTable, topicsSums);
			actor.tell(msg , getSelf());
			context().watch(actor);
			workerOrders.put(actor, msg);
		}
		log.info("Rozpoczynanie synchronizacji");
		for(ActorRef actor : workersList)
		{
			//wyślij pustą wiadomość
			actor.tell(new SyncMsg() , getSelf());
		}
	}
	
	private void syncTables(SyncMsg sync, ActorRef sender)
	{
		synchronized(mutex)
		{
		if(isFinished)
		{
			return;
		}
	
			for(Integer wordId : sync.getWordTopicsTable().keySet())
			{
				double[] topicValues = sync.getWordTopicsTable().get(wordId);
				//dla przysłanych słów, zaintegruj je, licząc średnią arytmetyczną
				for(int i = 0; i < topicValues.length; i++)
				{
					wordTopicsTable.get(wordId)[i] = (wordTopicsTable.get(wordId)[i] + topicValues[i]) / 2.0;
				}
				for(int i = 0; i < numberOfTopics; i++)
				{
					topicsSums[i] = (topicsSums[i] + sync.getTopicSums()[i]) / 2.0;
				}

			}
			
			//wstaw zaktualizowane wartości i wyślij z powrotem
			Map<Integer, double[]> newTable = new HashMap<>();
			
			for(Integer wordId : sync.getWordTopicsTable().keySet())
			{
				newTable.put(wordId, wordTopicsTable.get(wordId));
			}
			SyncMsg newSyncMsg = new SyncMsg(newTable, topicsSums);
			sender.tell(sync, getSelf());
			
			//sprawdź, czy nie trzeba już zakończyć
			checkTermination();
			
			//zwiększ licznik dla tego aktora
			synchronizations[workersList.indexOf(sender)]++;

		}
	}
	
	/** Sprawdza, czy nie trzeba zamknąć programu, bo zsynchronizował się z każdym wystarczającą ilość razy */
	private void checkTermination()
	{
		for(ActorRef actor : workersList)
		{
			if(synchronizations[workersList.indexOf(actor)] < numberOfSyncs)
			{
				return;
			}
		}
		isFinished = true;
		for(ActorRef actor : workersList)
		{
			//TerminateMsg zakończy wątki w Workerach
			actor.tell(new TerminateMsg(), getSelf());
			actor.tell(PoisonPill.getInstance(), ActorRef.noSender());
			getContext().unwatch(actor);
		}

		log.info("LDA Manager zakończył pracę");
		FinishMsg finishMsg = new FinishMsg(getBestWordsInTopic(), new LDAResponse(wordTopicsTable.size(), numberOfTopics, topicsSums, wordTopicsTable));
		parent.tell(finishMsg, getSelf());
	}
	
	/** Oblicz najlepsze słowa w każdym temacie */
    public List<int[]> getBestWordsInTopic()
    {
		List<int[]> response = new ArrayList<>();
		for(int topicId = 0; topicId < numberOfTopics; topicId++)
		{
			response.add(new int[wordTopicsTable.size()]); 
		}
		
		//wsadź prawdopodobieństwa do drzew
		List<Set<WordProbabilityPair>> setsInTopics = new ArrayList<>();
		for(int topicId = 0; topicId < numberOfTopics; topicId++)
		{
			Set<WordProbabilityPair> set = new TreeSet<>();
			final double wordsInTopic = topicsSums[topicId];
			for(int wordId = 0; wordId < wordTopicsTable.size(); wordId++)
			{
				double wordProbability = wordTopicsTable.get(wordId)[topicId] / wordsInTopic;
				set.add(new WordProbabilityPair(wordProbability, wordId));
			}
			setsInTopics.add(set);
		}
		//odczytaj
		for(int topicId = 0; topicId < numberOfTopics; topicId++)
		{
			int index = 0;
			for(WordProbabilityPair pair : setsInTopics.get(topicId))
			{
				response.get(topicId)[index] = pair.wordId;
				index++;
			}
		}
		
		return response;
    }
    
    /** Zrestartuj pracownika */
    private void restartWorker(Terminated msg)
    {
		if(!isFinished)
		{
			ActorRef brokenWorker = msg.actor();
			WorkOrderMsg order = workerOrders.get(brokenWorker);
			workerOrders.remove(brokenWorker);
			ActorRef newWorker = getContext().actorOf(LDAWorker.props(), brokenWorker.path().name());
			workersList.set(workersList.indexOf(brokenWorker), newWorker);
			synchronizations[workersList.indexOf(brokenWorker)] = 0;
			workerOrders.put(newWorker, order);
			log.warning("Zrestartowano aktora " + brokenWorker.path().name());
		}
    }
	
	/** Odbierz i wyślij wiadomość */
	@Override
	public Receive createReceive()
	{
		return receiveBuilder()
			.match(WorkOrderMsg.class, order -> 
			{
				parent = getSender();
				dispatchSubworkers(order.getNumberOfTopics(), order.getNumberOfSyncs(), order.getHistograms());
			})
			.match(SyncMsg.class, sync -> 
			{
				syncTables(sync, getSender());
			})
			.match(Terminated.class, this::restartWorker)
			.build();
	}

}
