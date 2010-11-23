package datasetFormat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents DocumentClassification dataset.
 * Invariant: The labels and document lists are sorted in lexicographical order
 * for the DocumentClassification-to-MulticlassClassification reduction to work.
 * 
 * @author Eugene Ma
 */
public class DocumentDataset {
	protected Map<String, List<File>> myLabelsToDocumentLists = new LinkedHashMap<String, List<File>>();
	
	// *******************************************************************************

	public DocumentDataset(File rawDirectory) {
		final List<File> labelDirectories = Arrays.asList(rawDirectory.listFiles());
		// Needs to be sorted to map to (MulticlassClassification) class indices correctly
		// (for DocumentClassification-to-MulticlassClassification reduction to work).
		Collections.sort(labelDirectories, new FileComparator());
		for (File labelDirectory : labelDirectories) {
			if (labelDirectory.isDirectory()) {
				final String label = labelDirectory.getName();
				final List<File> labelDocuments = new ArrayList<File>();
				
				// Filter the files in the label directory to include only documents (i.e., no directories).
				final File[] uncheckedLabelDocuments = labelDirectory.listFiles();
				for (File labelDocument : uncheckedLabelDocuments) {
					if (labelDocument.isFile()) {
						labelDocuments.add(labelDocument);
					}
					else {
						// TODO: Print error message.
					}
				}
				
				// Needs to be sorted to map to (MulticlassClassification) class indices correctly
				// (for DocumentClassification-to-MulticlassClassification reduction to work).
				Collections.sort(labelDocuments, new FileComparator());
				
				myLabelsToDocumentLists.put(label, Collections.unmodifiableList(labelDocuments));
			}
			else {
				// TODO: Print error message.
			}
		}
	}
	
	// -------------------------------------------------------------------------------
	
	public DocumentDataset(String basePath, Map<String, List<String>> labelsToDocumentStringLists) {
		final List<String> labels = new ArrayList<String>(labelsToDocumentStringLists.keySet());
		// Needs to be sorted to map to (MulticlassClassification) class indices correctly
		// (for DocumentClassification-to-MulticlassClassification reduction to work).
		Collections.sort(labels);
		for (String label : labels) {
			// Convert the document strings into actual files.
			final List<File> labelDocuments = new ArrayList<File>();
			for (String labelDocumentString : labelsToDocumentStringLists.get(label)) {
				labelDocuments.add(new File(basePath + "\\" + labelDocumentString));
			}
			
			// Needs to be sorted to map to (MulticlassClassification) class indices correctly
			// (for DocumentClassification-to-MulticlassClassification reduction to work).
			Collections.sort(labelDocuments, new FileComparator());
			
			myLabelsToDocumentLists.put(label, Collections.unmodifiableList(labelDocuments));
		}
	}
	
	// -------------------------------------------------------------------------------
	
	protected static class FileComparator implements Comparator<File> {
		@Override
		public int compare(File file1, File file2) {
			// TODO: Make non-system dependent.
			return file1.compareTo(file2);
		}
	}

	// *******************************************************************************

	public Map<String, List<File>> getOrderedLabelsToDocumentLists() {
		return Collections.unmodifiableMap(myLabelsToDocumentLists);
	}
	
	public void saveStructure(String newBasePath) {
		// Save the structure of the dataset with the given base path.
		// The documents saved are empty.
		// TODO: Better to have the base path as part of the document dataset class.
		
		final File baseDirectory = new File(newBasePath);
		baseDirectory.mkdir();
		
		for (String label : myLabelsToDocumentLists.keySet()) {
			final File labelDirectory = new File(newBasePath + "\\" + label);
			labelDirectory.mkdir();
			
			for (File document : myLabelsToDocumentLists.get(label)) {
				final File rebasedDocument = new File(newBasePath + "\\" + label + "\\" + document.getName());
				try {
					rebasedDocument.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException();
				}
			}
		}
	}
}
