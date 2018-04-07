package edu.unh.cs.treccar.read_data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Dan Graur 4/8/2018
 */
public class ReadQRels {

    public static Map<String, List<String>> readQRels(String pathToQRels) throws IOException {
        Map<String, List<String>> res = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader(new File(pathToQRels)));
        String line;

        while((line = reader.readLine()) != null) {
            // We expect the lines to look something like this: Fusarium%20wilt 0 7cfbadebddeb37ccc2d047c8257e1a5c4aecb74d 1

            /* Split by spaces */
            String[] tokens = line.split(" ");

            /* Replace the "%20" sequences with actual characters */
            tokens[0] = tokens[0].replaceAll("%20", " ");

            /* Check if the key exists, and if it doesn't, add an empty list */
            if (!res.containsKey(tokens[0]))
                res.put(tokens[0], new ArrayList<>());

            /* Add the id */
            res.get(tokens[0]).add(tokens[2]);
        }

        return res;
    }

    public static void main(String[] args) throws IOException {
        Map<String, List<String>> res = readQRels("./data_14/train.test200.cbor.article.qrels");

        for (String s : res.keySet()) {
            System.out.println(s);
        }
    }
}
