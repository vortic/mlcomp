java -jar RunForDiscretizingNaiveBayesClassifierEMTrainer.jar construct > err1.txt
java -jar RunForDiscretizingNaiveBayesClassifierEMTrainer.jar setHyperparameter 0.01 > err2.txt
java -jar RunForDiscretizingNaiveBayesClassifierEMTrainer.jar learn semi-train > err3.txt
java -jar RunForDiscretizingNaiveBayesClassifierEMTrainer.jar predict test predictions > err4.txt