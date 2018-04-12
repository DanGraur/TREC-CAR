package query.expansion;

import org.apache.lucene.document.Document;

/**
 * @author Dan Graur 4/12/2018
 */
public class DocumentWrapper {
    /**
     * Internal Lucene document ID
     */
    private int documentId;
    /**
     * Lucene docuemnt objects
     */
    private Document document;

    public DocumentWrapper(int documentId, Document document) {
        this.documentId = documentId;
        this.document = document;
    }

    public int getDocumentId() {
        return documentId;
    }

    public void setDocumentId(int documentId) {
        this.documentId = documentId;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }
}
