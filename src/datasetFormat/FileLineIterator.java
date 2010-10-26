package datasetFormat;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

/**
 * Abstracts reading a file as an iterator.
 * Implementation note: Before a line is returned, the next line is read.
 * Thus, if there was an error reading the next line,
 * the error message is delayed until an attempt to fetch the line is made.
 * 
 * @author Eugene Ma
 */
public class FileLineIterator implements Iterator<String> {
	protected String myFilename;
	protected BufferedReader myIn;
	
	protected boolean myErrorReadingLineFlag = false;
	protected int myNextLineNumber = 0;
	protected String myNextLine;
	
	public FileLineIterator(String filename) throws FileNotFoundException {
		myFilename = filename;
		
		final FileInputStream inputStream = new FileInputStream(new File(filename));
		final InputStreamReader inputReader = new InputStreamReader(inputStream);
		myIn = new BufferedReader(inputReader);
		
		try {
			// Priming read.
			myNextLine = myIn.readLine();
		}
		catch (IOException x) {
			myErrorReadingLineFlag = true;
		}
	}
	
	protected void finalize() throws Throwable {
		close();
	}
	
	public void close() throws IOException {
		myIn.close();
	}

	@Override
	public boolean hasNext() {
		return myErrorReadingLineFlag || myNextLine != null;
	}

	@Override
	public String next() {
		if (myErrorReadingLineFlag) {
			throw new RuntimeException(
					String.format("Error reading line %d from file \"%s\".", myNextLineNumber, myFilename));
		}
		
		final String previousLine = myNextLine;
		
		try {
			++myNextLineNumber;
			myNextLine = myIn.readLine();
		}
		catch (IOException x) {
			myErrorReadingLineFlag = true;
		}
		
		return previousLine;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException(
				String.format("You cannot remove from the read-only file \"%s\".", myFilename));
	}
}
