package query.expansion.rocchio;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
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

    public Query expand(String[] query, List<Document> relevantDocuments) throws IOException {
        /* Get the set of words for the query */
        Set<String> queryTerms = new HashSet<>(Arrays.asList(query));

        /* Get the frequency maps */
        Directory index = Utils.generateRelevantDirectory(relevantDocuments, analyzer, documentLimit);

        /* This (or part of this) will be the query vector */
        Map<String, Float> allTermFreq = extractTermFrequency(index);
        Map<String, Float> queryTermFreq = Utils.getTFIDF(index, queryTerms, targetField);

//        System.out.println("Size of the query term freq map: " + queryTermFreq.size());

//        for (Map.Entry<String, Float> s : queryTermFreq.entrySet())
//            System.out.println(s);
//
//        System.exit(1);

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

        /* Declare the similarity which will allow us to compute the IDF */
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

}
