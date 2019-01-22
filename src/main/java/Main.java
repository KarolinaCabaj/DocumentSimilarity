import actor.WorkManager;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import message.StartWorkMsg;

public class Main {

    public static void main(String[] args) {
        int numberOfWorkers = 10;
        int numberOfTopics = 30;
        final ActorSystem system = ActorSystem.create("DocumentSimilarity");
        final ActorRef workManagerActor = system.actorOf(WorkManager.props(numberOfWorkers, numberOfTopics), "Manager");
        workManagerActor.tell(
                new StartWorkMsg("src/main/java/ksiazki/urls.txt"),
                ActorRef.noSender());
    }
}
