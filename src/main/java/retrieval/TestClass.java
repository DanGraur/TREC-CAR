package retrieval;

import co.nstant.in.cbor.CborException;
import eval.Evaluator;
import javafx.util.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.benchmark.quality.QualityStats;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import query.QueryBuilder;
import query.ReadQRels;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.similarities.BM25Similarity;
import query.BinaryQueryBuilder;
import query.analyzer.CustomAnalyzer;
import query.analyzer.MyStopWords;
import query.TRECQuery;
import query.expansion.rm.RelevanceBasedLanguageModel;
import query.expansion.rocchio.Rocchio;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

/**
 * @author Dan Graur 4/6/2018
 */
public class TestClass {

    /**
     * This points to the QRel file which will be used towards gathering the ground truths
     */
    private static final String QREL_FILE = "./data_14/train.test200.cbor.hierarchical.qrels";
    /**
     * This points to the outline file which will be used towards extracting all the possible queries
     */
    private static final String OUTLINE_FILE = "./data_14/train.test200.cbor.outlines";
    /**
     * This points to the paragraphs file which will be used towards indexing
     */
    private static final String PARA_FILE = "./data_14/train.test200.cbor.paragraphs";

    public static void main(String[] args) throws IOException, CborException {

        Analyzer analyzer = new CustomAnalyzer(MyStopWords.stopWords);
        QueryBuilder queryBuilder = new BinaryQueryBuilder(analyzer, 128);
        String targetField = "paragraph";

        String pathToFile = "./data_14/train.test200.cbor.paragraphs";
        String pathToIndex = "./index";

        /* Create an index and stem the words */
//        new IndexCreator(pathToFile, pathToIndex, analyzer.createIndex();

        QuerySolver querySolver = new QuerySolver(
                System.in,
                targetField,
                pathToIndex,
//                new BinaryQueryBuilder(new StandardAnalyzer(MyStopWords.stopWords), 128),
                queryBuilder,
//                new ClassicSimilarity(), // Classic similarity is in fact TF-IDF with cosine
                new BM25Similarity(), // BM25 Similarity
//                new LMDirichletSimilarity(),
                "id",
//                new Rocchio(1f, 0.8f, 10, 5, targetField, analyzer, queryBuilder)
                new RelevanceBasedLanguageModel(10, 5, targetField, analyzer, queryBuilder, 0.7f, 0.7f)

        );

        querySolver.initiateSolver();

        Map<TRECQuery, Set<String>> groundTruths = ReadQRels.readGroundTruths(QREL_FILE, OUTLINE_FILE);

        Evaluator eval = new Evaluator(querySolver, groundTruths);
        Pair resPair = eval.evaluate();
        QualityStats qs = (QualityStats) resPair.getKey();
        double rPrecAvg = (double) resPair.getValue();

        querySolver.terminateSovler();


        System.out.println(
                "The big one: " +
                "\nAverage Precision: " + qs.getAvp() +
                "\nR-Prec: " + rPrecAvg +
                "\nMRR: " + qs.getMRR()
        );

        qs.log("Eval Summary", 2, new PrintWriter(System.out), null);

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
