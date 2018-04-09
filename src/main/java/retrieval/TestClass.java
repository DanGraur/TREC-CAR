package retrieval;

import co.nstant.in.cbor.CborException;
import eval.Evaluator;
import org.apache.lucene.benchmark.quality.QualityStats;
import query.ReadQRels;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.similarities.BM25Similarity;
import query.BinaryQueryBuilder;
import query.MyStopWords;
import query.TRECQuery;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dan Graur 4/6/2018
 */
public class TestClass {

    public static void main(String[] args) throws IOException, CborException {

        final String QREL_FILE = "./data_14/train.test200.cbor.hierarchical.qrels";
        final String OUTLINE_FILE = "./data_14/train.test200.cbor.outlines";

        String pathToFile = "./data_14/train.test200.cbor.paragraphs";
        String pathToIndex = "./index";

//        new IndexCreator(pathToFile, pathToIndex).createIndex();
        QuerySolver querySolver = new QuerySolver(
                System.in,
                "paragraph",
                pathToIndex,
                new BinaryQueryBuilder(new StandardAnalyzer(MyStopWords.stopWords), 128),
//                new ClassicSimilarity(), // Classic similarity is in fact TF-IDF
                new BM25Similarity(), // BM25 Similarity
                "id"
        );

        querySolver.initiateSolver();

        Map<TRECQuery, Set<String>> groundTruths = ReadQRels.readGroundTruths(QREL_FILE, OUTLINE_FILE);

        Evaluator eval = new Evaluator(querySolver, groundTruths);
        QualityStats qs = eval.evaluate();

        querySolver.terminateSovler();


        System.out.println("The big one: " + qs.getPrecisionAt(5));

//        try {
//
//            /* Initiate the solver */
//            querySolver.initiateSolver();
//
//            /* Solve some queries */
////            querySolver.answerQueries(10);
//            List<String> qRes = querySolver.answerQuery("Australian Astronomical Observatory", 10);
//
//            System.out.println("The number of initial elements: " + qRes.size());
//
//            qRes.retainAll(res.get("Australian Astronomical Observatory"));
//
//            System.out.println("The number of matching elements: " + qRes.size());
//
//            // TODO: try to not remove the %20 and do them normally
//
//            /* Terminate the solver */
//            querySolver.terminateSovler();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }
}
