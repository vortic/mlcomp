echo - status > status
java -jar RunForSemiSupervisedLearningDomain.jar inspect semi-train train > err1.txt
java -jar RunForSemiSupervisedLearningDomain.jar inspect test test > err2.txt
java -jar RunForSemiSupervisedLearningDomain.jar inspect semi-raw train > err3.txt
java -jar RunForSemiSupervisedLearningDomain.jar split semi-raw split-semi-train split-test > err4.txt
java -jar RunForSemiSupervisedLearningDomain.jar stripLabels test stripped-test > err5.txt
java -jar RunForSemiSupervisedLearningDomain.jar evaluate test predictions > err6.txt