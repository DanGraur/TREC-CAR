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
 * @author Dan Graur 4/9/2018
 */

public class RelevanceBasedLanguageModel implements Expander {
    public enum RMType {
        RM1, RM3
    }

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

    /**
     * Defines the extension algorithm to use
     */
    private RMType rmType;

    public RelevanceBasedLanguageModel(int termLimit,
                                       int documentLimit,
                                       String targetField,
                                       Analyzer analyzer,
                                       QueryBuilder queryBuilder,
                                       float lambda,
                                       float mixingLambda,
                                       RMType rmType) {
        this.termLimit = termLimit;
        this.targetField = targetField;
        this.queryBuilder = queryBuilder;
        this.rlm = new RLM(analyzer, documentLimit, termLimit, lambda, mixingLambda, targetField);
        this.rmType = rmType;
    }

    @Override
    public Query expand(String[] query, List<Document> relevantDocuments) throws IOException {
        /* Compute the P(Q|d) given the current set of relevant documents */
        rlm.setFeedbackStats(relevantDocuments, query);

        List<Map.Entry<String, WordProbability>> termMap;

        /* Choose which of the Relevance Model will be used for expansion */
        if (rmType == RMType.RM1)
            termMap = new ArrayList<>(rlm.RM1().entrySet());
        else
            termMap = new ArrayList<>(rlm.RM3(query).entrySet());

        /* Sort in descending order */
        termMap.sort(
                (Map.Entry<String, WordProbability> entry1, Map.Entry<String, WordProbability> entry2) ->
                        Float.compare(
                                entry2.getValue().p_w_given_R, entry1.getValue().p_w_given_R
                        )
        );

        for (Map.Entry<String, WordProbability> entry : termMap) {
            System.out.println(entry.getKey() + " " + entry.getValue().p_w_given_R);
        }

        System.out.println("\n");

        /* Build the query */
        StringBuilder queryString = new StringBuilder();

        for (Map.Entry<String, WordProbability> entry : termMap.subList(0, termMap.size() > (termLimit + 1) ? termLimit + 1 : termMap.size()))
            queryString.append(entry.getKey()).append(' ');

        return queryBuilder.buildQuery(targetField, queryString.toString());
    }
}