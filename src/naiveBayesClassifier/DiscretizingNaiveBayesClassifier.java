package naiveBayesClassifier;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import datasetFormat.SparseLineAST;

import edu.berkeley.nlp.util.Counter;

/**
 * Naive Bayes classifier.
 * The discretizer converts real-valued feature values into integers from 0 to k-1,
 * where k is the number of discrete values.
 * 
 * @author Eugene Ma
 */
public class DiscretizingNaiveBayesClassifier {
	/**
	 * Discretizes real-valued feature values to be a number from 0 to k-1,
	 * where k is the number of discrete values.
	 */
	public static interface Discretizer {
		public int discretize(double featureValue);
		public double inverseDiscretize(int discretizedFeatureValue);
		public int size();
	}
	
	// -------------------------------------------------------------------------------

	public static class SimpleBinaryDiscretizer implements Discretizer {
		@Override
		public int discretize(double featureValue) {
			return (featureValue > 0)? 1:0;
		}

		@Override
		public double inverseDiscretize(int discretizedFeatureValue) {
			return discretizedFeatureValue;
		}

		@Override
		public int size() {
			return 2;
		}
	}
	
	// *******************************************************************************

	/**
	 * Serializable form of the classifier for saving and loading the trained model.
	 */
	public static class FileData implements Serializable {
		private static final long serialVersionUID = 2883428680230567085L;
		
		public Counter<Integer> classIndicesToCounts;
		public HashMap<Integer, ArrayList<Counter<Integer>>>
			classIndicesToFeatureIndicesToDiscreteFeatureValuesToCounts;
		public int maxFeatureIndex;
		public String featureValueDiscretizerString;
		public double kForSmoothing;
	}
	
	// *******************************************************************************

	protected Counter<Integer> myClassIndicesToCounts;
	protected HashMap<Integer, ArrayList<Counter<Integer>>>
		myClassIndicesToFeatureIndicesToDiscreteFeatureValuesToCounts;
	protected int myMaxFeatureIndex;
	protected Discretizer myFeatureValueDiscretizer;
	protected double myKForSmoothing;
	
	// *******************************************************************************
	
	public void initialize(double kForSmoothing) {
		myClassIndicesToCounts = new Counter<Integer>();
		myClassIndicesToFeatureIndicesToDiscreteFeatureValuesToCounts
			= new HashMap<Integer, ArrayList<Counter<Integer>>>();
		myMaxFeatureIndex = 0;
		myFeatureValueDiscretizer = null;
		myKForSmoothing = kForSmoothing;
	}
	
	public void initializeRandomly(Set<Integer> classIndices, int maxFeatureIndex,
			Discretizer featureValueDiscretizer)
	{
		// Initialize the model with random parameters.
		
		myClassIndicesToCounts = new Counter<Integer>();
		myClassIndicesToFeatureIndicesToDiscreteFeatureValuesToCounts
			= new HashMap<Integer, ArrayList<Counter<Integer>>>();
		for (int classIndex : classIndices) {
			// TODO: Decide how to initialize the distribution of class indices.
			// Give the class (index) count a random count.
			//myClassIndicesToCounts.incrementCount(classIndex, 1 - Math.random());
			// Give all class indices the same count for a uniform distribution.
			myClassIndicesToCounts.incrementCount(classIndex, 1);
			
			// For each feature (index), give the discrete feature value counts a random count.
			final ArrayList<Counter<Integer>> featureIndicesToDiscreteFeatureValuesToCounts
				= newClassIndexConditionalDistribution(maxFeatureIndex);
			for (int featureIndex = 1; featureIndex <= maxFeatureIndex; ++featureIndex) {
				final Counter<Integer> discreteFeatureValuesToCounts
					= featureIndicesToDiscreteFeatureValuesToCounts.get(featureIndex);
				for (int discreteFeatureValue = 0;
					discreteFeatureValue < featureValueDiscretizer.size();
					++discreteFeatureValue)
				{
					discreteFeatureValuesToCounts.incrementCount(discreteFeatureValue, 1 - Math.random());
				}
			}
			myClassIndicesToFeatureIndicesToDiscreteFeatureValuesToCounts.put(
					classIndex, featureIndicesToDiscreteFeatureValuesToCounts);
		}
		
		myMaxFeatureIndex = maxFeatureIndex;
		
		myFeatureValueDiscretizer = featureValueDiscretizer;
		
		myKForSmoothing = 0;
	}
	
	public void initializeFromFileData(FileData fileData) {
		myClassIndicesToCounts = fileData.classIndicesToCounts;
		myClassIndicesToFeatureIndicesToDiscreteFeatureValuesToCounts
			= fileData.classIndicesToFeatureIndicesToDiscreteFeatureValuesToCounts;
		myMaxFeatureIndex = fileData.maxFeatureIndex;
		
		/*
		try {
			myFeatureValueDiscretizer
				= (Discretizer) Class.forName(fileData.featureValueDiscretizerString).newInstance();
		}
		catch (InstantiationException e) {
			e.printStackTrace();
			throw new RuntimeException("Unable to instantiate the feature value discretizer.");
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new RuntimeException("Unable to instantiate the feature value discretizer.");
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("Unable to instantiate the feature value discretizer.");
		}
		//*/
		// FIXME: Allow any discretizer to be loaded.
		myFeatureValueDiscretizer = new SimpleBinaryDiscretizer();
		
		myKForSmoothing = fileData.kForSmoothing;
		
		// TODO: Comment out debug code.
		System.out.println("Hyperparameter value: " + myKForSmoothing);
	}
	
	public FileData toFileData() {
		FileData fileData = new FileData();
		
		fileData.classIndicesToCounts = myClassIndicesToCounts;
		fileData.classIndicesToFeatureIndicesToDiscreteFeatureValuesToCounts
			= myClassIndicesToFeatureIndicesToDiscreteFeatureValuesToCounts;
		fileData.maxFeatureIndex = myMaxFeatureIndex;
		
		// FIXME: Allow any discretizer to be saved.
		fileData.featureValueDiscretizerString = myFeatureValueDiscretizer.getClass().toString();
		
		fileData.kForSmoothing = myKForSmoothing;
		
		return fileData;
	}
	
	public void setKForSmoothing(double kForSmoothing) {
		myKForSmoothing = kForSmoothing;
	}
	
	public double getKForSmoothing() {
		return myKForSmoothing;
	}

	public Set<Integer> getClassIndices() {
		return Collections.unmodifiableSet(myClassIndicesToCounts.keySet());
	}
	
	// *******************************************************************************
	
	public SparseLineAST gernerateLabeledInstance() {
		// Sample the class (index).
		final int classIndexSample
			= getIndexSample(myClassIndicesToCounts, myClassIndicesToCounts.size(),
					// TODO: Decide between smoothing or not.
					//false);
					true);
		
		final SparseLineAST labeledInstance = new SparseLineAST(String.format("%d", classIndexSample),
				new SparseLineAST.NumericValidator());
		
		// Sample the feature value for each feature index.
		final List<Counter<Integer>> featureIndicesToDiscretizedFeatureValuesToCounts
			= myClassIndicesToFeatureIndicesToDiscreteFeatureValuesToCounts.get(classIndexSample);
		for (int featureIndex = 1; featureIndex <= myMaxFeatureIndex; ++featureIndex) {
			// Sample the feature value for the feature index.
			final Counter<Integer> discretizedFeatureValuesToCounts
				= featureIndicesToDiscretizedFeatureValuesToCounts.get(featureIndex);
			final int discretizedFeatureValueSample = getIndexSample(
					discretizedFeatureValuesToCounts, myFeatureValueDiscretizer.size(), true);
			final double featureValueSample
				= myFeatureValueDiscretizer.inverseDiscretize(discretizedFeatureValueSample);
			
			labeledInstance.addFeatureIndexToFeatureValue(featureIndex, featureValueSample);
		}
		
		return labeledInstance;
	}
	
	public List<SparseLineAST> generateLabeledInstances(int numLabeledInstances) {
		List<SparseLineAST> labeledInstances = new ArrayList<SparseLineAST>(numLabeledInstances);
		
		for (int i = 0; i < numLabeledInstances; ++i) {
			labeledInstances.add(gernerateLabeledInstance());
		}
		
		return labeledInstances;
	}
	
	protected int getIndexSample(Counter<Integer> indicesToCounts, int numIndices, boolean smoothFlag) {
		// Sample an index from the given distribution of indices.
		
		final double randomValue = Math.random();
		double value = 0;
		for (int index : indicesToCounts.keySet()) {
			double indexLikelihood;
			if (smoothFlag) {
				indexLikelihood = (indicesToCounts.getCount(index) + myKForSmoothing)
					/ (indicesToCounts.totalCount() + numIndices * myKForSmoothing);
			}
			else {
				indexLikelihood = indicesToCounts.getCount(index) / indicesToCounts.totalCount();
			}
			value += indexLikelihood;
			
			if (value >= randomValue) {
				return index;
			}
		}
		
		return indicesToCounts.keySet().iterator().next();
	}
	
	// *******************************************************************************

	public void train(List<SparseLineAST> labeledInstances, int maxFeatureIndex,
			Discretizer featureValueDiscretizer)
	{
		myMaxFeatureIndex = maxFeatureIndex;
		myFeatureValueDiscretizer = featureValueDiscretizer;
		
		int numLabeledInstances = 0;
		for (SparseLineAST labeledInstance : labeledInstances) {
			// Skip over unlabeled instances.
			if (labeledInstance.getOutput().equals("u")) {
				continue;
			}
			numLabeledInstances++;
			
			final int classIndex = Integer.parseInt(labeledInstance.getOutput());
			
			myClassIndicesToCounts.incrementCount(classIndex, 1);
			
			ArrayList<Counter<Integer>> featureIndicesToDiscretizedFeatureValuesToCounts
				= myClassIndicesToFeatureIndicesToDiscreteFeatureValuesToCounts.get(classIndex);
			
			// First time seeing the class index? Then initialize a data structure for it.
			if (featureIndicesToDiscretizedFeatureValuesToCounts == null) {
				// Have a counter of feature values for each feature index.
				featureIndicesToDiscretizedFeatureValuesToCounts
					= newClassIndexConditionalDistribution(maxFeatureIndex);
				
				myClassIndicesToFeatureIndicesToDiscreteFeatureValuesToCounts.put(
						classIndex, featureIndicesToDiscretizedFeatureValuesToCounts);
			}
			
			// Increment the count of the feature value for each feature index.
			for (int featureIndex = 1; featureIndex <= maxFeatureIndex; ++featureIndex) {
				// Discretize the feature value.
				final double featureValue = labeledInstance.getFeatureValueFromFeatureIndex(featureIndex);
				final int discretizedFeatureValue = featureValueDiscretizer.discretize(featureValue);
				
				featureIndicesToDiscretizedFeatureValuesToCounts.get(featureIndex).incrementCount(
						discretizedFeatureValue, 1);
			}
		}
		
		System.out.println(String.format("Trained on %d labeled instances.", numLabeledInstances));
	}
	
	// -------------------------------------------------------------------------------

	public void softTrain(List<Counter<Integer>> labelDistributions,
			List<SparseLineAST> instances, int maxFeatureIndex,
			Discretizer featureValueDiscretizer)
	{
		myMaxFeatureIndex = maxFeatureIndex;
		myFeatureValueDiscretizer = featureValueDiscretizer;
		
		// Initialize the class-(index)-conditional distributions.
		final Set<Integer> classIndices = labelDistributions.get(0).keySet();
		for (int classIndex : classIndices) {
			myClassIndicesToFeatureIndicesToDiscreteFeatureValuesToCounts.put(
					classIndex, newClassIndexConditionalDistribution(maxFeatureIndex));
		}
		
		for (int i = 0; i < labelDistributions.size(); ++i) {
			final Counter<Integer> labelDistribution = labelDistributions.get(i);
			final SparseLineAST labeledInstance = instances.get(i);
			
			// Increment the counts by the fractional counts of the class indices.
			for (int classIndex : labelDistribution.keySet()) {
				final double classIndexFractionalCount = labelDistribution.getCount(classIndex);
				
				myClassIndicesToCounts.incrementCount(classIndex, classIndexFractionalCount);
				
				List<Counter<Integer>> featureIndicesToDiscretizedFeatureValuesToCounts
					= myClassIndicesToFeatureIndicesToDiscreteFeatureValuesToCounts.get(classIndex);
				
				// Increment the (fractional) count of the feature value for each feature index.
				for (int featureIndex = 1; featureIndex <= maxFeatureIndex; ++featureIndex) {
					// Discretize the feature value.
					final double featureValue = labeledInstance.getFeatureValueFromFeatureIndex(featureIndex);
					final int discretizedFeatureValue = featureValueDiscretizer.discretize(featureValue);
					
					featureIndicesToDiscretizedFeatureValuesToCounts.get(featureIndex).incrementCount(
							discretizedFeatureValue, classIndexFractionalCount);
				}
			}
		}
	}
	
	// -------------------------------------------------------------------------------

	protected static ArrayList<Counter<Integer>> newClassIndexConditionalDistribution(int maxFeatureIndex) {
		// Have a counter of feature values for each feature index.
		
		ArrayList<Counter<Integer>> featureIndicesToDiscretizedFeatureValuesToCounts
			= new ArrayList<Counter<Integer>>(maxFeatureIndex + 1);
		
		// Dummy since first feature index is 1.
		featureIndicesToDiscretizedFeatureValuesToCounts.add(null);
		
		for (int featureIndex = 1; featureIndex <= maxFeatureIndex; ++featureIndex) {
			featureIndicesToDiscretizedFeatureValuesToCounts.add(new Counter<Integer>());
		}
		
		return featureIndicesToDiscretizedFeatureValuesToCounts;
	}
	
	// *******************************************************************************

	public int getPrediction(SparseLineAST instance) {
		final Counter<Integer> softPredictions = getSoftPredictions(instance);
		
		// TODO: Comment out debug code.
		//System.out.println(softPredictions);
		
		// Argmax over the class indices to find the class index with the greatest score.
		double maxScore = Double.NEGATIVE_INFINITY;
		int maxClassIndex = 0;
		for (int classIndex : softPredictions.keySet()) {
			final double score = softPredictions.getCount(classIndex);
			
			// Update the class index with the max score.
			if (score > maxScore) {
				maxScore = score;
				maxClassIndex = classIndex;
			}
		}
		
		return maxClassIndex;
	}
	
	// -------------------------------------------------------------------------------

	public Counter<Integer> getSoftPredictions(SparseLineAST instance) {
		// Returns the log scores of class indices (predictions) for the given instance.
		// The log score is log P(y, x_(1:k)) where k is the number of features.
		
		Counter<Integer> softPredictions = new Counter<Integer>();
		
		final Set<Integer> classIndices
			= myClassIndicesToFeatureIndicesToDiscreteFeatureValuesToCounts.keySet();
		
		// Get the log scores of class indices (predictions) for the given instance.
		for (int classIndex : classIndices) {
			// Calculate the score (log likelihood) of the class index.
			
			///*
			// TODO: Choose between smoothing or not at all for label distribution.
			final double classIndexLikelihood = (myClassIndicesToCounts.getCount(classIndex) + myKForSmoothing)
				/ (myClassIndicesToCounts.totalCount() + classIndices.size() * myKForSmoothing);
			//*/
			/*
			final double classIndexLikelihood = myClassIndicesToCounts.getCount(classIndex)
				/ myClassIndicesToCounts.totalCount();
			//*/
			
			double score = Math.log(classIndexLikelihood);
			
			final List<Counter<Integer>> featureIndicesToDiscretizedFeatureValuesToCounts
				= myClassIndicesToFeatureIndicesToDiscreteFeatureValuesToCounts.get(classIndex);
			for (int featureIndex = 1; featureIndex <= myMaxFeatureIndex; ++featureIndex) {
				// Discretize the feature value.
				final double featureValue = instance.getFeatureValueFromFeatureIndex(featureIndex);
				final int dicretizedFeatureValue = myFeatureValueDiscretizer.discretize(featureValue);
				
				// Get the smoothed likelihood of the discretized feature value.
				final Counter<Integer> discretizedFeatureValuesToCounts
					= featureIndicesToDiscretizedFeatureValuesToCounts.get(featureIndex);
				
				final double discretizedFeatureValueCount
					= discretizedFeatureValuesToCounts.getCount(dicretizedFeatureValue);
				final double featureValueLikelihood = (discretizedFeatureValueCount + myKForSmoothing)
					/ (discretizedFeatureValuesToCounts.totalCount()
							+ myFeatureValueDiscretizer.size() * myKForSmoothing);

				score += Math.log(featureValueLikelihood);
			}
			
			softPredictions.incrementCount(classIndex, score);
		}
		
		return softPredictions;
	}
	
	// *******************************************************************************

	public double getLogLikelihood(List<SparseLineAST> instances) {
		// Returns the log likelihood of the given data under the classifier's model.
		
		double totalLogLikelihood = 0;
		
		for (SparseLineAST instance : instances) {
			final Counter<Integer> softPredictions = getSoftPredictions(instance);
			if (instance.getOutput().equals("u")) {
				// Convert the label scores into probabilities.
				Counter<Integer> unnormalizedLabelDistribution = new Counter<Integer>();
				for (int classIndex : softPredictions.keySet()) {
					unnormalizedLabelDistribution.incrementCount(
							classIndex, Math.exp(softPredictions.getCount(classIndex)));
				}
				
				// For unlabeled instances, add the log of the sum (over y) of the likelihoods of (x, y).
				totalLogLikelihood += Math.log(unnormalizedLabelDistribution.totalCount());
			}
			else {
				// For labeled instances, add the log likelihood of (x, y).
				final int goldClassIndex = Integer.parseInt(instance.getOutput());
				totalLogLikelihood += softPredictions.getCount(goldClassIndex);
			}
		}
		
		return totalLogLikelihood;
	}
	
	// *******************************************************************************

	public static Set<Integer> makeClassIndexSet(int numClassIndices) {
		Set<Integer> classIndices = new HashSet<Integer>();
		for (int classIndex = 1; classIndex <= numClassIndices; ++classIndex) {
			classIndices.add(classIndex);
		}
		return classIndices;
	}
}
