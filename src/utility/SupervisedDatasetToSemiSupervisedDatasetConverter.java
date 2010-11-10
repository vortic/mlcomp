package utility;

import java.io.*;
import java.util.*;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * Converts a given fraction of a given supervised learning dataset into unlabeled instances.
 * First argument: Fraction of the dataset to be labeled.
 * Second argument: Fraction of the unlabeled dataset to be kept.
 * 
 * @author Victor
 */
public class SupervisedDatasetToSemiSupervisedDatasetConverter {
	static List<String> lines = new ArrayList<String>();

	public static void main(String[] args) {
		try {
			// TODO: Check validity of arguments.
			
			BufferedReader contents = new BufferedReader(new FileReader(args[0]));
			BufferedWriter output = new BufferedWriter(new FileWriter(args[1]));
			
			final double fractionLabeled = Double.parseDouble(args[2]);
			final double fractionUnlabeled = 1 - fractionLabeled;
			final double fractionOfUnlabeledToKeep = Double.parseDouble(args[3]);

			try {
				// Read in the file, line by line.
				String line = null;
				while ((line = contents.readLine()) != null) {
					// System.out.println(line);
					lines.add(line);
				}
				//System.out.println();
				
				// The dataset is partitioned as follows:
				// First block is unlabeled to keep.
				// Second block is unlabeled to discard.
				// Third block is labeled (to keep).
				final int firstLabeledLineIndex = (int) (fractionUnlabeled * lines.size());
				final int firstUnlabeledLineIndexToDiscard
					= (int) (fractionOfUnlabeledToKeep * firstLabeledLineIndex);
				
				int count = 0;
				int numUnlabeledKept = 0;
				int numUnlabeledDiscarded = 0;
				int numLabeled = 0;
				for (String newLine : lines) {
					if (count < firstUnlabeledLineIndexToDiscard) {
						// First block: unlabeled to keep.
						
						String outLine = newLine.trim();
						final int indexOfFirstSpace = outLine.indexOf(' ');
						if (indexOfFirstSpace == -1) {
							outLine = "u";
						}
						else {
							outLine = "u" + newLine.substring(indexOfFirstSpace);
						}
						
						numUnlabeledKept++;
					}
					else if (count < firstLabeledLineIndex) {
						// Second block: unlabeled to discard.
						
						// Skip over line.
						
						numUnlabeledDiscarded++;
					}
					else {
						// Third block: labeled (to keep).
						
						output.write(newLine + "\n");
						
						numLabeled++;
					}

					count++;
				}

				System.out.println("Number of unlabeled kept: " + numUnlabeledKept);
				System.out.println("Number of unlabeled discarded: " + numUnlabeledDiscarded);
				System.out.println("Number of labeled: " + numLabeled);
				System.out.println("Total: " + count);
			} finally {
				contents.close();
				output.close();
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
