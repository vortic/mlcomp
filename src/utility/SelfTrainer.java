/*
 * Runs an external program
 */
package utility;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Reduces semi-supervised learning to supervised learning
 * by providing a wrapper to a supervised learning program.
 * 
 * @author Victor
 */
public class SelfTrainer {

	SupervisedDatasetToSemiSupervisedDatasetConverter semiSuperviser
		= new SupervisedDatasetToSemiSupervisedDatasetConverter();
    List<String> unlabeledInstances = new ArrayList<String>();
    List<String> labeledInstances = new ArrayList<String>();
    String programPath;

    public SelfTrainer() {
        try {
            BufferedReader programFile = new BufferedReader(new FileReader("programPath"));
            try {
                String line = null;
                while ((line = programFile.readLine()) != null) {
                    programPath = line;
                }
            } finally {
                programFile.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void train(String pathToTraining) {
        String[] args = {pathToTraining, "semiTrain"};
        SupervisedDatasetToSemiSupervisedDatasetConverter.main(args);
        try {
            BufferedReader training = new BufferedReader(new FileReader("semiTrain"));
            BufferedWriter labeled = new BufferedWriter(new FileWriter("newTrain"));
            BufferedWriter unlabeled = new BufferedWriter(new FileWriter("newTest"));
            try {
                String line = null;
                while ((line = training.readLine()) != null) {
                    if (line.substring(0, 1).equals("u")) {
                        unlabeledInstances.add(line);
                        unlabeled.write(line + "\n");
                    } else {
                        labeledInstances.add(line);
                        labeled.write(line + "\n");
                    }
                }
            } finally {
                training.close();
                labeled.close();
                unlabeled.close();
            }
            //run programPath with newTrain as training file
            String[] cmd = {programPath + "/run", "learn", "newTrain"};
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void test(String pathToTest, String outputName) {
        try {
            //run programPath to test pathToTest
            String[] cmd = {programPath + "/run", "predict", pathToTest, "programPredictions"};
            Runtime.getRuntime().exec(cmd);

            BufferedReader toAddToLabeled = new BufferedReader(new FileReader("programPredictions"));
            BufferedWriter results = new BufferedWriter(new FileWriter("finalResults"));
            try {
                String line = null;
                while ((line = toAddToLabeled.readLine()) != null) {
                    labeledInstances.add(line);
                }
                for (String labeledInstance : labeledInstances) {
                    results.write(labeledInstance + "\n");
                }
            } finally {
                toAddToLabeled.close();
                results.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        if (args[0].equals("construct")) {
            try {
                BufferedWriter programPath = new BufferedWriter(new FileWriter("programPath"));
                programPath.write(args[1]);
                programPath.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            SelfTrainer superviser = new SelfTrainer();
            if (args[0].equals("learn")) {
                superviser.train(args[1]);
            } else if (args[0].equals("predict")) {
                superviser.test(args[1], args[2]);
            } else {
                System.out.println("Unsupported usage: " + args);
            }
        }
    }
}
