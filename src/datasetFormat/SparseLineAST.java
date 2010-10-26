package datasetFormat;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import functions.UnaryPredicate;

/**
 * Represents a sparse encoding of an instance for MLcomp dataset formats
 * such as those for BinaryClassification, MulticlassClassification, and Regression.
 * E.g., "-1 5:8 1:-3".
 * The output can be any string;
 * a validator (just a unary predicate) needs to be given to validate
 * that the output is valid for the desired domain.
 * For example, "u" is a valid output for an instance of the SemiSupervised learning domain.
 * 
 * @author Eugene Ma
 */
public class SparseLineAST {
	public static class NumericValidator implements UnaryPredicate<String> {
		@Override
		public boolean test(String output) {
			// Just make sure the output is not definitely invalid (non-numeric).
			// TODO: Need a better check.
			
			try {
				Integer.parseInt(output);
			}
			catch (NumberFormatException intException) {
				try {
					Double.parseDouble(output);
				}
				catch (NumberFormatException doubleException) {
					//throw new RuntimeException(
					//		String.format("The output \"%s\" is definitely not valid.", output));
					return false;
				}
			}
			
			return true;
		}
	}
	
	public static class BinaryOrMulticlassValidator implements UnaryPredicate<String> {
		// All the class indices we have ever seen and validated.
		protected Set<Integer> myClassIndices = new HashSet<Integer>();

		@Override
		public boolean test(String output) {
			try {
				final int outputClassIndex = Integer.parseInt(output);
				if (outputClassIndex < -1) {
					return false;
				}
				else if (outputClassIndex == -1) {
					// If a -1 is used, it must be binary classification.
					if (myClassIndices.contains(-1)) {
						// We know it was binary classification before if we had seen a -1 before.
						return true;
					}
					else {
						// Make sure we didn't think it was multiclass classification before.
						for (int classIndex : myClassIndices) {
							if (classIndex != -1 && classIndex != 1) {
								return false;
							}
						}
						
						myClassIndices.add(-1);
						return true;
					}
				}
				else {
					// If a number greater than or equal to 1 is used,
					// it can be binary or multiclass classification.
					if (outputClassIndex == 1) {
						// One is okay for either binary or multiclass classification.
						myClassIndices.add(outputClassIndex);
						return true;
					}
					else {
						// Make sure we didn't think it wasn't binary classification before.
						if (!myClassIndices.contains(-1)) {
							myClassIndices.add(outputClassIndex);
							return true;
						}
						else {
							return false;
						}
					}
				}
			}
			catch (NumberFormatException intException) {
				return false;
			}
		}
	}
	
	public static class SemiSupervisedTrainingValidator implements UnaryPredicate<String> {
		protected int myNumLabeledInstances = 0;
		protected int myNumUnlabeledInstances = 0;
		protected UnaryPredicate<String> myLabeledOutputValidator;
		
		public SemiSupervisedTrainingValidator(UnaryPredicate<String> labeledOutputValidator) {
			if (labeledOutputValidator == null) {
				throw new RuntimeException("The labeled output validator cannot be null.");
			}
			else {
				myLabeledOutputValidator = labeledOutputValidator;
			}
		}
		
		@Override
		public boolean test(String output) {
			if (output.equals("u")) {
				myNumUnlabeledInstances++;
				return true;
			}
			else {
				if (myLabeledOutputValidator.test(output)) {
					myNumLabeledInstances++;
					return true;
				}
				else {
					return false;
				}
			}
		}
		
		public boolean datasetIsValid() {
			// Only valid if there is at least one unlabeled instance
			// and at least one labeled instance.
			return myNumLabeledInstances > 0 && myNumUnlabeledInstances > 0;
		}
		
		public int getNumLabeledInstances() {
			return myNumLabeledInstances;
		}
		
		public int getNumUnlabeledInstances() {
			return myNumUnlabeledInstances;
		}
	}
	
	// *******************************************************************************
	
	private String myOutput;
	private Map<Integer, Double> myFeatureIndicesToFeatureValues = new HashMap<Integer, Double>();
	private int myMaxFeatureIndex = 0;
	
	// *******************************************************************************

	public SparseLineAST(String output, UnaryPredicate<String> outputValidator) {
		//maybeValidateOutput(output);	// TODO: Fix and uncomment.
		if (!outputValidator.test(output)) {
			throw new RuntimeException("The output is not valid: " + output);
		}
		myOutput = output;
	}
	
	// *******************************************************************************
	
	public void addFeatureIndexToFeatureValueFromToken(String token) {
		// Precondition: The token is trimmed (i.e., no leading or following whitespace).
		
		String[] featureIndexAndFeatureValueArray = token.split(":");
		if (featureIndexAndFeatureValueArray.length != 2) {
			throw new RuntimeException(
					String.format("\"%s\" should be in the format \"featureIndex:featureValue\" "
							+ "where \"featureIndex\" is a positive integer "
							+ "and \"featureValue\" is a real number.",
							token));
		}
		else {
			final String featureIndexToken = featureIndexAndFeatureValueArray[0];
			final String featureValueToken = featureIndexAndFeatureValueArray[1];
			
			// Get and validate the feature index.
			int featureIndex;
			try {
				featureIndex = Integer.parseInt(featureIndexToken);
			}
			catch (NumberFormatException x) {
				throw new RuntimeException(
						String.format("The feature index \"%s\" must be a positive integer.",
								featureIndexToken));
			}
			if (featureIndex <= 0) {
				throw new RuntimeException(
						String.format("The feature index \"%s\" must be a positive integer.",
								featureIndexToken));
			}
			
			// Get and validate the feature value.
			double featureValue;
			try {
				featureValue = Double.parseDouble(featureValueToken);
			}
			catch (NumberFormatException x) {
				throw new RuntimeException(
						String.format("The feature value \"%s\" must be a real number.",
								featureValueToken));
			}
			
			addFeatureIndexToFeatureValue(featureIndex, featureValue);
		}
	}
	
	public void addFeatureIndexToFeatureValue(int featureIndex, double featureValue) {
		Double previousFeatureValue = myFeatureIndicesToFeatureValues.get(featureIndex);
		if (previousFeatureValue != null) {
			throw new RuntimeException(String.format("The feature value %f "
					+ "collides with a previous feature value %f at feature index %d.",
					featureValue, previousFeatureValue, featureIndex));
		}
		else {
			if (featureIndex > myMaxFeatureIndex) {
				myMaxFeatureIndex = featureIndex;
			}
			
			if (featureValue != 0) {
				myFeatureIndicesToFeatureValues.put(featureIndex, featureValue);
			}
		}
	}
	
	// -------------------------------------------------------------------------------
	
	public String getOutput() {
		return myOutput;
	}
	
	public double getFeatureValueFromFeatureIndex(int featureIndex) {
		final Double featureValue = myFeatureIndicesToFeatureValues.get(featureIndex);
		return (featureValue == null)? 0:featureValue.doubleValue();
	}
	
	public int getMaxFeatureIndex() {
		return myMaxFeatureIndex;
	}
	
	// -------------------------------------------------------------------------------

	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(getOutput());
		
		// Output the featureIndex:featureValue pairs sorted by featureIndex.
		final List<Integer> sortedFeatureIndices
			= new ArrayList<Integer>(myFeatureIndicesToFeatureValues.keySet());
		Collections.sort(sortedFeatureIndices);
		for (int featureIndex : sortedFeatureIndices) {
			sb.append(' ').append(featureIndex).append(':');
			appendFormattedFeatureValue(sb, myFeatureIndicesToFeatureValues.get(featureIndex));
		}
		
		return sb.toString();
	}
	
	public String toDenseString(int totalFeatureIndices) {
		double[] featureValues = new double[totalFeatureIndices + 1];
		
		// Populate the dense encoding with feature values.
		for (Map.Entry<Integer, Double> featureIndexToFeatureValue
				: myFeatureIndicesToFeatureValues.entrySet())
		{
			featureValues[featureIndexToFeatureValue.getKey()] = featureIndexToFeatureValue.getValue();
		}
		
		// Build the dense encoding line.
		StringBuilder sb = new StringBuilder();
		sb.append(myOutput);
		for (int i = 1; i < featureValues.length; ++i) {
			sb.append(' ');
			appendFormattedFeatureValue(sb, featureValues[i]);
		}
		
		return sb.toString();
	}
	
	protected static void appendFormattedFeatureValue(StringBuilder sb, double featureValue) {
		// Avoid the decimal point if the feature value is an integer.
		if (Math.IEEEremainder(featureValue, 1) == 0) {
			sb.append((int) featureValue);
		}
		else {
			sb.append(featureValue);
		}
	}
	
	// *******************************************************************************
	
	public static Map.Entry<List<SparseLineAST>, Integer> parseSparseEncodingFromInputLines(
			Iterator<String> lineIterator, UnaryPredicate<String> outputValidator)
	{
		// Returns the AST for the sparse-encoding lines and the max feature index.
		
		final List<SparseLineAST> sparseLineASTs = new ArrayList<SparseLineAST>();
		int maxFeatureIndex = 0;
		
		int lineNumber = 0;
		while (lineIterator.hasNext()) {
			final String line = lineIterator.next();
			
			String[] tokens = line.trim().split("\\s+");
			
			if (tokens.length > 0 && !(tokens.length == 1 && tokens[0].equals(""))) {
				try {
					// Read the output.
					SparseLineAST sparseLineAST = new SparseLineAST(tokens[0], outputValidator);
					
					// Read the feature index and feature value pairs.
					for (int i = 1; i < tokens.length; ++i) {
						sparseLineAST.addFeatureIndexToFeatureValueFromToken(tokens[i]);
					}
					
					// Update the max feature index.
					if (sparseLineAST.getMaxFeatureIndex() > maxFeatureIndex) {
						maxFeatureIndex = sparseLineAST.getMaxFeatureIndex();
					}
					
					sparseLineASTs.add(sparseLineAST);
				}
				catch (RuntimeException x) {
					throw new RuntimeException(String.format("Error while reading line %d: %s",
							lineNumber, x.getMessage()));
				}
			}	
			
			++lineNumber;
		}
		
		return new AbstractMap.SimpleEntry<List<SparseLineAST>, Integer>(sparseLineASTs, maxFeatureIndex);
	}

	// -------------------------------------------------------------------------------

	public static Map.Entry<List<SparseLineAST>, Integer> parseSparseEncodingFromInputFilename(
			String inputFilename, UnaryPredicate<String> outputValidator)
	{
		// Returns the AST for the sparse-encoding file and the max feature index.
		
		FileLineIterator inputIterator;
		try {
			inputIterator = new FileLineIterator(inputFilename);
		}
		catch (FileNotFoundException x) {
			throw new RuntimeException(
					String.format("Cannot find the file \"%s\".", inputFilename));
		}
		
		final Map.Entry<List<SparseLineAST>, Integer> sparseLineASTsAndMaxFeatureIndex
			= parseSparseEncodingFromInputLines(inputIterator, outputValidator);
		
		try {
			inputIterator.close();
		} catch (IOException e) {
			throw new RuntimeException(
					String.format("Error while closing the file \"%s\".", inputFilename));
		}
		
		return sparseLineASTsAndMaxFeatureIndex;
	}
	
	// -------------------------------------------------------------------------------

	public static void saveSparseEncoding(String outputFilename, List<SparseLineAST> spareLineASTs) {
		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream(new File(outputFilename));
		}
		catch (FileNotFoundException x) {
			throw new RuntimeException(String.format("Cannot find the file \"%s\".", outputFilename));
		}
		final OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream);
		final BufferedWriter out = new BufferedWriter(outputWriter);

		int lineNumber = 0;
		for (SparseLineAST sparseLineAST : spareLineASTs) {
			try {
				out.write(sparseLineAST.toString());
				out.newLine();
			}
			catch (IOException e) {
				throw new RuntimeException(
						String.format("An error occurred while writing line %d to file \"%s\".",
								lineNumber, outputFilename));
			}
			
			++lineNumber;
		}
		
		try {
			out.close();
		}
		catch (IOException e) {
			throw new RuntimeException(
					String.format("Error while closing the file \"%s\".", outputFilename));
		}
	}
}
