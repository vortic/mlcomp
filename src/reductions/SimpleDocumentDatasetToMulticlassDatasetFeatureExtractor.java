package reductions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import datasetFormat.DocumentDataset;
import datasetFormat.FileLineIterator;
import datasetFormat.SparseLineAST;
import edu.berkeley.nlp.util.Counter;

/**
 * Bag-of-words reduction: features indices are the words and feature values are word counts.
 * 
 * @author Eugene Ma
 */
public class SimpleDocumentDatasetToMulticlassDatasetFeatureExtractor {
	protected static final String gStateFilename = "simple-doc2multi-state.ser";

	protected static class State implements Serializable {
		private static final long serialVersionUID = 1288784871196184830L;
		
		// Two-way mapping: labels to class indices.
		protected ArrayList<String> myClassIndicesToLabels = new ArrayList<String>();
		protected HashMap<String, Integer> myLabelsToClassIndices = new HashMap<String, Integer>();
		protected int myNextClassIndex = 1;
		
		// Two-way mapping: words to feature indices.
		protected ArrayList<String> myFeatureIndicesToWords = new ArrayList<String>();
		protected HashMap<String, Integer> myWordsToFeatureIndices = new HashMap<String, Integer>();
		protected int myNextFeatureIndex = 1;
		
		// *******************************************************************************
		
		public State() {
			myClassIndicesToLabels.add("");	// Dummy for zeroth index.
			myFeatureIndicesToWords.add("");	// Dummy for zeroth index.
		}
		
		// *******************************************************************************

		// Two-way mapping: labels to class indices.
		
		public void addLabelIfNew(String label) {
			if (!myLabelsToClassIndices.containsKey(label)) {
				myClassIndicesToLabels.add(label);
				
				myLabelsToClassIndices.put(label, myNextClassIndex);
				myNextClassIndex++;
			}
		}
		
		public List<String> getClassIndicesToLabels() {
			return Collections.unmodifiableList(myClassIndicesToLabels);
		}
		
		public Map<String, Integer> getLabelsToClassIndices() {
			return Collections.unmodifiableMap(myLabelsToClassIndices);
		}
		
		// -------------------------------------------------------------------------------
		
		// Two-way mapping: words to feature indices.
		
		public void addWordIfNew(String word) {
			// Add the word as a feature if this is the first time we see the word.
			// (This is a two-way mapping from words to feature indices.)
			if (!myWordsToFeatureIndices.containsKey(word)) {
				myFeatureIndicesToWords.add(word);
				
				myWordsToFeatureIndices.put(word, myNextFeatureIndex);
				myNextFeatureIndex++;
			}
		}
		
		public List<String> getFeatureIndicesToWords() {
			return Collections.unmodifiableList(myFeatureIndicesToWords);
		}
		
		public Map<String, Integer> getWordsToFeatureIndices() {
			return Collections.unmodifiableMap(myWordsToFeatureIndices);
		}
		
		public int getMaxFeatureIndex() {
			return myNextFeatureIndex - 1;
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
			return new State();	// Create it if it does not exist.
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
			throw new RuntimeException("The arguments list must not be empty!");
		}
		else {
			final String command = args[0];
			if (command.equals("construct")) {
				construct(args);
			}
			else if (command.equals("extract")) {
				extract(args);
			}
			else {
				throw new RuntimeException(String.format("The command \"%s\" is not recognized.", command));
			}
		}
	}
	
	// *******************************************************************************
	
	public static void construct(String[] args) {
		// Do nothing.
	}
	
	// *******************************************************************************

	public static void extract(String[] args) {
		if (args.length != 3) {
			throw new RuntimeException(
					"The raw DocumentClassification datashard path must be the second argument, "
							+ "and the output MulticlassClassification datashard path must be the third argument.");
		}
		else {
			final State state = loadState();
			
			final String documentDatashardPath = args[1];
			final String multiclassDatashardPath = args[2];

			final DocumentDataset documentDataset = new DocumentDataset(new File(documentDatashardPath));
			
			// Create two-way mappings...

			final Map<String, List<File>> labelsToDocumentLists = documentDataset.getOrderedLabelsToDocumentLists();

			// ...for labels to class indices:
			for (String label : labelsToDocumentLists.keySet()) {
				state.addLabelIfNew(label);
			}
			
			// ...for words to feature indices:
			// Also, create an instance for each document.
			List<SparseLineAST> instances = new ArrayList<SparseLineAST>();
			for (String label : labelsToDocumentLists.keySet()) {
				for (File document : labelsToDocumentLists.get(label)) {
					final Counter<String> documentWordCounter = getDocumentWordCounter(document);
					
					// Begin turning the document into an instance by giving it an output (label/class index).
					final SparseLineAST instance
						= new SparseLineAST(state.getLabelsToClassIndices().get(label).toString(),
							new SparseLineAST.BinaryOrMulticlassValidator());
					
					for (String word : documentWordCounter.keySet()) {
						state.addWordIfNew(word);
						
						// Add the word count as a feature value to the instance.
						instance.addFeatureIndexToFeatureValue(
								state.getWordsToFeatureIndices().get(word),
								documentWordCounter.getCount(word));
					}
					
					instances.add(instance);
				}
			}
			
			SparseLineAST.saveSparseEncoding(multiclassDatashardPath, instances);
			
			final boolean showIndicesFlag = true;	// TODO
			
			// Save the label values.
			saveOneBasedStringList("label-values", showIndicesFlag, state.getClassIndicesToLabels());
			
			// Save the feature names.
			saveOneBasedStringList("feature-names", showIndicesFlag, state.getFeatureIndicesToWords());
			
			saveState(state);
		}
	}
	
	// -------------------------------------------------------------------------------
	
	public static Counter<String> getDocumentWordCounter(File document) {
		FileLineIterator inputIterator;
		try {
			inputIterator = new FileLineIterator(document.getPath());
		}
		catch (FileNotFoundException x) {
			throw new RuntimeException(
					String.format("Cannot find the file \"%s\".", document.getPath()));
		}
		
		// Count the words in the document.
		Counter<String> documentWordCounts = new Counter<String>();
		while (inputIterator.hasNext()) {
			final String line = inputIterator.next();
			final String[] words = line.trim().split("\\s+");
			for (String word : words) {
				documentWordCounts.incrementCount(word, 1);
			}
		}
		
		try {
			inputIterator.close();
		} catch (IOException e) {
			throw new RuntimeException(
					String.format("Error while closing the file \"%s\".", document.getPath()));
		}
		
		return documentWordCounts;
	}

	// -------------------------------------------------------------------------------

	public static void saveOneBasedStringList(String filename, boolean showIndicesFlag, List<String> stringList) {
		// Each string in the list is saved to its own line in order.
		// The zeroth element is excluded (thus, "one-based").
		// There is a flag to print the indices in the format, "5: fifthWord";
		// otherwise, only the word will be printed, as in "fifthWord".
		
		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream(new File(filename));
		}
		catch (FileNotFoundException x) {
			throw new RuntimeException(String.format("Cannot find the file \"%s\".", filename));
		}
		final OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream);
		final BufferedWriter out = new BufferedWriter(outputWriter);

		try {
			Iterator<String> iterator = stringList.iterator();
			
			// Skip over the zeroth element.
			if (iterator.hasNext()) {
				iterator.next();
			}
			
			// Output the other elements, one on each line.
			int nextIndex = 1;
			while (iterator.hasNext()) {
				final String string = iterator.next();
				
				if (showIndicesFlag) {
					out.write(nextIndex + ": ");
				}
				nextIndex++;
				
				out.write(string);
				out.newLine();
			}
		}
		catch (IOException e) {
			throw new RuntimeException(
					String.format("An error occurred while writing to file \"%s\".", filename));
		}

		try {
			out.close();
		}
		catch (IOException e) {
			throw new RuntimeException(
					String.format("Error while closing the file \"%s\".", filename));
		}
	}
}
