package eval;

import javafx.util.Pair;
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
    /**
     * Specifies whether this evaluator should be verbose
     */
    private boolean verbose;

    public Evaluator(QuerySolver solver, Map<TRECQuery, Set<String>> groundTruths, boolean verbose) {
        this.solver = solver;
        this.groundTruths = groundTruths;
        this.verbose = verbose;
    }

    /**
     * Run the evaluation function on the provided data
     *
     * @return a QualityStats object, which aggregates the evaluation results
     */
    public Pair<QualityStats, Double> evaluate() {
        double rPrecAvg = 0.0;
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

            /* Precision @ optimalResults.size() is in fact R-Prec */
            int maxGoodRes = optimalResults.size() == 0 ? 1 : (optimalResults.size() > 20 ? 20 : optimalResults.size());
            rPrecAvg += qs.getPrecisionAt(maxGoodRes);

            if (verbose)
                System.out.println("\t>>> Average Precision: " + qs.getAvp() + "\n\t>>> R-Prec: " + qs.getPrecisionAt(maxGoodRes) + "\n\t>>> MRR: " + qs.getMRR() + '\n');
        }

        int resSize = individualResults.size();

        return new Pair<>(QualityStats.average(individualResults.toArray(new QualityStats[resSize])), rPrecAvg / resSize);
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
