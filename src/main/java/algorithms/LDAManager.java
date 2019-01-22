package algorithms;
import akka.actor.AbstractActor;
import akka.event.LoggingAdapter;
import akka.actor.Props;
import message.WorkOrderMsg;
import akka.event.Logging;

/** Zarządza i rozdaje aktorów algorytmu LDA */
public class LDAManager extends AbstractActor
{
	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	/** Konstruktor */
	private LDAManager()
	{
		log.info("Manager LDA wystartował");
	}
	
	/** Fabryka do konstrukcji z zewnątrz */
	public static Props props()
	{
		return Props.create(LDAManager.class, LDAManager::new);
	}
	
	/** Rozstaw pracowników **/
	private void dispatchSubworkers(int numberOfTopics, int numberOfSyncs)
	{
		log.info("Rozstawianie pracowników na " + numberOfTopics + " tematów przy " + numberOfSyncs + " synchronizajach");
	}
	
	/** Odbierz i wyślij wiadomość */
	@Override
	public Receive createReceive()
	{
		return receiveBuilder()
			.match(WorkOrderMsg.class, order -> 
			{
				dispatchSubworkers(order.getNumberOfTopics(), order.getNumberOfSyncs());
			})
			.build();
	}

}
