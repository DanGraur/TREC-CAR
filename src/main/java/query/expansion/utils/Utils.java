package query.expansion.utils;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.index.*;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.RAMDirectory;
import query.expansion.DocumentWrapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dan Graur 4/11/2018
 */
public class Utils {

    public static Map<String, Float> getTFIDF(Directory document, Set<String> tokens, String targetField) throws IOException {
        /* Declare the Map */
        Map<String, Float> frequencyMap = new HashMap<>();

        /* Get the target field terms */
        IndexReader reader = DirectoryReader.open(document);
        Fields fields = MultiFields.getFields(reader);
        Terms terms = fields.terms(targetField);
        TermsEnum termsEnum = terms.iterator();

        /* Get the number of documents */
        int docNumber = reader.numDocs();

        /* Declare the similarity */
        ClassicSimilarity similarity = new ClassicSimilarity();

        while (termsEnum.next() != null)
            if (tokens.contains(termsEnum.term().toString().toLowerCase())) {
                Term term = new Term(targetField, termsEnum.term().toString());
                int docFreq = reader.docFreq(term);
                long termFreq = reader.totalTermFreq(term);
//                int docFreq = termsEnum.docFreq();

                /* Compute the TF-IDF * beta */
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
    public static Directory generateRelevantIndex(List<DocumentWrapper> relevantDocuments, Analyzer analyzer, int documentLimit) throws IOException {
        /* In-memory */
        Directory index= new RAMDirectory();
        IndexWriter indexWriter = new IndexWriter(index, new IndexWriterConfig(analyzer));

        /* Add the docs to the index */
        for (int i = 0; i < relevantDocuments.size() && i < documentLimit; ++i) {
            indexWriter.addDocument(relevantDocuments.get(i).getDocument());
        }

        /* Store the documents, and finalize the indexing process */
        indexWriter.close();

        return index;
    }
}
