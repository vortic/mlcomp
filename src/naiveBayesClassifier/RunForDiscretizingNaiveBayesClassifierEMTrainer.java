package naiveBayesClassifier;

/**
 * Just "renames" the "learn" helper method to do EM instead of supervised training.
 * 
 * @author Eugene Ma
 */
public class RunForDiscretizingNaiveBayesClassifierEMTrainer extends RunForDiscretizingNaiveBayesClassifier {
	public static void main(String[] args) {
		RunForDiscretizingNaiveBayesClassifier.main(args);
	}
	
	protected static void learn(String[] args) {
		RunForDiscretizingNaiveBayesClassifier.semiSupervisedLearn(args);
	}
}
