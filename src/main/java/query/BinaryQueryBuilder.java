package query;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.io.IOException;

/**
 * @author Dan Graur 4/7/2018
 */
final public class BinaryQueryBuilder extends QueryBuilder {
    /**
     * The constructor
     *
     * @param analyzer the analyzer used by the query builder
     */
    protected BinaryQueryBuilder(Analyzer analyzer) {
        super(analyzer);
    }

    public BinaryQueryBuilder(Analyzer analyzer, int maxTokens) {
        super(analyzer, maxTokens);
    }

    @Override
    public Query buildQuery(String targetField, String queryString) throws IOException {
        TokenStream stream = analyzer.tokenStream(targetField, queryString);
        stream.reset();
        tokens.clear();

        while(stream.incrementToken())
            tokens.add(stream.getAttribute(CharTermAttribute.class).toString());

        stream.end();
        stream.close();

        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        for (String token : tokens)
            builder.add(new TermQuery(new Term(targetField, token)), BooleanClause.Occur.SHOULD);

        return builder.build();

    }
}
