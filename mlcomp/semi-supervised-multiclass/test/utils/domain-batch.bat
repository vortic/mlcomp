echo - status > status

java -jar RunForSemiSupervisedLearningDomain.jar inspect semi-train > err1.txt
java -jar RunForSemiSupervisedLearningDomain.jar inspect bad-semi-train1 > err2.txt
java -jar RunForSemiSupervisedLearningDomain.jar inspect bad-semi-train2 > err3.txt
java -jar RunForSemiSupervisedLearningDomain.jar inspect test > err4.txt
java -jar RunForSemiSupervisedLearningDomain.jar inspect semi-raw > err5.txt

java -jar RunForSemiSupervisedLearningDomain.jar split semi-raw split-semi-train split-test > err6.txt

java -jar RunForSemiSupervisedLearningDomain.jar stripLabels test stripped-test > err7.txt

java -jar RunForSemiSupervisedLearningDomain.jar evaluate test predictions > err8.txt