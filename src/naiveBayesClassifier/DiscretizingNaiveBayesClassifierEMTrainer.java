package naiveBayesClassifier;

import java.util.ArrayList;
import java.util.List;

import datasetFormat.SparseLineAST;

import edu.berkeley.nlp.util.Counter;

/**
 * EM trainer for the (discretizing) Naive Bayes classifier.
 * 
 * @author Eugene Ma
 */
public class DiscretizingNaiveBayesClassifierEMTrainer {
	public static final double gMaxConvergenceDifference = 0.0000001;
	
	public static DiscretizingNaiveBayesClassifier train(double kForSmoothing,
			List<SparseLineAST> instances, int maxFeatureIndex,
			DiscretizingNaiveBayesClassifier.Discretizer featureValueDiscretizer,
			final int maxNumIterations)
	{
		// Initialize the model from the labeled instances.
		DiscretizingNaiveBayesClassifier classifier = new DiscretizingNaiveBayesClassifier();
		classifier.initialize(kForSmoothing);
		final List<SparseLineAST> labeledInstances = getLabeledInstances(instances);
		System.out.println(String.format("There are %d labeled instances.", labeledInstances.size()));
		classifier.train(labeledInstances, maxFeatureIndex, featureValueDiscretizer);
		
		double logLikelihood = classifier.getLogLikelihood(instances);
		System.out.println(String.format("Log likelihood for iteration 0: %f", logLikelihood));
		
		for (int iteration = 1; iteration <= maxNumIterations; ++iteration) {
			// E-step: Get the soft label distributions.
			List<Counter<Integer>> labelDistributions = getLabelDistributions(classifier, instances);
			
			// M-step: Maximize likelihood.
			classifier = new DiscretizingNaiveBayesClassifier();
			classifier.initialize(kForSmoothing);
			classifier.softTrain(labelDistributions, instances, maxFeatureIndex, featureValueDiscretizer);
			
			final double previousLogLikelihood = logLikelihood;
			logLikelihood = classifier.getLogLikelihood(instances);
			System.out.println(String.format("Log likelihood for iteration %d: %f", iteration, logLikelihood));
			
			// Early exit if the log likelihood has "converged."
			if (Math.abs(logLikelihood - previousLogLikelihood) <= gMaxConvergenceDifference) {
				System.out.println("Log likelihood of the training data has converged.");
				break;
			}
		}
		
		return classifier;
	}
	
	public static List<SparseLineAST> getLabeledInstances(List<SparseLineAST> instances) {
		// Filters out the unlabeled instances (those labeled with "u").
		
		List<SparseLineAST> labeledInstances = new ArrayList<SparseLineAST>();
		
		for (SparseLineAST instance : instances) {
			if (!instance.getOutput().equals("u")) {
				labeledInstances.add(instance);
			}
		}
		
		return labeledInstances;
	}
	
	public static List<Counter<Integer>> getLabelDistributions(
			DiscretizingNaiveBayesClassifier classifier, List<SparseLineAST> instances)
	{
		List<Counter<Integer>> labelDistributions = new ArrayList<Counter<Integer>>();
		
		for (SparseLineAST instance : instances) {
			Counter<Integer> labelDistribution = new Counter<Integer>();
			
			if (instance.getOutput().equals("u")) {
				// For unlabeled instances, the probability mass is distributed by confidence.
				
				// Convert the label scores into probabilities.
				final Counter<Integer> softPredictions = classifier.getSoftPredictions(instance);
				for (int classIndex : softPredictions.keySet()) {
					labelDistribution.incrementCount(
							classIndex, Math.exp(softPredictions.getCount(classIndex)));
				}
				labelDistribution.normalize();
			}
			else {
				// For labeled instances, all of the probability mass goes to the gold class index.
				final int goldClassIndex = Integer.parseInt(instance.getOutput());
				for (int classIndex : classifier.getClassIndices()) {
					if (classIndex == goldClassIndex) {
						labelDistribution.incrementCount(classIndex, 1);
					}
					else {
						labelDistribution.incrementCount(classIndex, 0);
					}
				}
			}
			
			labelDistributions.add(labelDistribution);
		}
		
		return labelDistributions;
	}
}
