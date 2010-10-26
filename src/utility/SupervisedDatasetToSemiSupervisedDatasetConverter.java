package utility;

import java.io.*;
import java.util.*;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * Converts a given fraction of a given supervised learning dataset into unlabeled instances.
 * 
 * @author Victor
 */
public class SupervisedDatasetToSemiSupervisedDatasetConverter {
    static List<String> lines = new ArrayList<String>();

    public static void main(String[] args) {
        try {
        	double fractionUnlabeled;
        	fractionUnlabeled = Double.parseDouble(args[0]);
        	
            BufferedReader contents = new BufferedReader(new FileReader(args[1]));
            BufferedWriter output = new BufferedWriter(new FileWriter(args[2]));
            try {
                String line = null;
                while ((line = contents.readLine()) != null) {
//                    System.out.println(line);
                    lines.add(line);
                }
                System.out.println();
                int count = 0;
                int relabeled = 0;
                for (String newLine : lines) {
                    String outLine;
                    if (count < fractionUnlabeled * lines.size()) {
                    	outLine = newLine.trim();
                    	final int indexOfFirstSpace = outLine.indexOf(' ');
                    	if (indexOfFirstSpace == -1) {
                    		outLine = "u";
                    	}
                    	else {
                    		outLine = "u" + newLine.substring(indexOfFirstSpace);
                    	}
                        relabeled++;
                    } else {
                        outLine = newLine;
                    }
                    
                    output.write(outLine + "\n");
//                    System.out.println(outLine);
                    count++;
                }

//                System.out.println("Unlabeled: " + relabeled + " out of: " + lines.size());
            } finally {
                contents.close();
                output.close();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
