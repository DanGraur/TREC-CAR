package retrieval;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @author Dan Graur 4/4/2018
 */
public class IndexingTest {
    /**
     * Holds the paragraphs for indexing
     */
    private List<String> paragraphs;

    /**
     * The file indexer
     */
    private IndexWriter indexer;

    /**
     * The analyzer
     */
    private static StandardAnalyzer analyzer = new StandardAnalyzer();

    /**
     * Class Constructor
     */
    public IndexingTest() {
        paragraphs = new ArrayList<>();
    }

    /**
     * Coordinating method, used to
     */
    private void start() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("########################### START ###########################");

        /* Get the paragraphs */
        System.out.println("Path to the file containing the paragraphs: ");
        String pathToFile = scanner.nextLine();
        pathToFile = "./data/paragraphs.txt";
        paragraphs = getParagraphs(pathToFile);

        /* Index the paragraphs */
        System.out.println("Path to the file containing the index: ");
        String pathToIndex = scanner.nextLine();
        pathToIndex = "./index";
        indexParagraphs(pathToIndex);

        /* Answer queries */
        System.out.println("Please input queries: ");
        answerQueries(pathToIndex, scanner , "paragraph");

        /* Delete the index, as it will continue to increase in size in the next runs */
        try {
            FileUtils.deleteDirectory(new File(pathToIndex));
        } catch (IOException e) {
            System.err.println("Unable to delete the index directory: " + pathToIndex + "\nError: " + e.getMessage());
        }

        System.out.println("########################### END ###########################");
    }

    /**
     * Asnwer a set of queries
     *
     * @param pathToIndex path to the index directory
     */
    private void answerQueries(String pathToIndex, Scanner scanner, String searchedField) {
        IndexReader reader = null;

        try {
            /* Search elements */
            reader = DirectoryReader.open(FSDirectory.open(FileSystems.getDefault().getPath(pathToIndex)));
            IndexSearcher searcher = new IndexSearcher(reader);

            /* Sanity Check: Ensure the number of documents in the index DB is the same */
//            System.out.println("The number of docs is: " + reader.numDocs());
//            System.out.println("The max number of docs is: " + reader.maxDoc());
//            System.out.println("The number of deleted docs is: " + reader.numDeletedDocs());
//
//            for (int i = 0; i < reader.maxDoc(); i++) {
//                Document doc = reader.document(i);
//                String contents = doc.get(searchedField);
//
//                System.out.println(i + "#: " + contents);
//            }


//            TopScoreDocCollector collector = TopScoreDocCollector.create(5);

            /* Query parser */
            QueryParser parser = new QueryParser(searchedField, analyzer);

            /* Read queries, and process them until there the user cancels the operation */
            System.out.print("> ");
            String q = scanner.nextLine();

            while(!q.equalsIgnoreCase("q")) {

                TopScoreDocCollector collector = TopScoreDocCollector.create(5);
                Query query = parser.parse(q);// new QueryParser(searchedField, analyzer).parse(q);
                searcher.search(query, collector);

                for (ScoreDoc scoreDoc : collector.topDocs().scoreDocs) {
                    Document doc = searcher.doc(scoreDoc.doc);

                    System.out.println("The score: " + scoreDoc.score + "\n" + doc.getField(searchedField));
                }


                System.out.print("> ");
                q = scanner.nextLine();
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get the paragraphs of a file
     *
     * @param filePath the path to the file
     * @return a list of the file's paragraphs
     */
    public static List<String> getParagraphs(String filePath) {
        List<String> paraList = new ArrayList<>();
        File theFile = new File(filePath);

        if (theFile.isFile()) {
            try(BufferedReader reader = new BufferedReader(new FileReader(theFile))) {

                /* Add the paragraphs, but avoid adding empty lines */
                String line;
                while((line = reader.readLine()) != null)
                    if (!line.isEmpty())
                        paraList.add(line);

            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Encountered an IO Error: " + e.getMessage());
            }

        } else {
            System.err.println("The supplied path does not point to a file");
        }

        /* Return the paragraphs */
        return paraList;
    }

    /**
     * Index the paragraphs (a.k.a. documents) located in the paragraph list
     *
     * @param pathToIndex the dir path to the
     */
    private void indexParagraphs(String pathToIndex) {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        
        try {
            FSDirectory dir = FSDirectory.open(FileSystems.getDefault().getPath(pathToIndex));
            indexer = new IndexWriter(dir, config);

            for (String paragraph : paragraphs) {
                Document doc = new Document();

                doc.add(new TextField("paragraph", paragraph, Field.Store.YES));

                indexer.addDocument(doc);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                indexer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        IndexingTest obj = new IndexingTest();

        obj.start();
    }


}
