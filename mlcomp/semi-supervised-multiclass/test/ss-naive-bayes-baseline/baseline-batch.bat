java -jar RunForDiscretizingNaiveBayesClassifier.jar construct > err1.txt
java -jar RunForDiscretizingNaiveBayesClassifier.jar setHyperparameter 0.01 > err2.txt
java -jar RunForDiscretizingNaiveBayesClassifier.jar learn semi-train > err3.txt
java -jar RunForDiscretizingNaiveBayesClassifier.jar predict test predictions > err4.txt