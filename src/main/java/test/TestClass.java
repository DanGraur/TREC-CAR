package test;

import eval.Evaluator;
import eval.ReadQRels;
import javafx.util.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.benchmark.quality.QualityStats;
import org.apache.lucene.search.similarities.BM25Similarity;
import query.BinaryQueryBuilder;
import query.QueryBuilder;
import query.QuerySolver;
import query.TRECQuery;
import query.analyzer.CustomAnalyzer;
import query.analyzer.MyStopWords;
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
    /**
     * This points to the paragraphs file which will be used towards indexing
     */
    private static final String PATH_TO_INDEX = "./index";
    /**
     * Verbose output
     */
    private static final boolean VERBOSE = false;
    /**
     * The default target search field
     */
    private static final String TARGET_FIELD =  "paragraph";
    /**
     * The id field
     */
    private static final String ID_FIELD =  "id";

    public static void main(String[] args) throws IOException {
        Analyzer analyzer = new CustomAnalyzer(MyStopWords.stopWords);
        QueryBuilder queryBuilder = new BinaryQueryBuilder(analyzer, 128);

        /* Create an index and stem the words */
//       new IndexCreator(PARA_FILE, PATH_TO_INDEX, analyzer).createIndex();


        /* Create the query solver, which will be used for evaluation */
        QuerySolver querySolver = new QuerySolver(
                System.in,
                TARGET_FIELD,
                PATH_TO_INDEX,
//                new BinaryQueryBuilder(new StandardAnalyzer(MyStopWords.stopWords), 128),
                queryBuilder,
//                new ClassicSimilarity(), // Classic similarity is in fact TF-IDF with cosine
                new BM25Similarity(), // BM25 Similarity
//                new LMDirichletSimilarity(), // Dirichlet Similarity
                ID_FIELD,
//                null,
                new Rocchio(1f, 0.8f, 10, 5, TARGET_FIELD, analyzer, queryBuilder),
//                new RelevanceBasedLanguageModel(10, 5, TARGET_FIELD, analyzer, queryBuilder, 0.7f, 0.7f),
                VERBOSE

        );

        /* Initiate the query solver */
        querySolver.initiateSolver();

        /* Read and compile together the raw queries, and their ground truths */
        Map<TRECQuery, Set<String>> groundTruths = ReadQRels.readGroundTruths(QREL_FILE, OUTLINE_FILE);

        /* Create the evaluation object, and proceed with the evaluation */
        Evaluator eval = new Evaluator(querySolver, groundTruths, VERBOSE);

        /* Get the final results */
        Pair<QualityStats, Double> resPair = eval.evaluate();
        QualityStats qs = resPair.getKey();
        double rPrecAvg = resPair.getValue();

        /* Terminate the query solver */
        querySolver.terminateSovler();

        /* Print the final results */
        System.out.println(
                "\n\nFINAL RESULTS: " +
                "\nAverage Precision: " + qs.getAvp() +
                "\nR-Prec: " + rPrecAvg +
                "\nMRR: " + qs.getMRR()
        );

        /* Log the results */
        qs.log("Eval Summary", 2, new PrintWriter(System.out), null);
    }
}
