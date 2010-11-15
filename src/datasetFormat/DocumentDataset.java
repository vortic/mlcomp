package datasetFormat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Eugene Ma
 */
public class DocumentDataset {
	protected Map<String, List<File>> myLabelsToDocumentLists = new HashMap<String, List<File>>();
	
	public DocumentDataset(File rawDirectory) {
		final File[] labelDirectories = rawDirectory.listFiles();
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
				
				myLabelsToDocumentLists.put(label, Collections.unmodifiableList(labelDocuments));
			}
			else {
				// TODO: Print error message.
			}
		}
	}
	
	public Map<String, List<File>> getLabelsToDocumentLists() {
		return Collections.unmodifiableMap(myLabelsToDocumentLists);
	}
}
