package query;

import java.util.Objects;

/**
 * @author Dan Graur 4/9/2018
 */
public class TRECQuery {
    /**
     * An ID of the query, which allows one to identify its relevant documents
     */
    private String queryId;
    /**
     * The plain query, which is actually used towards building the Lucene Query
     */
    private String plainQuery;

    public TRECQuery(String queryId, String plainQuery) {
        this.queryId = queryId;
        this.plainQuery = plainQuery;
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public String getPlainQuery() {
        return plainQuery;
    }

    public void setPlainQuery(String plainQuery) {
        this.plainQuery = plainQuery;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TRECQuery trecQuery = (TRECQuery) o;
        return Objects.equals(queryId, trecQuery.queryId) &&
                Objects.equals(plainQuery, trecQuery.plainQuery);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queryId, plainQuery);
    }

    @Override
    public String toString() {
        return "TRECQuery{" +
                "queryId='" + queryId + '\'' +
                ", plainQuery='" + plainQuery + '\'' +
                '}';
    }
}
