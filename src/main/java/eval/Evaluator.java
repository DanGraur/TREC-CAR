package eval;

import org.apache.lucene.benchmark.quality.QualityStats;
import query.TRECQuery;
import retrieval.QuerySolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dan Graur 4/10/2018
 */
public class Evaluator {
    /**
     * The query solver
     */
    private QuerySolver solver;
    /**
     * A map containing the set of TRECQuery objects (for the IDs and the queries themselves), and the set of relevant paragraph IDs
     */
    private Map<TRECQuery, Set<String>> groundTruths;

    public Evaluator(QuerySolver solver, Map<TRECQuery, Set<String>> groundTruths) {
        this.solver = solver;
        this.groundTruths = groundTruths;
    }

    /**
     * Run the evaluation function on the provided data
     *
     * @return a QualityStats object, which aggregates the evaluation results
     */
    public QualityStats evaluate() {
        List<QualityStats> individualResults = new ArrayList<>();

        for (TRECQuery trecQuery : groundTruths.keySet()) {
            Set<String> optimalResults = groundTruths.get(trecQuery);
            QualityStats qs = new QualityStats(optimalResults.size(), 1L);

            try {
                List<String> results = solver.answerQuery(trecQuery.getPlainQuery(), 5);

                for (int i = 0; i < results.size(); ++i)
                        qs.addResult(i + 1, optimalResults.contains(results.get(i)), 1L);
                
            } catch (IOException e) {
                e.printStackTrace();
            }

            individualResults.add(qs);

            System.out.println(qs.getPrecisionAt(5));
        }

        return QualityStats.average(individualResults.toArray(new QualityStats[individualResults.size()]));
    }
}
