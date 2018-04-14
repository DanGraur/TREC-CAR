package retrieval;

import co.nstant.in.cbor.CborException;
import edu.unh.cs.treccar.Data;
import edu.unh.cs.treccar.read_data.DeserializeData;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;

/**
 * @author Dan Graur 4/5/2018
 */
public class IndexCreator {
    /**
     * The file indexer
     */
    private IndexWriter indexer;

    /**
     * The analyzer
     */
    private Analyzer analyzer;

    /**
     *  Path to the index directory
     */
    private String pathToIndex;

    /**
     *  Path to the file containing the paragraphs
     */
    private String pathToParagraphFile;

    public IndexCreator(String pathToParagraphFile, String pathToIndex, Analyzer analyzer) {
        this.analyzer = analyzer;
        this.pathToParagraphFile = pathToParagraphFile;
        this.pathToIndex = pathToIndex;
    }

    public IndexCreator(String pathToParagraphFile,  Analyzer analyzer) {
        this.analyzer = analyzer;
        this.pathToParagraphFile = pathToParagraphFile;
        this.pathToIndex = "./index";
    }

    public IndexCreator(String pathToParagraphFile, String pathToIndex) {
        this.analyzer = new StandardAnalyzer();
        this.pathToParagraphFile = pathToParagraphFile;
        this.pathToIndex = pathToIndex;
    }

    public IndexCreator(String pathToParagraphFile) {
        this.analyzer = new StandardAnalyzer();
        this.pathToParagraphFile = pathToParagraphFile;
        this.pathToIndex = "./index";
    }

    public IndexWriter getIndexer() {
        return indexer;
    }

    /**
     * Create an index directory for the paragraphs specified in the source file
     *
     * @throws IOException Thrown when one cannot open a file / directory
     * @throws CborException This exception is never really thrown
     */
    public void createIndex() throws IOException, CborException {
        /* Open here to insure that if indeed the directory cannot be created, the program will not move any further and will throw an exception */
        FSDirectory dir = FSDirectory.open(FileSystems.getDefault().getPath(pathToIndex));

        /* Create the indexer */
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        indexer = new IndexWriter(dir, config);

        /* Open the paragraph file here, as to ensure that the execution does not proceed if this file cannot be opened */
        final FileInputStream fileInputStream = new FileInputStream(new File(pathToParagraphFile));

        /* Read all the paragraphs, and index them */
        for(Data.Paragraph p: DeserializeData.iterableParagraphs(fileInputStream)) {
            Document doc = new Document();

            /* Index the paragraph field and the id of the paragraph (we'll need the latter later for checking against the ground truth) */
            /* Create a custom field which will store the term vectors (should be useful for the RMs) */
            FieldType termVectorCustomFType = new FieldType(TextField.TYPE_STORED);
            termVectorCustomFType.setStoreTermVectors(true);
            termVectorCustomFType.setStored(true);
            Field paraField = new Field("paragraph", p.getTextOnly(), termVectorCustomFType);

            /* Add the field(s): paragraph, and id */
            doc.add(paraField);
//            doc.add(new TextField("paragraph", p.getTextOnly(), Field.Store.YES));
            doc.add(new TextField("id", p.getParaId(), Field.Store.YES));

            indexer.addDocument(doc);
        }

        try {
            indexer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
