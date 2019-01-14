import actor.WorkManager;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import message.StartWorkMsg;

public class Main {

    public static void main(String[] args) {
        int numberOfWorkers = 10;
        final ActorSystem system = ActorSystem.create("DocumentSimilarity");
        final ActorRef workManagerActor = system.actorOf(WorkManager.props(numberOfWorkers), "Manager");
        workManagerActor.tell(
                new StartWorkMsg("src/main/java/ksiazki/urls.txt"),
                ActorRef.noSender());

//        String filePath = "C:\\Users\\Karola\\Desktop\\WordSimilarity_kopia\\src\\main\\java\\ksiazki\\urls.txt";
//        UrlLoader ul = new UrlLoader(filePath);
//        String[] pages = new WikiDownloader(ul.getUrls()).getPages();

//       TODO przenieść to poniżej do metody
        //Master
//        przekazuje secondary terms i document(strone)
//        TextPreprocessor textPreprocessor = new TextPreprocessor();
//        List<String[]> listOfDocumentsTerms = createLisOfDocumentTerms(pages, textPreprocessor);
//        Vectorizer vectorizer = new Vectorizer(listOfDocumentsTerms);
//        ArrayList<String> terms = vectorizer.getTerms();

        //Secondary dostaje dokument i termsy, zwraca wektor, który wyliczył
//        TextPreprocessor textPreprocessor = new TextPreprocessor();
//        String[] tokens = textPreprocessor.getPreparedTokens(document);
//        Vectorizer vectorizer = new Vectorizer(listOfDocumentsTerms);
//        RealVector v = vectorizer.createOccurrenceVector(tokens);
//        return v;

        //Master
//        zbiera od wszytskich agentów wektory
//        List<RealVector> vectors = new ArrayList<>();
//            vectors.add(v); dla kazdego otrzymanego wektora

//        List<RealVector> vectors = new ArrayList<>();
//        for (String chapter : pages) {
//            String[] tokens = textPreprocessor.getPreparedTokens(chapter);
//            RealVector v = vectorizer.createOccurrenceVector(tokens);
//            vectors.add(v);
//        }
//
//        //MAster
////        na podstawie wektorów tworzy macierz wystapien i macierz reprezentujaca slowa
//        RealMatrix counts = vectorizer.getCountMatrix(vectors);
//        LSI lsi = new LSI(counts, 7, terms);
//        lsi.performSingularValueDecomposition();
//        lsi.wordRepresentation();
//
//        //agenty moga sie podzielic szukaniem roznicy ewentualnie, aby dac im po jakas część słow, a potem polaczyc listy wynikowe
//        TestingWordsConverter testingWordsConverter = new TestingWordsConverter();
//        List<String[]> testingData = testingWordsConverter.getParsedTestingData();
//        ArrayList<Double> diff = lsi.getDifferences(testingData);
//
//
//        //wypisanie sredniej, oraz ilosci elementów w podanej klasie
//        showAverage(diff);
//        ResultEvaluator evaluator = new ResultEvaluator();
//        evaluator.showEvaluationResults(diff, QualityMeasureEnum.GREAT);
    }


//    private static void showAverage(ArrayList<Double> diff) {
//        Double average = diff.stream().mapToDouble(val -> val).average().orElse(0.0);
//        System.out.println("Average : " + average);
//    }
}
