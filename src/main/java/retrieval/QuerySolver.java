package retrieval;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import query.QueryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.util.Scanner;

/**
 * @author Dan Graur 4/6/2018
 */
public class QuerySolver {
    /**
     * The analyzer
     */
    private static StandardAnalyzer analyzer = new StandardAnalyzer();

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
     * Class constructor
     *
     * @param queryStream the source of the query
     * @param targetField the target field of the query
     * @param pathToIndex the path to where the index is located
     * @param queryBuilder the query builder
     * @param similarity the similarity of the query search
     */
    public QuerySolver(InputStream queryStream, String targetField, String pathToIndex, QueryBuilder queryBuilder, Similarity similarity) {
        this.scanner = new Scanner(queryStream);
        this.targetField = targetField;
        this.pathToIndex = pathToIndex;
        this.queryBuilder = queryBuilder;
        this.similarity = similarity;
    }

    public String getTargetField() {
        return targetField;
    }

    public String getPathToIndex() {
        return pathToIndex;
    }

    /**
     * Answer a set of queries until 'q' is pressed
     */
    public void answerQueries(int resultNumber) {
        IndexSearcher searcher = null;

        try {
            /* Compute the searcher */
            searcher = createSearcher();

            /* Set the similarity of the searcher */
            searcher.setSimilarity(similarity);

            /* Query parser */
            QueryParser parser = new QueryParser(targetField, analyzer);

            /* Read queries, and process them until there the user cancels the operation */
            String q;

            do {
                System.out.print("> ");
                q = scanner.nextLine();
            } while(q.isEmpty());

            while(!q.equalsIgnoreCase("q")) {

                TopScoreDocCollector collector = TopScoreDocCollector.create(resultNumber);
                Query query = parser.parse(q);// new QueryParser(searchedField, analyzer).parse(q);
                searcher.search(queryBuilder.buildQuery(targetField, q), collector);

                for (ScoreDoc scoreDoc : collector.topDocs().scoreDocs) {
                    Document doc = searcher.doc(scoreDoc.doc);

                    System.out.println("The score: " + scoreDoc.score + "\n" + doc.getField(targetField));
                }


                do {
                    System.out.print("> ");
                    q = scanner.nextLine();
                } while(q.isEmpty());
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        } finally {
            try {
                searcher.getIndexReader().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void answerQuery(String q, int resultNumber) throws IOException, ParseException {
        /* Compute the searcher */
        IndexSearcher searcher = createSearcher();

        /* Query parser */
        QueryParser parser = new QueryParser(targetField, analyzer);

        TopScoreDocCollector collector = TopScoreDocCollector.create(resultNumber);
        Query query = parser.parse(scanner.nextLine());
        searcher.search(query, collector);

        for (ScoreDoc scoreDoc : collector.topDocs().scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);

            System.out.println("The score: " + scoreDoc.score + "\n" + doc.getField(targetField));
        }

        searcher.getIndexReader().close();
    }

    private IndexSearcher createSearcher() throws IOException {
        return new IndexSearcher(DirectoryReader.open(FSDirectory.open(FileSystems.getDefault().getPath(pathToIndex))));
    }
}
