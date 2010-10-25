/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package domains.semiSupervisedLearning;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Victor
 */
public class HelperFunctions {

    public static void stripLabels(String inDataShardPath, String outDataShardPath) {
        try {
            BufferedReader inDataShard = new BufferedReader(new FileReader(inDataShardPath));
            BufferedWriter outDataShard = new BufferedWriter(new FileWriter(outDataShardPath));
            try {
                String line = null;
                while ((line = inDataShard.readLine()) != null) {
                    outDataShard.write("u" + line.substring(line.indexOf(" ")) + "\n");
                }
            } finally {
                inDataShard.close();
                outDataShard.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void evaluate(String dataShardPath, String predictionPath) {
        try {
            BufferedReader dataShard = new BufferedReader(new FileReader(dataShardPath));
            BufferedReader predictions = new BufferedReader(new FileReader(predictionPath));
            BufferedWriter status = new BufferedWriter(new FileWriter("status"));
            try {
                String line = null;
                List<String> correctLabels = new ArrayList<String>();
                List<String> predictedLabels = new ArrayList<String>();
                while ((line = dataShard.readLine()) != null) {
                    correctLabels.add(line);
                }
                while ((line = predictions.readLine()) != null) {
                    predictedLabels.add(line);
                }
                if (correctLabels.size() == predictedLabels.size()) {
                    double correct = 0.0;
                    double incorrect = 0.0;
                    double total = 0.0;
                    for (int i = 0; i < correctLabels.size(); i++) {
                        if (correctLabels.get(i).equals(predictedLabels.get(i))) {
                            correct++;
                        } else {
                            incorrect++;
                        }
                        total++;
                    }
                    status.write("numErrors: " + incorrect + "\n");
                    status.write("numExamples: " + total + "\n");
                    status.write("errorRate: " + incorrect/total + "\n");
                }
            } finally {
                dataShard.close();
                predictions.close();
                status.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        HelperFunctions h = new HelperFunctions();
        h.stripLabels("input.txt", "strippedLabels.txt");
        h.evaluate("predictions.txt", "test.txt");
    }
}
