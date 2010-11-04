package naiveBayesClassifier;

/**
 * Just "renames" the "learn" helper method to do EM instead of supervised training.
 * 
 * @author Eugene Ma
 */
public class RunForDiscretizingNaiveBayesClassifierEMTrainer extends RunForDiscretizingNaiveBayesClassifier {
	public static void main(String[] args) {
		// TODO: May really want to get it so that the learn() method is overridden
		// to prevent repeating the logic for reading off the arguments for a command here.
		// (This would mean making learn() and similar methods instance methods instead of static methods.)
		
		if (args.length == 0) {
			throw new RuntimeException("The arguments list must be non-empty.");
		}
		else {
			final String command = args[0];
			if (command.equals("learn")) {
				RunForDiscretizingNaiveBayesClassifier.semiSupervisedLearn(args);
			}
			else {
				RunForDiscretizingNaiveBayesClassifier.main(args);
			}
		}
	}
}
