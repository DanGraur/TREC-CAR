package query.expansion.rm;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import query.QueryBuilder;
import query.expansion.Expander;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author dwaipayan
 */

public class RelevanceBasedLanguageModel implements Expander {
    /**
     * Maximal number of terms of the new expanded query
     */
    private int termLimit;
    /**
     * The target field for the search operation
     */
    private String targetField;
    /**
     * The query builder, used to build the Lucene query for the expanded plain query
     */
    private QueryBuilder queryBuilder;

    /**
     * The RML object, which actually implements the algorithms
     */
    private RLM rlm;

    public RelevanceBasedLanguageModel(int termLimit, int documentLimit, String targetField, Analyzer analyzer, QueryBuilder queryBuilder, float lambda, float mixingLambda) {
        this.termLimit = termLimit;
        this.targetField = targetField;
        this.queryBuilder = queryBuilder;
        this.rlm = new RLM(analyzer, documentLimit, termLimit, lambda, mixingLambda, targetField);
    }

    @Override
    public Query expand(Query query, List<Document> relevantDocuments) throws IOException {
        /* Compute the P(Q|d) given the current set of relevant documents */
        rlm.setFeedbackStats(relevantDocuments, query.toString().split("\\s+"));

        List<Map.Entry<String, WordProbability>> termMap = new ArrayList<>(rlm.RM3(query).entrySet()); //rlm.RM1();

        termMap.sort(
                (Map.Entry<String, WordProbability> entry1, Map.Entry<String, WordProbability> entry2) ->
                        Float.compare(
                                entry1.getValue().p_w_given_R, entry2.getValue().p_w_given_R
                        )
        );

        StringBuilder queryString = new StringBuilder();

        for (Map.Entry<String, WordProbability> entry : termMap.subList(0, termMap.size() > (termLimit + 1) ? termLimit + 1 : termMap.size()))
            queryString.append(' ').append(entry.getKey());

        return queryBuilder.buildQuery(targetField, queryString.toString());
    }
}