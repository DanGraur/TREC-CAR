package query.expansion.rm;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import query.expansion.utils.Utils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Dan Graur 4/9/2018
 */
public class RLM {
    /**
     * The target field for the queries
     */
    private String targetField;
    /**
     * The analyzer
     */
    private Analyzer analyzer;
    /**
     * number of feedback terms
     */
    private int numFeedbackTerms;
    /**
     * number of feedback documents
     */
    private int numFeedbackDocs;
    /**
     *  mixing weight, used for doc-col weight adjustment
     */
    private float mixingLambda;
    /**
     * query mixing parameter; to be used for RM3
     */
    private float lambda;
    /**
     * Hashmap of Vectors of all feedback documents, keyed by luceneDocId.
     */
    private Map<Integer, DocumentVector> feedbackDocumentVectors;
    /**
     * HashMap of PerTermStat of all feedback terms, keyed by the term.
     */
    private Map<String, PerTermStat> feedbackTermStats;
    /**
     * HashMap of P(Q|D) for all feedback documents, keyed by luceneDocId.
     */
    private Map<Integer, Float> hash_P_Q_Given_D;
    /**
     * List, for sorting the words in non-increasing order of probability.
     */
    private List<WordProbability> list_PwGivenR;
    /**
     * HashMap of P(w|R) for 'numFeedbackTerms' terms with top P(w|R) among each w in R,
     * keyed by the term with P(w|R) as the value.
     */
    private HashMap<String, WordProbability> hashmap_PwGivenR;
    /**
     * The size of the vocabulary
     */
    private long vocabularySize;


    public RLM(Analyzer analyzer, int numFeedbackDocs, int numFeedbackTerms, float lambda, float  mixingLambda, String targetField) {
        this.analyzer = analyzer;
        this.numFeedbackDocs = numFeedbackDocs;
        this.numFeedbackTerms = numFeedbackTerms;
        this.mixingLambda = mixingLambda;
        this.lambda = lambda;
        this.targetField = targetField;
    }


    /**
     * Compute P(Q|d) probabilities given a set of set of relevant documents for the initial query, and the tokenized query itself
     *
     * @param hits a set of set of relevant documents for the initial query
     * @param analyzedQuery the tokenized query itself
     * @throws IOException
     */
    public void setFeedbackStats(List<Document> hits, String[] analyzedQuery) throws IOException {
        /* Clear/Initiate the DS we'll be using in the algorithm */
        feedbackDocumentVectors = new HashMap<>();
        feedbackTermStats = new HashMap<>();
        hash_P_Q_Given_D = new HashMap<>();

        /* Get an index reader, given the current hit list */
        Directory directory = Utils.generateRelevantDirectory(hits, analyzer, numFeedbackDocs);

        /* Create the Index */
        IndexSearcher indexSearcher = Utils.createIndexSearcher(directory);

        /* Compute the vocabulary size */
        getVocabularySize(indexSearcher.getIndexReader());

//        System.out.println("The vocabulary size is: " + vocabularySize);

        /* Get the document ids */
        List<Integer> docIds = Utils.getDocumentIds(indexSearcher, numFeedbackDocs);

        for (Integer luceneDocId : docIds) {

            /* Get the document vector */
            DocumentVector docV = DocumentVector.getDocumentVector(luceneDocId, indexSearcher.getIndexReader(), targetField);



            /* Defensive check - see if the document vector has indeed been created */
            if(docV == null)
                continue;

            /* Add the vector of the relevant documents in a map */
            feedbackDocumentVectors.put(luceneDocId, docV);

            /* Iterate through each term of the document */
            for (Map.Entry<String, PerTermStat> entrySet : docV.docPerTermStat.entrySet()) {
                /* Get the term and its stats */
                String key = entrySet.getKey();
                PerTermStat value = entrySet.getValue();

                /* If the term does not exist in the feedbackTermStats, then add it together with its initial stats */
                if(!feedbackTermStats.containsKey(key))
                    feedbackTermStats.put(key, new PerTermStat(key, value.getCF(), value.getDF()));
                else {
                    /* If it does exist, then update the Corpus Frequency and the Document Frequency of the term in the feedbackTermStats map */
                    value.setCF(value.getCF() + feedbackTermStats.get(key).getCF());
                    value.setDF(value.getDF() + feedbackTermStats.get(key).getDF());
                    feedbackTermStats.put(key, value);
                }
            }
        }

        /* Calculate the P(Q|d) probability for each initially relevant document */
        for (Map.Entry<Integer, DocumentVector> entrySet : feedbackDocumentVectors.entrySet()) {
            // for each feedback document
            int luceneDocId = entrySet.getKey();
            DocumentVector docV = entrySet.getValue();

            float p_Q_GivenD = 1;

            /* Assume unigram model, hence independent probabilities, hence product */
            for (String qTerm : analyzedQuery)
                p_Q_GivenD *= return_Smoothed_MLE(qTerm, docV);
            /* If this is the first time we are recording the probability for this document, then add it; else something wrong has happened */
            if(!hash_P_Q_Given_D.containsKey(luceneDocId))
                hash_P_Q_Given_D.put(luceneDocId, p_Q_GivenD);
        }

    }

    /**
     * Compute the Smoothed Maximum Likelihood Estimate
     *
     * @param t the term whose MLE is computed
     * @param dv the vector representing the Document in cause
     * @return the MLE of the term given the document vector
     */
    public float return_Smoothed_MLE(String t, DocumentVector dv) {
        float smoothedMLEofTerm = 1;
        PerTermStat docPTS;

//        HashMap<String, PerTermStat>     docPerTermStat = dv.getDocPerTermStat();
//        docPTS = docPerTermStat.get(t);
        docPTS = dv.docPerTermStat.get(t);
//        colPTS = collStat.perTermStat.get(t);
        PerTermStat colPTS = feedbackTermStats.get(t);

        if (colPTS != null) {
            smoothedMLEofTerm =
                    ((docPTS!=null)?(mixingLambda * (float)docPTS.getCF() / (float)dv.getDocSize()):(0)) +
                            ((feedbackTermStats.get(t)!=null)?((1.0f-mixingLambda)*(float)feedbackTermStats.get(t).getCF()/(float) vocabularySize):0);
        }

        return smoothedMLEofTerm;
    }

    /**
     * mixingLambda*tf(t,d)/d-size + (1-mixingLambda)*cf(t)/col-size
     * @param pts
     * @param dv The document vector under consideration
     * @return MLE of t in a document dv, smoothed with collection statistics
     * @throws IOException IOException
     */
    public float return_Smoothed_MLE(PerTermStat pts, DocumentVector dv) {

        float smoothedMLEofTerm;
        PerTermStat docPTS;
        PerTermStat colPTS;

//        HashMap<String, PerTermStat>     docPerTermStat = dv.getDocPerTermStat();
//        docPTS = docPerTermStat.get(t);
        docPTS = dv.docPerTermStat.get(pts.t);
//        colPTS = collStat.perTermStat.get(t);

        smoothedMLEofTerm =
                ((docPTS!=null)?(mixingLambda * (float)docPTS.getCF() / (float)dv.getDocSize()):(0))
                        + (1.0f-mixingLambda)*((float)pts.getCF()/(float) vocabularySize);
//            + ((colPTS!=null)?((1.0f-mixingLambda)*(float)colPTS.getCF() / (float)vocabularySize):(0));

        return smoothedMLEofTerm;
    } // ends return_Smoothed_MLE()


    /**
     * Computes the frequency of the term qTerm given the query qTerms (as String[])
     *
     * @param qTerms the query
     * @param qTerm the term
     * @return the term's frequency in the query
     */
    public float returnMLE_of_q_in_Q(String[] qTerms, String qTerm) {
        int count=0;

        for (String queryTerm : qTerms)
            if (qTerm.equals(queryTerm))
                ++count;

        return ((float)count / (float)qTerms.length);
    }

    /**
     * Compute the candidate expanded query terms as a map of terms -> probability (given the set of relevant documents in the relevant set)
     *
     * @return the candidate expanded query terms as a map of terms -> probability (given the set of relevant documents in the relevant set); warning maps are not sorted
     * @throws Exception
     */
    public Map<String, WordProbability> RM1() {
        /* Declare the accumulator variable for the probability */
        float p_W_GivenR_one_doc;
        list_PwGivenR = new ArrayList<>();
        /* The linked hash map should maintain proper insertion order */
        hashmap_PwGivenR = new LinkedHashMap<>();

        // Calculating for each wi in R: P(wi|R)~P(wi, q1 ... qk)
        // P(wi, q1 ... qk) = \sum_{D \in initial-ret-docs} {P(w|D)*\prod_{i=1... k} {P(qi|D}}

        /* Iterate over each term from the set of relevant documents */
        for (Map.Entry<String, PerTermStat> entrySet : feedbackTermStats.entrySet()) {
            /* Get the term itself, and get the probability for this particular  */
            String t = entrySet.getKey();
            p_W_GivenR_one_doc = 0;

            /* Compute the P(w|R) probability (w is the word and R is the relevant document collection) by using all the relevant documents */
            for (Map.Entry<Integer, DocumentVector> docEntrySet : feedbackDocumentVectors.entrySet())
                p_W_GivenR_one_doc += return_Smoothed_MLE(entrySet.getValue(), feedbackDocumentVectors.get(docEntrySet.getKey())) *
                        hash_P_Q_Given_D.get(docEntrySet.getKey());

            /* Add the probability of this particular word to the collection */
            list_PwGivenR.add(new WordProbability(t, p_W_GivenR_one_doc));
        }

        /* Sort the word probabilities in decreasing order */
        list_PwGivenR.sort((t, t1) -> Float.compare(t1.p_w_given_R, t.p_w_given_R));

        for (WordProbability singleTerm : list_PwGivenR)
            if (!hashmap_PwGivenR.containsKey(singleTerm.w))
                /* The value here was previously re-created (i.e. new object) */
                hashmap_PwGivenR.put(singleTerm.w, singleTerm);

        return hashmap_PwGivenR;
    }


    public Map<String, WordProbability> RM3(String[] analyzedQuery) {
        /* By running RM1 we'll have the word -> probability | R in hashmap_PwGivenR */
        RM1();

        /*  */
        int maxSize = list_PwGivenR.size() > numFeedbackDocs + 1 ? numFeedbackDocs + 1 : list_PwGivenR.size();

        /* Only consider a subset of the extracted term set */
        List<String> reducedProbabilities = new ArrayList<>(list_PwGivenR)
                .subList(0, maxSize)
                .stream()
                .map((WordProbability probability) -> probability.w)
                .collect(Collectors.toList());

        hashmap_PwGivenR.keySet().retainAll(reducedProbabilities);

        /* Get the normalization term */
        float normalizationFactor = 0.0f;

        for (Map.Entry<String, WordProbability> element : hashmap_PwGivenR.entrySet())
            normalizationFactor += element.getValue().p_w_given_R;

        /* Normalize the probabilities */
        for (Map.Entry<String, WordProbability> entry : hashmap_PwGivenR.entrySet())
            entry.getValue().p_w_given_R /= normalizationFactor;

        normalizationFactor = 0;
        /* Multiply each word probability with the lambda factor */
        for (Map.Entry<String, WordProbability> entrySet : hashmap_PwGivenR.entrySet()) {
            WordProbability value = entrySet.getValue();
            value.p_w_given_R *= lambda;
            normalizationFactor += value.p_w_given_R;
        }

        // Now P(w|R) = lambda*P(w|R)
        //* Each w which are also query terms: P(w|R) += (1-lambda)*P(w|Q)
        //      P(w|Q) = tf(w,Q)/|Q|
        for (String qTerm : analyzedQuery) {
            float probability = (1.0f - lambda) * returnMLE_of_q_in_Q(analyzedQuery, qTerm);

            if (hashmap_PwGivenR.containsKey(qTerm)) { // qTerm is in R
                hashmap_PwGivenR.get(qTerm).p_w_given_R += probability;
                normalizationFactor += probability;
            } else
                /* No need to add to the normalizationFactor since we did it in the previous for loop */
                hashmap_PwGivenR.put(qTerm, new WordProbability(qTerm, probability));
        }

        /* Normalize the values */
        for (Map.Entry<String, WordProbability> entry : hashmap_PwGivenR.entrySet())
            entry.getValue().p_w_given_R /= normalizationFactor;

        return hashmap_PwGivenR;
    }

    /**
     * Get the total number of unique terms in the index
     *
     * @param indexReader the index from which information will be extracted
     * @return vocabularySize Total number of terms in the vocabulary
     * @throws IOException IOException
     */
    private long getVocabularySize(IndexReader indexReader) throws IOException {
        Fields fields = MultiFields.getFields(indexReader);
        Terms terms = fields.terms(targetField);

        vocabularySize = terms.getSumTotalTermFreq();

        return vocabularySize;
    }

//    /**
//     * Returns the expanded query in BooleanQuery form with P(w|R) as
//     * corresponding weights for the expanded terms
//     * @param expandedQuery The expanded query
//     * @param query The query
//     * @return BooleanQuery to be used for consequent re-retrieval
//     * @throws Exception
//     */
//    public BooleanQuery getExpandedQuery(HashMap<String, WordProbability> expandedQuery) {
//
//        BooleanQuery booleanQuery = new BooleanQuery();
//
//        for (Map.Entry<String, WordProbability> entrySet : expandedQuery.entrySet()) {
//            String key = entrySet.getKey();
//            if(key.contains(":"))
//                continue;
//            WordProbability wProba = entrySet.getValue();
//            float value = wProba.p_w_given_R;
//
//            Term t = new Term(rblm.fieldToSearch, key);
//            Query tq = new TermQuery(t);
//            tq.setBoost(value);
//            BooleanQuery.setMaxClauseCount(4096);
//            booleanQuery.add(tq, BooleanClause.Occur.SHOULD);
//        }
//
//        return booleanQuery;
//    } // ends getExpandedQuery()
}