package query.expansion.utils;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;
import java.util.*;

/**
 * @author Dan Graur 4/11/2018
 */
public class Utils {

    public static Map<String, Float> getTFIDF(Directory directory, Set<String> tokens, String targetField) throws IOException {
        /* Declare the Map */
        Map<String, Float> frequencyMap = new HashMap<>();

        /* Get the target field terms */
        IndexReader reader = DirectoryReader.open(directory);
        Fields fields = MultiFields.getFields(reader);
        Terms terms = fields.terms(targetField);
        TermsEnum termsEnum = terms.iterator();

        /* Get the number of documents */
        int docNumber = reader.numDocs();

        /* Declare the similarity */
        ClassicSimilarity similarity = new ClassicSimilarity();

        while (termsEnum.next() != null)
            if (tokens.contains(termsEnum.term().utf8ToString())) {
                Term term = new Term(targetField, termsEnum.term().utf8ToString());
                int docFreq = reader.docFreq(term);
                long termFreq = reader.totalTermFreq(term);

                /* Compute the TF-IDF */
                frequencyMap.put(term.text(), termFreq * similarity.idf(docFreq, docNumber));
            }

        /* Close the reader */
        reader.close();

        return frequencyMap;
    }


    /**
     * Generate a small in-memory index from the set of initial relevant documents for a query
     *
     * @param relevantDocuments a list of initially relevant documents for a query
     * @return the index for this
     * @throws IOException
     */
    public static Directory generateRelevantDirectory(List<Document> relevantDocuments, Analyzer analyzer, int documentLimit) throws IOException {
        /* In-memory */
        Directory index= new RAMDirectory();
        IndexWriter indexWriter = new IndexWriter(index, new IndexWriterConfig(analyzer));

        /* Add the docs to the index */
        for (int i = 0; i < relevantDocuments.size() && i < documentLimit; ++i) {
            indexWriter.addDocument(relevantDocuments.get(i));
        }

        /* Store the documents, and finalize the indexing process */
        indexWriter.close();

        return index;
    }

    /**
     * Create a method with the given directory
     *
     * @param directory the directory from which the index reader will be created
     * @return the index searcher
     * @throws IOException
     */
    public static IndexSearcher createIndexSearcher(Directory directory) throws IOException {
        return new IndexSearcher(DirectoryReader.open(directory));
    }

    /**
     * Get the ids of the documents inside an Index
     *
     * @param docLimit a number of maximal documents
     * @param searcher the index reader
     * @return the ids of the docs within the index
     * @throws IOException
     */
    public static List<Integer> getDocumentIds(IndexSearcher searcher, int docLimit) throws IOException {
        List<Integer> idList = new ArrayList<>();

        ScoreDoc[] scoreDocs = searcher.search(new MatchAllDocsQuery(), docLimit).scoreDocs;


        for (int i = 0; i < scoreDocs.length; ++i) {
            idList.add(scoreDocs[i].doc);
//            System.out.println(searcher.doc(scoreDocs[i].doc));

//            System.out.println(searcher.getIndexReader().getTermVector(scoreDocs[i].doc));
        }

        return idList;
    }
}
