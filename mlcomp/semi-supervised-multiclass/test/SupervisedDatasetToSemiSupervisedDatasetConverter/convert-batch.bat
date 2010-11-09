java -jar SupervisedDatasetToSemiSupervisedDatasetConverter.jar train semi-train_5_50 0.05 0.5 > err1.txt
java -jar SupervisedDatasetToSemiSupervisedDatasetConverter.jar train semi-train_5_100 0.05 1.0 > err2.txt
java -jar SupervisedDatasetToSemiSupervisedDatasetConverter.jar train semi-train_30_50 0.3 0.5 > err3.txt
java -jar SupervisedDatasetToSemiSupervisedDatasetConverter.jar train semi-train_30_100 0.3 1.0 > err4.txt
