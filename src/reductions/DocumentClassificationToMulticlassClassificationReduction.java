package reductions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import datasetFormat.DocumentDataset;
import datasetFormat.SparseLineAST;

/**
 * Reduction from DocumentClassification to MulticlassClassification.
 * 
 * @author Eugene Ma
 */
public class DocumentClassificationToMulticlassClassificationReduction {
	//protected static final String gHyperparameterFilename = "hyperparameter";	// TODO: Include?
	
	protected static final String gStateFilename = "state.ser";
	
	protected static final String gTempTrainDatashardForExtractedFeaturesFilename = "temp-train";
	protected static final String gTempTestDatashardForExtractedFeaturesFilename = "temp-test";
	protected static final String gTempPredictionsForMulticlassClassificationFilename = "temp-predictions";
	
	// *******************************************************************************
	
	protected static class State implements Serializable {
		private static final long serialVersionUID = 9051798346483600106L;
		
		// TODO: Have data hiding?
		public String multiclassClassifierFilename = "";
		public String featureExtractorFilename = "";
		public ArrayList<String> classIndicesToLabels = new ArrayList<String>();
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			
			sb.append(String.format("multiclassClassifierFilename: %s\n", multiclassClassifierFilename));
			sb.append(String.format("featureExtractorFilename: %s\n", featureExtractorFilename));
			sb.append(String.format("classIndicesToLabels: %s\n", classIndicesToLabels));
			
			return sb.toString();
		}
	}

	// -------------------------------------------------------------------------------
	
	protected static void saveState(State state) {
		try {
			FileOutputStream fileStream = new FileOutputStream(gStateFilename);
			ObjectOutputStream os = new ObjectOutputStream(fileStream);
			
			os.writeObject(state);
			
			os.close();
			
			final String message = String.format("State saved to \"%s\".", gStateFilename);
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
	
	// -------------------------------------------------------------------------------

	protected static State loadState() {
		// Load the state.
		try {
			// Try loading.
			
			FileInputStream fileStream = new FileInputStream(gStateFilename);
			ObjectInputStream os = new ObjectInputStream(fileStream);

			State state = (State) os.readObject();
			
			final String message = "\"" + gStateFilename + "\" loaded.";
			System.out.println(message);
				
			os.close();
			
			return state;
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
	
	// *******************************************************************************

	public static void main(String[] args) {
		if (args.length == 0) {
			throw new RuntimeException("The arguments list must be non-empty.");
		}
		
		final String command = args[0];
		if (command.equals("construct")) {
			construct(args);
		}
		else if (command.equals("setHyperparameter")) {
			setHyperparameter(args);
		}
		else if (command.equals("learn")) {
			learn(args);
		}
		else if (command.equals("predict")) {
			predict(args);
		}
		else {
			throw new RuntimeException("Unrecognized command: " + command);
		}
	}
	
	// *******************************************************************************
	
	protected static void construct(String[] args) {
		if (args.length != 3) {
			System.out.println("Please specify the path for the multiclass classifier as the second argument, "
					+ "and the path for the DocumentClassification-to-MulticlassClassification feature extractor "
					+ "as the third argument.");
		}
		else {
			final String multiclassClassifierFilename = args[1];
			final String featureExtractorFilename = args[2];
			
			// Initialize the state.
			final State state = new State();
			state.multiclassClassifierFilename = multiclassClassifierFilename;
			state.featureExtractorFilename = featureExtractorFilename;
			
			// Save the state.
			saveState(state);
		}
	}
	
	// *******************************************************************************

	protected static void setHyperparameter(String[] args) {
		if (args.length != 2) {
			System.out.println("Please specify the hyperparameter value as the second argument.");
		}
		else {
			final State state = loadState();
			
			// Set the hyperparameter of the MulticlassClassification classifier.
			final String hyperparameter = args[1];
			try {
				String[] cmd = {"java", "-jar", state.multiclassClassifierFilename,
						"setHyperparameter", hyperparameter};
				Runtime.getRuntime().exec(cmd).waitFor();
			}
			catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException();
			}
		}
	}
	
	// *******************************************************************************

	protected static void learn(String[] args) {
		if (args.length != 2) {
			System.out.println("Please specify the path for the training data as the second argument.");
		}
		else {
			final State state = loadState();
			
			final String trainingDataFilename = args[1];
			
			// Extract features
			// (i.e., convert from DocumentClassification dataset to MulticlassClassification dataset).
			try {
				String[] cmd = {"java", "-jar", state.featureExtractorFilename,
						"extract", trainingDataFilename, gTempTrainDatashardForExtractedFeaturesFilename};
				Runtime.getRuntime().exec(cmd).waitFor();
			}
			catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException();
			}

			// Train the classifier on the extracted features.
			try {
				String[] cmd = {"java", "-jar", state.multiclassClassifierFilename,
						"learn", gTempTrainDatashardForExtractedFeaturesFilename};
				Runtime.getRuntime().exec(cmd).waitFor();
			}
			catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException();
			}
			
			// Remember the document labels (in order) to be able to map
			// the MulticlassClassification class indices back to the document labels.
			final DocumentDataset trainingDocumentDataset = new DocumentDataset(new File(trainingDataFilename));
			state.classIndicesToLabels.clear();
			state.classIndicesToLabels.add("");	// Dummy label for index zero.
			for (Map.Entry<String, List<File>> labelsToDocumentLists
					: trainingDocumentDataset.getOrderedLabelsToDocumentLists().entrySet())
			{
				state.classIndicesToLabels.add(labelsToDocumentLists.getKey());
			}
			
			saveState(state);
		}
	}
	
	// *******************************************************************************

	protected static void predict(String[] args) {
		if (args.length != 3) {
			System.out.println("Please specify the path for the test data as the second argument "
					+ "and the path to save the predictions as the third argument.");
		}
		else {
			final State state = loadState();
			
			final String testDataFilename = args[1];
			
			// Extract features
			// (i.e., convert from DocumentClassification dataset to MulticlassClassification dataset).
			try {
				String[] cmd = {"java", "-jar", state.featureExtractorFilename,
						"extract", testDataFilename, gTempTestDatashardForExtractedFeaturesFilename};
				Runtime.getRuntime().exec(cmd).waitFor();
			}
			catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException();
			}

			// Use the classifier to make predictions.
			try {
				String[] cmd = {"java", "-jar", state.multiclassClassifierFilename,
						"predict",
							gTempTestDatashardForExtractedFeaturesFilename,
							gTempPredictionsForMulticlassClassificationFilename};
				Runtime.getRuntime().exec(cmd).waitFor();
			}
			catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException();
			}
			
			// Initialize the data structure for the DocumentClassification predictions.
			final Map<String, List<String>> labelsToDocumentStringLists = new HashMap<String, List<String>>();
			for (int classIndex = 1; classIndex < state.classIndicesToLabels.size(); ++classIndex) {
				labelsToDocumentStringLists.put(state.classIndicesToLabels.get(classIndex), new ArrayList<String>());
			}
			
			// Get the names of the test documents in lexicographical order.
			final DocumentDataset testDocumentDataset = new DocumentDataset(new File(testDataFilename));
			
			// Convert the MulticlassClassification predictions into DocumentClassification predictions.
			List<SparseLineAST> predictions = SparseLineAST.parseSparseEncodingFromInputFilename(
					gTempPredictionsForMulticlassClassificationFilename,
					new SparseLineAST.BinaryOrMulticlassValidator()).getKey();
			Iterator<SparseLineAST> predictionIterator = predictions.iterator();
			for (List<File> testDocuments : testDocumentDataset.getOrderedLabelsToDocumentLists().values()) {
				for (File testDocument : testDocuments) {
					if (!predictionIterator.hasNext()) {
						throw new RuntimeException("Mismatch between test documents and multiclass predictions!");
					}
					else {
						final SparseLineAST multiclassPrediction = predictionIterator.next();
						final int classIndex = Integer.parseInt(multiclassPrediction.getOutput());
						
						// Convert classIndex into label.
						final String label = state.classIndicesToLabels.get(classIndex);
						
						labelsToDocumentStringLists.get(label).add(testDocument.getName());
					}
				}
			}
			
			// Save the predictions to file.
			final String predictionsFilename = args[2];
			final DocumentDataset predictionDocumentDataset
				= new DocumentDataset(predictionsFilename, labelsToDocumentStringLists);
			predictionDocumentDataset.saveStructure(predictionsFilename);
		}
	}
}
