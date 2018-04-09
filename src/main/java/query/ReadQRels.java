package query;

import edu.unh.cs.treccar.Data;
import edu.unh.cs.treccar.read_data.DeserializeData;

import java.io.*;
import java.util.*;

/**
 * @author Dan Graur 4/8/2018
 */
public class ReadQRels {

    /**
     * Creates a map between the (plain) queries, and a list of paragraph ids which are relevant for the query
     *
     * @param pathToQRels the path to the QRels file
     * @param plain if true, remove the '%20' and replace with ' '
     * @return a map between the plain queries, and a list of paragraph ids which are relevant for the query
     * @throws IOException
     */
    public static Map<String, Set<String>> readQRels(String pathToQRels, boolean plain) throws IOException {
        Map<String, Set<String>> res = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader(new File(pathToQRels)));
        String line;

        while ((line = reader.readLine()) != null) {
            // We expect the lines to look something like this: Fusarium%20wilt 0 7cfbadebddeb37ccc2d047c8257e1a5c4aecb74d 1

            /* Split by spaces */
            String[] tokens = line.split(" ");

            if (plain)
                /* Replace the "%20" sequences with actual characters */
                tokens[0] = tokens[0].replaceAll("%20", " ");

            /* Check if the key exists, and if it doesn't, add an empty list */
            if (!res.containsKey(tokens[0]))
                res.put(tokens[0], new HashSet<>());

            /* Add the id */
            res.get(tokens[0]).add(tokens[2]);
        }

        return res;
    }


    /**
     * Create a data structure which hosts both the queries (as TRECQuery objects), their IDs, and the IDs of the paragraphs relevant to the queries.
     *
     * @param pathToQRels the path to the QRels file
     * @param pathToOutlines the path to the outline file (which hosts the queries)
     * @return a map between TRECQuery objects, and a list of paragraph ids which are relevant for the query
     * @throws IOException
     */
    public static Map<TRECQuery, Set<String>> readGroundTruths(String pathToQRels, String pathToOutlines) throws IOException {
        /* Read the ground truths */
        Map<String, Set<String>> stringQueries = readQRels(pathToQRels, false);

        /* The result */
        Map<TRECQuery, Set<String>> res = new HashMap<>();


        /* Get the queries */
        final FileInputStream inputStream = new FileInputStream(new File(pathToOutlines));
        for(Data.Page page: DeserializeData.iterableAnnotations(inputStream))
            for (List<Data.Section> sectionPath : page.flatSectionPaths()){
                String queryId = Data.sectionPathId(page.getPageId(), sectionPath);
                String query = page.getPageName() + " " + String.join(" ", Data.sectionPathHeadings(sectionPath));

                /* Add the data */
                res.put(
                        new TRECQuery(queryId, query),
                        stringQueries.get(queryId)
                );
            }

        return res;
    }


    public static void main(String[] args) throws IOException {
        Map<TRECQuery, Set<String>> res = readGroundTruths("./data_14/train.test200.cbor.hierarchical.qrels", "./data_14/train.test200.cbor.outlines");

        for (TRECQuery trecQuery : res.keySet()) {
            System.out.println(trecQuery);
            System.out.println("Size of relevant set: " + res.get(trecQuery).size() + "\n");
        }
    }
}
