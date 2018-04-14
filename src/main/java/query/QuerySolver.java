package query;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import query.expansion.Expander;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.util.*;

/**
 * One must first create an object of this class. The one should initiate the solver
 * and then one can submit queries for soling. Finally, the solver needs to be closed.
 *
 * @author Dan Graur 4/6/2018
 */
public class QuerySolver {
    /**
     * The field indicating the id
     */
    private String idField;

    /**
     * Std.in reader
     */
    private Scanner scanner;

    /**
     * The default field for the query
     */
    private String targetField;

    /**
     * The path to the index directory
     */
    private String pathToIndex;

    /**
     * The query builder
     */
    private QueryBuilder queryBuilder;

    /**
     * The similarity of the query
     */
    private Similarity similarity;

    /**
     * The index searcher
     */
    private IndexSearcher searcher;

    /**
     * Query expanding strategy
     */
    private Expander queryExpander;

    /**
     * Specifies if it should be verbose or not

     */
    private boolean verbose;

    /**
     * Class constructor
     *
     * @param queryStream the source of the query
     * @param targetField the target field of the query
     * @param pathToIndex the path to where the index is located
     * @param queryBuilder the query builder
     * @param similarity the similarity of the query search
     * @param queryExpander the query expanding strategy
     */
    public QuerySolver(InputStream queryStream,
                       String targetField,
                       String pathToIndex,
                       QueryBuilder queryBuilder,
                       Similarity similarity,
                       Expander queryExpander,
                       boolean verbose) {
        this.scanner = new Scanner(queryStream);
        this.targetField = targetField;
        this.pathToIndex = pathToIndex;
        this.queryBuilder = queryBuilder;
        this.similarity = similarity;
        this.idField = "";
        this.queryExpander = queryExpander;
        this.verbose = verbose;
    }

    /**
     * Class constructor
     *
     * @param queryStream the source of the query
     * @param targetField the target field of the query
     * @param pathToIndex the path to where the index is located
     * @param queryBuilder the query builder
     * @param similarity the similarity of the query search
     */
    public QuerySolver(InputStream queryStream,
                       String targetField,
                       String pathToIndex,
                       QueryBuilder queryBuilder,
                       Similarity similarity,
                       boolean verbose) {
        this.scanner = new Scanner(queryStream);
        this.targetField = targetField;
        this.pathToIndex = pathToIndex;
        this.queryBuilder = queryBuilder;
        this.similarity = similarity;
        this.idField = "";
        this.verbose = verbose;
    }

    /**
     * Class constructor
     *
     * @param queryStream the source of the query
     * @param targetField the target field of the query
     * @param pathToIndex the path to where the index is located
     * @param queryBuilder the query builder
     * @param similarity the similarity of the query search
     */
    public QuerySolver(InputStream queryStream,
                       String targetField,
                       String pathToIndex,
                       QueryBuilder queryBuilder,
                       Similarity similarity,
                       String idField,
                       boolean verbose) {
        this.scanner = new Scanner(queryStream);
        this.targetField = targetField;
        this.pathToIndex = pathToIndex;
        this.queryBuilder = queryBuilder;
        this.similarity = similarity;
        this.idField = idField;
        this.verbose = verbose;
    }

    /**
     * Class constructor
     *
     * @param queryStream the source of the query
     * @param targetField the target field of the query
     * @param pathToIndex the path to where the index is located
     * @param queryBuilder the query builder
     * @param similarity the similarity of the query search
     * @param queryExpander the query expanding strategy
     */
    public QuerySolver(InputStream queryStream,
                       String targetField,
                       String pathToIndex,
                       QueryBuilder queryBuilder,
                       Similarity similarity,
                       String idField,
                       Expander queryExpander,
                       boolean verbose) {
        this.scanner = new Scanner(queryStream);
        this.targetField = targetField;
        this.pathToIndex = pathToIndex;
        this.queryBuilder = queryBuilder;
        this.similarity = similarity;
        this.idField = idField;
        this.queryExpander = queryExpander;
        this.verbose = verbose;
    }

    public String getTargetField() {
        return targetField;
    }

    public void setTargetField(String targetField) {
        this.targetField = targetField;
    }

    public String getPathToIndex() {
        return pathToIndex;
    }

    public String getIdField() {
        return idField;
    }

    public void setIdField(String idField) {
        this.idField = idField;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Initiates the query solver
     *
     * @throws IOException if unable to create the Index Searcher
     */
    public void initiateSolver() throws IOException {
        /* Compute the searcher */
        searcher = createSearcher();

        /* Set the similarity of the searcher */
        searcher.setSimilarity(similarity);
    }

    /**
     * Solves a query
     *
     * @param q the query string to solve
     * @param resultNumber the number of results to be returned
     * @throws IOException when unable to build the query
     * @return a list of paragraph ids
     */
    public List<String> answerQuery(String q, int resultNumber) throws IOException {
        Query query = queryBuilder.buildQuery(targetField, q);
        List<String> res = new ArrayList<>();
        TopScoreDocCollector collector = TopScoreDocCollector.create(resultNumber);
        searcher.search(query, collector);

//        System.out.println(">>>>>? " + query.toString());

        if (verbose)
            System.out.println("> The Raw Query: " + q);

        /* If there is a query expander in place, then expand the query by first gathering some relevant documents */
        if (queryExpander != null) {
            List<Document> relevantDocuments = new ArrayList<>();

            for (ScoreDoc scoreDoc : collector.topDocs().scoreDocs) {
                relevantDocuments.add(searcher.doc(scoreDoc.doc));
//                System.out.println(searcher.getIndexReader().getTermVector(scoreDoc.doc, targetField));
            }

            /* Send the query for expansion; first make sure to unparse it, as to remove Lucene specific additions */
            query = queryExpander.expand(unParseQuery(query), relevantDocuments);
            collector = TopScoreDocCollector.create(resultNumber);
            searcher.search(query, collector);
        }

        /* Perform the true query */
        for (ScoreDoc scoreDoc : collector.topDocs().scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);

            if (!idField.isEmpty()) {
                if (verbose)
                    System.out.println("\t>> Paragraph ID: " + doc.getField(idField).stringValue());

                res.add(doc.getField(idField).stringValue());
            }

            if (verbose)
                System.out.println("\t>> Raw Paragraph: " + doc.getField(targetField).stringValue() + "\n\t>> Match Score: " + scoreDoc.score + '\n');
        }

        return res;
    }

    /**
     * Terminates the solver
     *
     * @throws IOException if unable to close the index reader
     */
    public void terminateSovler() throws IOException {
        searcher.getIndexReader().close();
    }

    /**
     * Answer a set of queries until 'q' is pressed
     */
    public Map<String, List<String>> answerQueries(int resultNumber) {
        Map<String, List<String>> res = new HashMap<>();

        try {
            String q;

            do {
                System.out.print("> ");
                q = scanner.nextLine();
            } while(q.isEmpty());

            while(!q.equalsIgnoreCase("q")) {

                /* Get the results and store them for later return */
                res.put(
                        q,
                        answerQuery(q, resultNumber)
                );

                do {
                    System.out.print("> ");
                    q = scanner.nextLine();
                } while(q.isEmpty());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return res;
    }

    private IndexSearcher createSearcher() throws IOException {
        return new IndexSearcher(DirectoryReader.open(FSDirectory.open(FileSystems.getDefault().getPath(pathToIndex))));
    }

    /**
     * Get the analyzed tokens of the analyzed query (i.e. transform something like paragraph:<term> into <term>)
     *
     * @param query the query to be reparsed
     * @return the query's (already analyzed) tokens
     */
    private String[] unParseQuery(Query query) {
        /* Let's formulate it nicely using streaming and 'functional' programming */
        return Arrays
                .stream(query.toString().split("\\s+"))
                .map((String bigToken) -> {
                    String[] tokens = bigToken.split(":");
                    if (tokens.length > 1)
                        return tokens[1];
                    return "";
                })
                .filter((String element) -> !element.equals(""))
                .toArray(String[]::new);
    }
}
