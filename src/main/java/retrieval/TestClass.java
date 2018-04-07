package retrieval;

import co.nstant.in.cbor.CborException;
import edu.unh.cs.treccar.read_data.ReadQRels;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.BM25Similarity;
import query.BinaryQueryBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Dan Graur 4/6/2018
 */
public class TestClass {

    public static void main(String[] args) throws IOException, CborException {

        String pathToFile = "./data_14/train.test200.cbor.paragraphs";
        String pathToIndex = "./index";

//        new IndexCreator(pathToFile, pathToIndex).createIndex();
        QuerySolver querySolver = new QuerySolver(
                System.in,
                "paragraph",
                pathToIndex,
                new BinaryQueryBuilder(new StandardAnalyzer(), 128),
                new BM25Similarity(),
                "id"
        );


        Map<String, List<String>> res = ReadQRels.readQRels("./data_14/train.test200.cbor.article.qrels");

        try {

            /* Initiate the solver */
            querySolver.initiateSolver();

            /* Solve some queries */
//            querySolver.answerQueries(10);
            List<String> qRes = querySolver.answerQuery("Australian Astronomical Observatory", 10);

            System.out.println("The number of initial elements: " + qRes.size());

            qRes.retainAll(res.get("Australian Astronomical Observatory"));

            System.out.println("The number of matching elements: " + qRes.size());

            // TODO: try to not remove the %20 and do them normally

            /* Terminate the solver */
            querySolver.terminateSovler();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
