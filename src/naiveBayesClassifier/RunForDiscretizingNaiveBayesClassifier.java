package naiveBayesClassifier;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import datasetFormat.SparseLineAST;

import edu.berkeley.nlp.util.Pair;

/**
 * MLcomp program interface for DiscretizingNaiveBayesClassifier.
 * 
 * @author Eugene Ma
 */
public class RunForDiscretizingNaiveBayesClassifier {
	protected static final String gHyperparameterFilename = "hyperparameter";
	protected static final String gLearnedModelFilename = "learnedModel.ser";
	
	// *******************************************************************************

	public static void main(String[] args) {
		if (args.length == 0) {
			throw new RuntimeException("The arguments list must be non-empty.");
		}
		
		final String command = args[0];
		if (command.equals("construct")) {
			
		}
		else if (command.equals("setHyperparameter")) {
			setHyperparameter(args);
		}
		else if (command.equals("learn")) {
			learn(args);
		}
		else if (command.equals("semiSupervisedLearn")) {
			semiSupervisedLearn(args);
		}
		else if (command.equals("randomLearn")) {
			randomLearn(args);
		}
		else if (command.equals("generate")) {
			generate(args);
		}
		else if (command.equals("predict")) {
			predict(args);
		}
		else {
			throw new RuntimeException("Unrecognized command: " + command);
		}
	}
	
	// *******************************************************************************

	protected static void setHyperparameter(String[] args) {
		if (args.length != 2) {
			System.out.println("Please specify the hyperparameter value as the second argument.");
		}
		else {
			// Save the hyperparameter to file.
			
			FileOutputStream outputStream;
			try {
				outputStream = new FileOutputStream(new File(gHyperparameterFilename));
			}
			catch (FileNotFoundException x) {
				throw new RuntimeException(
						String.format("Cannot find the file \"%s\".", gHyperparameterFilename));
			}
			final OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream);
			final BufferedWriter out = new BufferedWriter(outputWriter);

			try {
				out.write(args[1]);
				out.newLine();
			}
			catch (IOException e) {
				throw new RuntimeException(
						String.format("An error occurred while writing to file \"%s\".",
								gHyperparameterFilename));
			}
			
			try {
				out.close();
			}
			catch (IOException e) {
				throw new RuntimeException(
						String.format("Error while closing the file \"%s\".", gHyperparameterFilename));
			}
		}
	}
	
	// *******************************************************************************

	protected static void learn(String[] args) {
		if (args.length != 2) {
			System.out.println("Please specify the path for the training data as the second argument.");
		}
		else {
			final String trainingDataFilename = args[1];
			
			try {
				// Parse the sparse training data.
				// Note: "u" outputs for training instances are accepted; they'll just be ignored.
				final Map.Entry<List<SparseLineAST>, Integer> instancesToMaxFeatureIndex
					= SparseLineAST.parseSparseEncodingFromInputFilename(trainingDataFilename,
							new SparseLineAST.SemiSupervisedTrainingValidator(new SparseLineAST.NumericValidator()));
				final List<SparseLineAST> instances = instancesToMaxFeatureIndex.getKey();
				final int maxFeatureIndex = instancesToMaxFeatureIndex.getValue();
				
				// Ignore unlabeled instances.
				final List<SparseLineAST> labeledInstances = SparseLineAST.getLabeledInstances(instances);
				
				// Initialize and train the classifier on the training data.
				DiscretizingNaiveBayesClassifier classifier = new DiscretizingNaiveBayesClassifier();
				classifier.initialize(getKForSmoothing().getSecond());
				classifier.train(labeledInstances, maxFeatureIndex,
						new DiscretizingNaiveBayesClassifier.SimpleBinaryDiscretizer());
				
				// Save the trained model.
				saveTrainedModelFileData(classifier.toFileData());
			}
			catch (RuntimeException x) {
				System.out.println(x.getMessage());
				for (StackTraceElement element : x.getStackTrace()) {
					System.out.println(element);
				}
			}
		}
	}

	// *******************************************************************************

	protected static void semiSupervisedLearn(String[] args) {
		if (args.length < 2) {
			System.out.println("Please specify the path for the training data as the second argument.");
		}
		else {
			final String trainingDataFilename = args[1];
			
			int maxNumIterations;
			if (args.length == 3) {
				maxNumIterations = Integer.parseInt(args[2]);
			}
			else {
				maxNumIterations = Integer.MAX_VALUE;
			}
			
			try {
				// Parse the sparse training data.
				final Map.Entry<List<SparseLineAST>, Integer> instancesToMaxFeatureIndex
					= SparseLineAST.parseSparseEncodingFromInputFilename(trainingDataFilename,
							new SparseLineAST.SemiSupervisedTrainingValidator(
									new SparseLineAST.NumericValidator()));
				final List<SparseLineAST> instances = instancesToMaxFeatureIndex.getKey();
				final int maxFeatureIndex = instancesToMaxFeatureIndex.getValue();
				
				// Initialize and train the classifier on the training data.
				DiscretizingNaiveBayesClassifier classifier
					= DiscretizingNaiveBayesClassifierEMTrainer.train(getKForSmoothing().getSecond(),
							instances, maxFeatureIndex,
							new DiscretizingNaiveBayesClassifier.SimpleBinaryDiscretizer(),
							maxNumIterations);
				
				// Save the trained model.
				saveTrainedModelFileData(classifier.toFileData());
			}
			catch (RuntimeException x) {
				System.out.println(x.getMessage());
			}
		}
	}

	// *******************************************************************************

	protected static void randomLearn(String[] args) {
		if (args.length != 3) {
			System.out.println("Please specify the number of classes as the second argument "
					+ "and the number of features as the third argument.");
		}
		else {
			final int numClassIndices = Integer.parseInt(args[1]);
			final int maxFeatureIndex = Integer.parseInt(args[2]);
			
			// Initialize the classifier to random parameters.
			DiscretizingNaiveBayesClassifier classifier = new DiscretizingNaiveBayesClassifier();
			classifier.initializeRandomly(
					DiscretizingNaiveBayesClassifier.makeClassIndexSet(numClassIndices),
					maxFeatureIndex,
					new DiscretizingNaiveBayesClassifier.SimpleBinaryDiscretizer());
			
			// Save the trained model.
			saveTrainedModelFileData(classifier.toFileData());
		}
	}

	// *******************************************************************************

	protected static void generate(String[] args) {
		if (args.length != 3) {
			System.out.println("Please specify the number of instances as the second argument "
					+ "and the path to save the instances as the third argument.");
		}
		else {
			final int numInstances = Integer.parseInt(args[1]);
			final String dataFilename = args[2];
			
			// Initialize the classifier from the file data.
			DiscretizingNaiveBayesClassifier classifier = new DiscretizingNaiveBayesClassifier();
			classifier.initializeFromFileData(loadTrainedModelFileData());

			SparseLineAST.saveSparseEncoding(dataFilename,
					classifier.generateLabeledInstances(numInstances));
		}
	}

	// *******************************************************************************

	protected static void predict(String[] args) {
		if (args.length != 3) {
			System.out.println("Please specify the path for the test data as the second argument "
					+ "and the path to save the predictions as the third argument.");
		}
		else {
			final String testDataFilename = args[1];
			
			try {
				// Parse the sparse testing data.
				final Map.Entry<List<SparseLineAST>, Integer> instancesToMaxFeatureIndex
					= SparseLineAST.parseSparseEncodingFromInputFilename(testDataFilename,
							new SparseLineAST.NumericValidator());
				final List<SparseLineAST> instances = instancesToMaxFeatureIndex.getKey();
				final int maxFeatureIndex = instancesToMaxFeatureIndex.getValue();
				
				// Initialize the classifier from the file data.
				DiscretizingNaiveBayesClassifier classifier = new DiscretizingNaiveBayesClassifier();
				classifier.initializeFromFileData(loadTrainedModelFileData());

				// Make predictions.
				int numCorrectPredictions = 0;
				List<Integer> predictedClassIndices = new ArrayList<Integer>(instances.size());
				for (SparseLineAST instance : instances) {
					final int predictedClassIndex = classifier.getPrediction(instance);
					predictedClassIndices.add(predictedClassIndex);
					
					final int correctClassIndex = Integer.parseInt(instance.getOutput());
					if (predictedClassIndex == correctClassIndex) {
						numCorrectPredictions += 1;
					}
				}
				
				System.out.println(String.format("%d of %d predicted correctly (%f).",
						numCorrectPredictions, predictedClassIndices.size(),
						numCorrectPredictions / (double) predictedClassIndices.size()));
				
				// Save the predictions to file.
				final String predictionsFilename = args[2];
				savePredictions(predictionsFilename, predictedClassIndices);
			}
			catch (RuntimeException x) {
				System.out.println(x.getMessage());
			}
		}
	}

	// *******************************************************************************

	protected static Pair<Boolean, Double> getKForSmoothing() {
		// Returns the hyperparameter (k for additive smoothing) loaded from file.
		// If fail, return false and 0 for the hyperparameter.
		
		try {
			final FileInputStream fileStream = new FileInputStream(gHyperparameterFilename);
			final InputStreamReader inputReader = new InputStreamReader(fileStream);
			final BufferedReader in = new BufferedReader(inputReader);
			
			final String hyperparameterString = in.readLine();
			final double hyperparameter = Double.parseDouble(hyperparameterString.trim());
			// TODO: Should we limit the k for additive smoothing to be less than or equal to 1?
			System.out.println(String.format("Loaded hyperparameter = %f. Using k = %f for smoothing.",
					hyperparameter, hyperparameter / 100.0));
			return new Pair<Boolean, Double>(true, hyperparameter / 100.0);
		}
		catch (FileNotFoundException x) {
			
		}
		catch (IOException x) {
			
		}
		catch (NumberFormatException x) {
			
		}
		
		System.out.println("Cannot load hyperparameter. Using k = 0.0 for smoothing.");
		return new Pair<Boolean, Double>(false, 0.0);
	}
	
	protected static DiscretizingNaiveBayesClassifier.FileData loadTrainedModelFileData() {
		// Load the trained model.
		try {
			// Try loading.
			
			FileInputStream fileStream = new FileInputStream(gLearnedModelFilename);
			ObjectInputStream os = new ObjectInputStream(fileStream);

			DiscretizingNaiveBayesClassifier.FileData trainedModelFileData
				= (DiscretizingNaiveBayesClassifier.FileData) os.readObject();
			
			final String message = "\"" + gLearnedModelFilename + "\" loaded.";
			System.out.println(message);
				
			os.close();
			
			return trainedModelFileData;
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException();
		}
		catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException();
		}
	}
	
	protected static void saveTrainedModelFileData(DiscretizingNaiveBayesClassifier.FileData fileData) {
		try {
			FileOutputStream fileStream = new FileOutputStream(gLearnedModelFilename);
			ObjectOutputStream os = new ObjectOutputStream(fileStream);
			
			os.writeObject(fileData);
			
			os.close();
			
			final String message = String.format("Learned model saved to \"%s\".",
					gLearnedModelFilename);
			System.out.println(message);
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException();
		}
	}
	
	protected static void savePredictions(String predictionsFilename, List<Integer> predictedClassIndices) {
		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream(new File(predictionsFilename));
		}
		catch (FileNotFoundException x) {
			throw new RuntimeException(String.format("Cannot find the file \"%s\".", predictionsFilename));
		}
		final OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream);
		final BufferedWriter out = new BufferedWriter(outputWriter);

		int lineNumber = 0;
		for (int predictedClassIndex : predictedClassIndices) {
			try {
				out.write(String.format("%d", predictedClassIndex));
				out.newLine();
			}
			catch (IOException e) {
				throw new RuntimeException(
						String.format("An error occurred while writing line %d to file \"%s\".",
								lineNumber, predictionsFilename));
			}
			
			++lineNumber;
		}
		
		try {
			out.close();
		}
		catch (IOException e) {
			throw new RuntimeException(
					String.format("Error while closing the file \"%s\".", predictionsFilename));
		}
	}
}
