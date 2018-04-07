package retrieval;

import co.nstant.in.cbor.CborException;

import java.io.IOException;

/**
 * @author Dan Graur 4/6/2018
 */
public class TestClass {

    public static void main(String[] args) throws IOException, CborException {

        String pathToFile = "./data_14/train.test200.cbor.paragraphs";
        String pathToIndex = "./index";

//        new IndexCreator(pathToFile, pathToIndex).createIndex();
        new QuerySolver(System.in, "paragraph", pathToIndex).answerQueries(5);

    }
}
