/**
 * RM3: Complete;
 * Relevance Based Language Model with query mix.
 * References:
 *      1. Relevance Based Language Model - Victor Lavrenko - SIGIR-2001
 *      2. UMass at TREC 2004: Novelty and HARD - Nasreen Abdul-Jaleel - TREC-2004
 */
package query.expansion.rm;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;
import query.QueryBuilder;
import query.expansion.DocumentWrapper;
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
     * Maximal number of documents to be considered in the relevant index
     */
    private int documentLimit;
    /**
     * The target field for the search operation
     */
    private String targetField;
    /**
     * The analyzer used for creating the initial query and the indexing itself
     */
    private Analyzer analyzer;
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
        this.documentLimit = documentLimit;
        this.targetField = targetField;
        this.analyzer = analyzer;
        this.queryBuilder = queryBuilder;
        this.rlm = new RLM(analyzer, documentLimit, termLimit, lambda, mixingLambda, targetField);
    }

    @Override
    public Query expand(Query query, List<DocumentWrapper> relevantDocuments) throws IOException {
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