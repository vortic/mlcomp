package naiveBayesClassifier;

/**
 * 
 * @author Eugene Ma
 */
public class BatchFileGenerator {
	public static void main(String[] args) {
		// Generate a batch file to generate data to compare supervised and semi-supervised learning.
		
		final int totalNumDivisions = 40;
		
		System.out.println("echo --- > out.txt");
		System.out.println();
		
		for (int numDivisions = 0; numDivisions <= totalNumDivisions; ++numDivisions) {
			double fractionUnlabeled = numDivisions / (double) totalNumDivisions;
			
			System.out.println(String.format(
					"java -jar SupervisedDatasetToSemiSupervisedDatasetConverter.jar %f train semi-train",
					fractionUnlabeled));
			
			final int numEMIterations = 6;
			
			System.out.println(String.format("echo %.4f%%%% unlabeled >> out.txt", fractionUnlabeled * 100));

			// Supervised training.
			System.out.println("java -jar RunForDiscretizingNaiveBayesClassifier.jar construct");
			System.out.println("java -jar RunForDiscretizingNaiveBayesClassifier.jar setHyperparameter 0.01");
			System.out.println(
					"java -jar RunForDiscretizingNaiveBayesClassifier.jar semiSupervisedLearn semi-train 0");
			System.out.println(
					"java -jar RunForDiscretizingNaiveBayesClassifier.jar predict test predictions >> out.txt");
			
			// EM training
			System.out.println("java -jar RunForDiscretizingNaiveBayesClassifier.jar construct");
			System.out.println("java -jar RunForDiscretizingNaiveBayesClassifier.jar setHyperparameter 0.01");
			System.out.println(String.format(
					"java -jar RunForDiscretizingNaiveBayesClassifier.jar semiSupervisedLearn semi-train %d",
					numEMIterations));
			System.out.println(
					"java -jar RunForDiscretizingNaiveBayesClassifier.jar predict test predictions >> out.txt");
			
			System.out.println("echo --- >> out.txt");
			System.out.println();
		}
	}
}
