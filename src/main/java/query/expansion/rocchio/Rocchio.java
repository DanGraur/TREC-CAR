package query.expansion.rocchio;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import query.QueryBuilder;
import query.expansion.Expander;
import query.expansion.utils.Utils;

import java.io.IOException;
import java.util.*;

/**
 * This is a modern version of Rocchio which does not make use of the gamma (considering the (ona average) large size of irrelevant documents, this is probably a good idea).
 *
 * @author Dan Graur 4/11/2018
 */
public class Rocchio implements Expander {
    /**
     * Alpha weight (see the Rocchio algorithm)
     */
    private float alpha;
    /**
     * Beta weight (see the Rocchio algorithm)
     */
    private float beta;
    /**
     * Maximal number of terms of the new expanded query
     */
    private int termLimit;
    /**
     * Maximal number of documents to be considered in the relevant index
     */
    private int documentLimit;
    /**
     * The target field for the search operation
     */
    private String targetField;
    /**
     * The analyzer used for creating the initial query and the indexing itself
     */
    private Analyzer analyzer;
    /**
     * The query builder, used to build the Lucene query for the expanded plain query
     */
    private QueryBuilder queryBuilder;

    public Rocchio(float alpha, float beta, int termLimit, int documentLimit, String targetField, Analyzer analyzer, QueryBuilder queryBuilder) {
        this.alpha = alpha;
        this.beta = beta;
        this.termLimit = termLimit;
        this.documentLimit = documentLimit;
        this.targetField = targetField;
        this.analyzer = analyzer;
        this.queryBuilder = queryBuilder;
    }

    public Query expand(Query query, List<Document> relevantDocuments) throws IOException {
        /* Get the set of words for the query */
        Set<String> queryTerms = new HashSet<>(Arrays.asList(query.toString().split("\\s+")));

        /* Lowercase all (this should be lower case by default (due to the lowercase filter), but just to make sure) */
        queryTerms.stream().map(String::toLowerCase);

        /* Get the frequency maps */
        Directory index = generateRelevantIndex(relevantDocuments);
        /* This (or part of this) will be the query vector */
        Map<String, Float> allTermFreq = extractTermFrequency(index);
        Map<String, Float> queryTermFreq = Utils.getTFIDF(index, queryTerms, targetField);

        for (Map.Entry<String, Float> term : queryTermFreq.entrySet()) {
            /* Multiply the current entry with alpha */
            float modifiedFreq = term.getValue() * alpha;

            /* Check if the entry exists in the allTermFreq; if it does extract the beta multiplied TF-IDF */
            if (allTermFreq.containsKey(term.getKey()))
                modifiedFreq += allTermFreq.get(term.getKey());

            /* Update or introduce the term in the  */
            allTermFreq.put(term.getKey(), modifiedFreq);
        }

        List<Map.Entry<String, Float>> finalQueryTerms = new ArrayList<>(allTermFreq.entrySet());
        finalQueryTerms.sort(Comparator.comparing(Map.Entry::getValue));
        Collections.reverse(finalQueryTerms);

        StringBuilder queryString = new StringBuilder();

        for (Map.Entry<String, Float> entry : finalQueryTerms.subList(0, finalQueryTerms.size() > (termLimit + 1) ? termLimit + 1 : finalQueryTerms.size()))
            queryString.append(' ').append(entry.getKey());

        return queryBuilder.buildQuery(targetField, queryString.toString());
    }

    /**
     * Creates an iterable list of terms and TF-IDF computer frequencies, as extracted from the passed Directory
     *
     * @param directory  dictionary containing the documents in the relevant set for the initial query
     * @return an iterable list of terms and TF-IDF computer frequencies, as extracted from the passed Directory
     * @throws IOException
     */
    private Map<String,Float> extractTermFrequency(Directory directory) throws IOException {
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

        while (termsEnum.next() != null) {
            Term term = new Term(targetField, termsEnum.term().utf8ToString());
            int docFreq = reader.docFreq(term);
            long termFreq = reader.totalTermFreq(term);
//            int docFreq = termsEnum.docFreq();

            /* Compute the TF-IDF * beta */
            frequencyMap.put(term.text(), beta * termFreq * similarity.idf(docFreq, docNumber));
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
    private Directory generateRelevantIndex(List<Document> relevantDocuments) throws IOException {
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



}
