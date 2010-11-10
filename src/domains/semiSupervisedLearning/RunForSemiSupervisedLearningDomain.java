package domains.semiSupervisedLearning;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import datasetFormat.SparseLineAST;

/**
 * Interface for the MLcomp SemiSupervisedMulticlass domain.
 * 
 * @author Eugene Ma
 */
public class RunForSemiSupervisedLearningDomain {
	protected static final boolean gAppendToStatusFileFlag = true;

	public static final String gStatusFilename = "status";
	public static final double gFractionOfLabeledInstancesForTraining = 0.7;

	// *******************************************************************************
	public static void main(String[] args) {
		if (args.length == 0) {
			throw new RuntimeException("The arguments list must not be empty!");
		}
		else {
			final String command = args[0];
			if (command.equals("construct")) {
				// Do nothing.
			}
			else if (command.equals("inspect")) {
				inspect(args);
			}
			else if (command.equals("split")) {
				split(args);
			}
			else if (command.equals("stripLabels")) {
				stripLabels(args);
			}
			else if (command.equals("evaluate")) {
				evaluate(args);
			}
			else {
				throw new RuntimeException(String.format("The command \"%s\" is not recognized.", command));
			}
		}
	}

	// *******************************************************************************
	public static void inspect(String[] args) {
		if (args.length != 2) {
			throw new RuntimeException("The datashard path must be the second argument. "
					+ "The arguments are: " + Arrays.asList(args));
		}
		else {
			final String datasetPath = args[1];

			// Instance outputs can an be an integer >= -1 or "u".
			final SparseLineAST.BinaryOrMulticlassValidator baseOutputValidator
				= new SparseLineAST.BinaryOrMulticlassValidator();
			final SparseLineAST.SemiSupervisedTrainingValidator outputValidator
				= new SparseLineAST.SemiSupervisedTrainingValidator(baseOutputValidator);

			try {
				final Map.Entry<List<SparseLineAST>, Integer> instancesToMaxFeatureIndex
					= SparseLineAST.parseSparseEncodingFromInputFilename(datasetPath, outputValidator);

				// Output summary statistics to a status file.

				FileOutputStream outputStream;
				try {
					outputStream = new FileOutputStream(new File(gStatusFilename), gAppendToStatusFileFlag);
				}
				catch (FileNotFoundException x) {
					throw new RuntimeException(String.format("Cannot find the file \"%s\".", gStatusFilename));
				}
				final OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream);
				final BufferedWriter out = new BufferedWriter(outputWriter);

				try {
					// Document divider.
					out.write("---");
					out.newLine();

					// Path.
					out.write("path: " + datasetPath);
					out.newLine();

					// Number of instances.
					out.write("numExamples: " + instancesToMaxFeatureIndex.getKey().size());
					out.newLine();

					// Number of labeled instances.
					out.write("numLabeledExamples: " + outputValidator.getNumLabeledInstances());
					out.newLine();

					// Number of unlabeled instances.
					out.write("numUnlabeledExamples: " + outputValidator.getNumUnlabeledInstances());
					out.newLine();

					// Number of labels.
					out.write("numLabels: " + baseOutputValidator.getNumClassIndices());
					out.newLine();
					
					// Max feature index.
					out.write("maxFeatureIndex: " + instancesToMaxFeatureIndex.getValue());
					out.newLine();
				}
				catch (IOException e) {
					throw new RuntimeException(
							String.format("An error occurred while writing to file \"%s\".", gStatusFilename));
				}

				try {
					out.close();
				}
				catch (IOException e) {
					throw new RuntimeException(
							String.format("Error while closing the file \"%s\".", gStatusFilename));
				}
			}
			catch (RuntimeException x) {
				// FIXME: Need to tell the difference between an error and not
				// recognizing the format.
				System.out.print("An error was encountered: " + x.toString());
				// System.out.println(
				// String.format("The datashard does not conform to the \"%s\" format.",
				// datasetType));
			}
		}
	}

	// *******************************************************************************
	public static void split(String[] args) {
		if (args.length != 4) {
			throw new RuntimeException(
					"The raw datashard path must be the second argument, "
							+ "the train datashard path must be the third argument, "
							+ "and the test datashard path must be the fourth argument.");
		}
		else {
			final String rawDatashardPath = args[1];
			final String trainDatashardPath = args[2];
			final String testDatashardPath = args[3];

			// Parse the raw dataset.
			final SparseLineAST.SemiSupervisedTrainingValidator outputValidator
				= new SparseLineAST.SemiSupervisedTrainingValidator(
					new SparseLineAST.NumericValidator());
			final List<SparseLineAST> instances
				= SparseLineAST.parseSparseEncodingFromInputFilename(rawDatashardPath, outputValidator).getKey();

			// Split the dataset into labeled and unlabeled instances.
			final List<SparseLineAST> labeledInstances
				= new ArrayList<SparseLineAST>(outputValidator.getNumLabeledInstances());
			final List<SparseLineAST> unlabeledInstances
				= new ArrayList<SparseLineAST>(outputValidator.getNumUnlabeledInstances());
			for (SparseLineAST instance : instances) {
				if (instance.getOutput().equals("u")) {
					unlabeledInstances.add(instance);
				}
				else {
					labeledInstances.add(instance);
				}
			}

			Collections.shuffle(labeledInstances);

			// Create the training and test sets.

			// The labeled instances are split between the training and test sets.
			final List<SparseLineAST> trainingSet = new ArrayList<SparseLineAST>();
			final List<SparseLineAST> testSet = new ArrayList<SparseLineAST>();
			final int firstTestIndex
				= (int) Math.floor(labeledInstances.size() * gFractionOfLabeledInstancesForTraining);
			for (int i = 0; i < labeledInstances.size(); ++i) {
				if (i < firstTestIndex) {
					trainingSet.add(labeledInstances.get(i));
				}
				else {
					testSet.add(labeledInstances.get(i));
				}
			}

			trainingSet.addAll(unlabeledInstances);

			SparseLineAST.saveSparseEncoding(trainDatashardPath, trainingSet);
			SparseLineAST.saveSparseEncoding(testDatashardPath, testSet);
		}
	}

	// *******************************************************************************
	protected static void stripLabels(String[] args) {
		if (args.length != 3) {
			throw new RuntimeException(
					"The inDatashardPath should be the second argument, "
							+ "and the outDatashardPath should be the third argument.");
		} else {
			stripLabels(args[1], args[2]);
		}
	}

	/**
	 * 
	 * @author Victor
	 */
	protected static void stripLabels(String inDataShardPath, String outDataShardPath) {
		try {
			BufferedReader inDataShard = new BufferedReader(new FileReader(inDataShardPath));
			BufferedWriter outDataShard = new BufferedWriter(new FileWriter(outDataShardPath));
			try {
				String line = null;
				while ((line = inDataShard.readLine()) != null) {
					int firstWhiteSpace = line.indexOf(" ");
					if (firstWhiteSpace == -1) {
						outDataShard.write("0\n");
					} else {
						outDataShard.write("0" + line.substring(firstWhiteSpace) + "\n");
					}
				}
			} finally {
				inDataShard.close();
				outDataShard.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// *******************************************************************************
	
	protected static void evaluate(String[] args) {
		if (args.length != 3) {
			throw new RuntimeException(
					"The testDatashardPath should be the second argument, "
							+ "and the predictionDatashardPath should be the third argument.");
		} else {
			evaluate(args[1], args[2]);
		}
	}

	/**
	 * 
	 * @author Victor
	 */
	protected static void evaluate(String dataShardPath, String predictionPath) {
		try {
			BufferedReader dataShard = new BufferedReader(new FileReader(dataShardPath));
			BufferedReader predictions = new BufferedReader(new FileReader(predictionPath));

			// Set up status file to append.
			FileOutputStream outputStream;
			try {
				outputStream = new FileOutputStream(new File(gStatusFilename), gAppendToStatusFileFlag);
			} catch (FileNotFoundException x) {
				throw new RuntimeException(String.format(
						"Cannot find the file \"%s\".", gStatusFilename));
			}
			final OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream);
			final BufferedWriter status = new BufferedWriter(outputWriter);

			try {
				String line = null;
				List<String> correctLabels = new ArrayList<String>();
				List<String> predictedLabels = new ArrayList<String>();
				while ((line = dataShard.readLine()) != null) {
					int firstWhiteSpace = line.indexOf(" ");
					if (firstWhiteSpace == -1) {
						correctLabels.add(line);
					} else {
						correctLabels.add(line.substring(0, firstWhiteSpace));
					}
				}
				while ((line = predictions.readLine()) != null) {
					predictedLabels.add(line);
				}
				if (correctLabels.size() == predictedLabels.size()) {
					int correct = 0;
					int incorrect = 0;
					int total = 0;
					for (int i = 0; i < correctLabels.size(); i++) {
						int correctLabel = Integer.parseInt(correctLabels.get(i));
						int predictedLabel = Integer.parseInt(predictedLabels.get(i));
						if (correctLabel == predictedLabel) {
							correct++;
						} else {
							incorrect++;
						}
						total++;
					}
					status.write("---\n");
					status.write("numErrors: " + incorrect + "\n");
					status.write("numExamples: " + total + "\n");
					status.write("errorRate: " + incorrect / (double) total + "\n");
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
}
