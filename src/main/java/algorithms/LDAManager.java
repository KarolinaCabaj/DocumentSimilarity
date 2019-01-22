package algorithms;
import akka.actor.*;
import algorithms.LDAWorker;
import akka.actor.AbstractActor;
import akka.event.LoggingAdapter;
import akka.actor.Props;
import message.WorkOrderMsg;
import message.SyncMsg;
import akka.event.Logging;
import java.util.*;
import java.lang.*;

/** Zarządza i rozdaje aktorów algorytmu LDA */
public class LDAManager extends AbstractActor
{
	/** Logger */
	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	/** Lista aktorów */
	private List<ActorRef> workersList;
	
	/** Tablica Słowa ↔ Tematy, liczebności danego słowa w danym temacie */
	private List<double[]> wordTopicsTable;
	
	/** Sumy słów w danym temacie */
	private double[] topicsSums;
	
	/** Ilości synchronizacji */
	private Map<ActorRef, Integer> synchronizations;

	/** Ilość szukanych tematów */
	private int numberOfTopics;
	
	/** Ilość synchronizacji z każdym, potrzebna do zakończenia programu */
	private int numberOfSyncs;
	 
	/** Czy już nie odbiera wiadomości */
	boolean isFinished;
	
	/** Obiekt do synchronizacji */
	Object mutex = new Object();
	
	/** Konstruktor */
	private LDAManager()
	{
		log.info("Manager LDA zrodził się");
		workersList = new ArrayList<>();
		this.synchronizations = new HashMap<>();
	}
	
	/** Fabryka do konstrukcji z zewnątrz */
	public static Props props()
	{
		return Props.create(LDAManager.class, LDAManager::new);
	}
	
	/** Rozstaw pracowników **/
	private void dispatchSubworkers(int numberOfTopics, int numberOfSyncs, List<int[]> histograms)
	{
		final int numberOfWorkers = histograms.size();
		this.numberOfTopics = numberOfTopics;
		this.numberOfSyncs = numberOfSyncs;
		log.info("Rozstawianie " + numberOfWorkers + " pracowników na " + numberOfTopics + " tematów przy " + numberOfSyncs + " synchronizajach");
		
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
			synchronizations.put(worker, new Integer(0));
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
			actor.tell(new WorkOrderMsg(numberOfTopics, textOfIdsWithTopics, wordTopicsTable, topicsSums) , getSelf());
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
	
			for(Integer wordId : sync.wordTopicsTable.keySet())
			{
				double[] topicValues = sync.wordTopicsTable.get(wordId);
				//dla przysłanych słów, zaintegruj je, licząc średnią arytmetyczną
				for(int i = 0; i < topicValues.length; i++)
				{
					wordTopicsTable.get(wordId)[i] = (wordTopicsTable.get(wordId)[i] + topicValues[i]) / 2.0;
				}
				for(int i = 0; i < numberOfTopics; i++)
				{
					topicsSums[i] = (topicsSums[i] + sync.topicsSums[i]) / 2.0;
				}
				//zwiększ licznik dla tego aktora
				log.info("Synchronized " + sender + " times " + synchronizations.get(sender));
				synchronizations.put(sender, new Integer(synchronizations.get(sender).intValue() + 1));
			}
			
			//wstaw zaktualizowane wartości i wyślij z powrotem
			for(Integer wordId : sync.wordTopicsTable.keySet())
			{
				sync.wordTopicsTable.put(wordId, wordTopicsTable.get(wordId));
			}
			sync.topicsSums = topicsSums;
			sender.tell(sync, getSelf());
			
			//sprawdź, czy nie trzeba już zakończyć
			checkTermination();

		}
	}
	
	/** Sprawdza, czy nie trzeba zamknąć programu, bo zsynchronizował się z każdym wystarczającą ilość razy */
	private void checkTermination()
	{
		for(ActorRef actor : synchronizations.keySet())
		{
			if(synchronizations.get(actor).intValue() < numberOfSyncs)
			{
				return;
			}
		}
		
		for(ActorRef actor : workersList)
		{
			isFinished = true;
			
			
			getContext().unwatch(actor);
			actor.tell(PoisonPill.getInstance(), ActorRef.noSender());
		}
						
		//TODO
		//zwróć wynik
		log.info("LDA Manager zakończył pracę");
	}
	
	/** Odbierz i wyślij wiadomość */
	@Override
	public Receive createReceive()
	{
		return receiveBuilder()
			.match(WorkOrderMsg.class, order -> 
			{
				dispatchSubworkers(order.getNumberOfTopics(), order.getNumberOfSyncs(), order.getHistograms());
			})
			.match(SyncMsg.class, sync -> 
			{
				syncTables(sync, getSender());
			})
			.build();
	}

}
