package query;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dan Graur 4/7/2018
 */
public abstract class QueryBuilder {
    /**
     * The query analyzer
     */
    protected Analyzer analyzer;
    /**
     * The query tokens
     */
    protected List<String> tokens;

    /**
     * The constructor
     *
     * @param analyzer the analyzer used by the query builder
     */
    protected QueryBuilder(Analyzer analyzer) {
        this.analyzer = analyzer;
        tokens = new ArrayList<>(128);
    }

    /**
     *
     * @param analyzer the analyzer used by the query builder
     * @param maxTokens the max number of tokens
     */
    public QueryBuilder(Analyzer analyzer, int maxTokens) {
        this.analyzer = analyzer;
        this.tokens = new ArrayList<>(maxTokens);
    }

    /**
     * Build a query, gven the queryString
     *
     * @param targetField the default search field in the index
     * @param queryString the string which represents the user supplied query
     * @return the system used query
     */
    public abstract Query buildQuery(String targetField, String queryString) throws IOException;
}
