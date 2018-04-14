package query.expansion.rm;

import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Dan Graur 4/9/2018
 */
public class DocumentVector {

    /**
     * PerTermStat of the Document.
     */
    public HashMap<String, PerTermStat>     docPerTermStat;
    /**
     * Size of the Document.
     */
    private int                              size;
    /**
     * The retrieval score of the document after a retrieval. *Mostly unused*
     */
    private float                            docScore;   // retrieval score

    public DocumentVector() {
        docPerTermStat = new HashMap<>();
    }

    public DocumentVector(HashMap<String, PerTermStat> docVec, int size) {
        this.docPerTermStat = docVec;
        this.size = size;
    }

    public DocumentVector(HashMap<String, PerTermStat> docVec, int size, float docScore) {
        this.docPerTermStat = docVec;
        this.size = size;
        this.docScore = docScore;
    }

    public HashMap getDocPerTermStat() {return docPerTermStat;}
    public int getDocSize() {return size;}
    public float getDocScore() {return docScore;}

    /**
     * Returns the document vector for a document with lucene-docid=luceneDocId
       Returns dv containing 
      1) docPerTermStat: a HashMap of (t,PerTermStat) type
      2) size : size of the document
     * @param luceneDocId
     * @param cs
     * @return document vector
     * @throws IOException 
     */
    public static DocumentVector getDocumentVector(int luceneDocId, CollectionStatistics cs, String targetField) throws IOException {

        DocumentVector dv = new DocumentVector();
        int docSize = 0;

        if(cs.indexReader==null) {
            System.out.println("Error: null == indexReader in showDocumentVector(int,IndexReader)");
            System.exit(1);
        }

        // t vector for this document and field, or null if t vectors were not indexed
        Terms terms = cs.indexReader.getTermVector(luceneDocId, targetField);
        if(null == terms) {
            System.err.println("Error getDocumentVector(): Term vectors not indexed: "+luceneDocId);
            return null;
        }

        TermsEnum iterator = terms.iterator();
        BytesRef byteRef = null;

        //* for each word in the document
        while((byteRef = iterator.next()) != null) {
            String term = new String(byteRef.bytes, byteRef.offset, byteRef.length);
            //int docFreq = iterator.docFreq();            // df of 't'
            long termFreq = iterator.totalTermFreq();    // tf of 't'
            //System.out.println(t+": tf: "+termFreq);
            docSize += termFreq;

            //* termFreq = cf, in a document; df = 1, in a document
            //dv.docPerTermStat.put(t, new PerTermStat(t, termFreq, 1));
            dv.docPerTermStat.put(term, new PerTermStat(term, termFreq, 1, cs.perTermStat.get(term).getIDF(), (double)termFreq/(double)cs.getVocSize()));
        }
        dv.size = docSize;
        //System.out.println("DocSize: "+docSize);

        return dv;
    }

    public static DocumentVector getDocumentVector(int luceneDocId, IndexReader indexReader, String targetField) throws IOException {
        DocumentVector dv = new DocumentVector();
        int docSize = 0;

        if(indexReader==null) {
            System.out.println("Error: null == indexReader in showDocumentVector(int,IndexReader)");
            System.exit(1);
        }

//        Terms terms = indexReader.getTermVector(luceneDocId, targetField);

        Fields fields = indexReader.getTermVectors(luceneDocId);
        Terms terms = fields.terms(targetField);

//        System.out.println("ASD: " + terms);

        if(null == terms) {
            System.err.println("Error getDocumentVector(): Term vectors not indexed: "+luceneDocId);
            return null;
        }

        TermsEnum iterator = terms.iterator();
        BytesRef byteRef;

        //* for each word in the document
        while((byteRef = iterator.next()) != null) {
            String term = new String(byteRef.bytes, byteRef.offset, byteRef.length);
            //int docFreq = iterator.docFreq();            // df of 't'
            long termFreq = iterator.totalTermFreq();    // tf of 't'
            //System.out.println(t+": tf: "+termFreq);
            docSize += termFreq;

            //* termFreq = cf, in a document; df = 1, in a document
            //dv.docPerTermStat.put(t, new PerTermStat(t, termFreq, 1));
            dv.docPerTermStat.put(term, new PerTermStat(term, termFreq, 1, getIdf(term, indexReader, targetField), getCollectionProbability(term, indexReader, targetField)));
        }
        dv.size = docSize;
        //System.out.println("DocSize: " + docSize);

        return dv;
    }

    public static double getIdf(String term, IndexReader indexReader, String fieldName) throws IOException {
        int docCount = indexReader.maxDoc();      // total number of documents in the index
        Fields fields = MultiFields.getFields(indexReader);
        Term termInstance = new Term(fieldName, term);
        long df = indexReader.docFreq(termInstance);       // DF: Returns the number of documents containing the term

        double idf;
        idf = Math.log((float)(docCount)/(float)(df+1));

        return idf;
    }

    public static long getVocabularySize(IndexReader indexReader, String field) throws IOException {
        Fields fields = MultiFields.getFields(indexReader);
        Terms terms = fields.terms(field);
        if(null == terms) {
            System.err.println("Field: "+field);
            System.err.println("Error buildCollectionStat(): terms Null found");
        }
        long vocSize = terms.getSumTotalTermFreq();  // total number of terms in the index in that field

        return vocSize;
    }


    public static float getCollectionProbability(String term, IndexReader reader, String fieldName) throws IOException {

        Term termInstance = new Term(fieldName, term);
        long termFreq = reader.totalTermFreq(termInstance); // CF: Returns the total number of occurrences of term across all documents (the sum of the freq() for each doc that has this term).

        return (float) termFreq / (float) getVocabularySize(reader, fieldName);
    }

    /**
     * Returns true if the docPerTermStat is not-zero; else, return false.
     * @return 
     */
    public boolean printDocumentVector() {

        if(this == null) {
            System.err.println("Error: printing document vector. Calling docVec null");
            System.exit(1);
        }
        if(0 == this.docPerTermStat.size()) {
            System.out.println("Error: printing document vector. Calling docVec zero");
            return false;
        }
        for (Map.Entry<String, PerTermStat> entrySet : this.docPerTermStat.entrySet()) {
            String key = entrySet.getKey();
            PerTermStat value = entrySet.getValue();
            System.out.println(key + " : " + value.getCF());
        }
        return true;
    }
}