package reductions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
public class SimpleDocumentDatasetToMulticlassDatasetReduction {
	protected List<SparseLineAST> myInstances = new ArrayList<SparseLineAST>();
	
	// Two-way mapping: labels to class indices.
	protected List<String> myClassIndicesToLabels = new ArrayList<String>();
	protected Map<String, Integer> myLabelsToClassIndices = new HashMap<String, Integer>();
	
	// Two-way mapping: words to feature indices.
	protected List<String> myFeatureIndicesToWords = new ArrayList<String>();
	protected Map<String, Integer> myWordsToFeatureIndices = new HashMap<String, Integer>();
	
	// *******************************************************************************

	public SimpleDocumentDatasetToMulticlassDatasetReduction(DocumentDataset documentDataset) {
		// Create two-way mappings...

		final Map<String, List<File>> labelsToDocumentLists = documentDataset.getLabelsToDocumentLists();

		// ...for labels to class indices:
		myClassIndicesToLabels.add("");	// Dummy for zeroth index.
		int nextClassIndex = 1;
		for (String label : labelsToDocumentLists.keySet()) {
			myClassIndicesToLabels.add(label);
			
			myLabelsToClassIndices.put(label, nextClassIndex);
			nextClassIndex++;
		}
		
		// ...for words to feature indices:
		// Also, create an instance for each document.
		myFeatureIndicesToWords.add("");	// Dummy for zeroth index.
		int nextFeatureIndex = 1;
		for (String label : labelsToDocumentLists.keySet()) {
			for (File document : labelsToDocumentLists.get(label)) {
				final Counter<String> documentWordCounter = getDocumentWordCounter(document);
				
				// Begin turning the document into an instance by giving it an output (label/class index).
				final SparseLineAST instance = new SparseLineAST(myLabelsToClassIndices.get(label).toString(),
						new SparseLineAST.BinaryOrMulticlassValidator());
				
				for (String word : documentWordCounter.keySet()) {
					// Add the word as a feature if this is the first time we see the word.
					// (This is a two-way mapping from words to feature indices.)
					if (!myWordsToFeatureIndices.containsKey(word)) {
						myFeatureIndicesToWords.add(word);
						
						myWordsToFeatureIndices.put(word, nextFeatureIndex);
						nextFeatureIndex++;
					}
					
					// Add the word count as a feature value to the instance.
					instance.addFeatureIndexToFeatureValue(
							myWordsToFeatureIndices.get(word),
							documentWordCounter.getCount(word));
				}
				
				myInstances.add(instance);
			}
		}
	}
	
	// *******************************************************************************

	public List<SparseLineAST> getInstances() {
		return Collections.unmodifiableList(myInstances);
	}
	
	public int getMaxFeatureIndex() {
		return myFeatureIndicesToWords.size() - 1;
	}
	
	// -------------------------------------------------------------------------------

	// Two-way mapping: labels to class indices.
	
	public List<String> getClassIndicesToLabels() {
		return Collections.unmodifiableList(myClassIndicesToLabels);
	}
	
	public Map<String, Integer> getLabelsToClassIndices() {
		return Collections.unmodifiableMap(myLabelsToClassIndices);
	}
	
	// -------------------------------------------------------------------------------
	
	// Two-way mapping: words to feature indices.
	
	public List<String> getFeatureIndicesToWords() {
		return Collections.unmodifiableList(myFeatureIndicesToWords);
	}
	
	public Map<String, Integer> getWordsToFeatureIndices() {
		return Collections.unmodifiableMap(myWordsToFeatureIndices);
	}

	// *******************************************************************************

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
			else if (command.equals("reduce")) {
				reduce(args);
			}
			else {
				throw new RuntimeException(String.format("The command \"%s\" is not recognized.", command));
			}
		}
	}
	
	// *******************************************************************************

	public static void reduce(String[] args) {
		if (args.length != 3) {
			throw new RuntimeException(
					"The raw DocumentClassification datashard path must be the second argument, "
							+ "and the output MulticlassClassification datashard path must be the third argument.");
		}
		else {
			final String documentDatashardPath = args[1];
			final String multiclassDatashardPath = args[2];

			final DocumentDataset documentDataset = new DocumentDataset(new File(documentDatashardPath));
			final SimpleDocumentDatasetToMulticlassDatasetReduction reduction
				= new SimpleDocumentDatasetToMulticlassDatasetReduction(documentDataset);
			
			SparseLineAST.saveSparseEncoding(multiclassDatashardPath, reduction.getInstances());
			
			final boolean showIndicesFlag = true;	// TODO
			
			// Save the label values.
			saveOneBasedStringList("label-values", showIndicesFlag, reduction.getClassIndicesToLabels());
			
			// Save the feature names.
			saveOneBasedStringList("feature-names", showIndicesFlag, reduction.getFeatureIndicesToWords());
		}
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
