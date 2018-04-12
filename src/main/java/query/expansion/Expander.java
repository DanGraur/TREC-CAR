package query.expansion;

import org.apache.lucene.search.Query;

import java.io.IOException;
import java.util.List;

/**
 * @author Dan Graur 4/11/2018
 */
public interface Expander {

    /**
     * Expand the query
     *
     * @param query the query to be expanded
     * @param relevantDocuments the set of relevant documents, for the initial query
     * @return a new query, which expands on the initial query based on its relevant documents
     */
    Query expand(Query query, List<DocumentWrapper> relevantDocuments) throws IOException;
}
