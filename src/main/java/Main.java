import actor.WorkManager;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import message.StartWorkMsg;

public class Main {

    public static void main(String[] args) {
        int numberOfWorkers = 10;
        final ActorSystem system = ActorSystem.create("DocumentSimilarity");
        final ActorRef workManagerActor = system.actorOf(WorkManager.props(numberOfWorkers), "Manager");
        workManagerActor.tell(new StartWorkMsg(), ActorRef.noSender());
        // TODO przenieść to poniżej do metody
//        String path = "D:\\studia\\semestr4\\WEDT\\WordSimilarity\\src\\main\\java\\ksiazki\\The Fault in Our Stars ( PDFDrive.com ).pdf";
//        BookReader bookReader = new BookReader(path, 10);
//        bookReader.readBook();
//        bookReader.divideByChapters(); //todo obecnie pierwszy rozdzial jest pusty w tej obrobce
//        TermDictionary termDictionary = new TermDictionary();
//
//        List<String> chapters = Arrays.asList(bookReader.getChapters());
//        TextPreprocessor textPreprocessor = new TextPreprocessor();
//        for (String document : bookReader.getChapters()) {
//            String[]tokens = textPreprocessor.getPreparedTerms(document);//tokenizer.getTokens(document);
//            termDictionary.addTerms(tokens);
//        }
//
//        Vectorizer vectorizer = new Vectorizer(termDictionary, textPreprocessor, false);
//        RealMatrix counts = vectorizer.getCountMatrix(chapters);
//        double[][] matrixToPrint = counts.getData();
//        for (double[] row : matrixToPrint){ //todo kazda kolumna to document,
//            System.out.println(Arrays.toString(row)); //todo: cos dziwnego bo trzecie slowo wyjatkowo czesto
//        }
    }
}

