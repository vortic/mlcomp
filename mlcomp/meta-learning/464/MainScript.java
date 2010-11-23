import weka.core.Instances;
import weka.core.Attribute;
import weka.classifiers.Classifier;
import weka.classifiers.lazy.KStar;
import weka.core.converters.SVMLightLoader;
import weka.core.converters.ArffLoader;
import java.io.File;
import java.io.*;
 
public class MainScript {

	public static void main(String[] args) throws Exception {
    	if (args.length < 2) {
	      System.out.println("\nUsage Training: java MainScript train <inputDataFile> \n");
	      System.out.println("\nUsage Testing: java MainScript test <inputDataFile> <outputDataFile> \n");
	      System.exit(1);
	    }

		if (args[0].equals("learn")) {

			try {
				Instances data = loadData(args[1]);
				data.setClassIndex(data.numAttributes() - 1);
				
				Classifier clsfr = new KStar();
				
				clsfr.buildClassifier(data);

				// serialize model
				try {	
					weka.core.SerializationHelper.write("model_file", clsfr);
					weka.core.SerializationHelper.write("class_att_file",data.classAttribute());
				} catch (Exception ex) {
					System.out.println("\nCan't create model output stream, file name ok?\n");
					ex.printStackTrace();
			      	System.exit(1);
				}

			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}

		} else if (args[0].equals("predict")) {
			Classifier cls;
			try { // deserialize model
				cls = (Classifier) weka.core.SerializationHelper.read("model_file");
				Attribute class_att = (Attribute) weka.core.SerializationHelper.read("class_att_file");
				Instances data = loadData(args[1]);
				data.setClassIndex(data.numAttributes() - 1);
				try {
					// Classify each instance and write prediction to disk
			        PrintWriter out = new PrintWriter(new FileWriter(args[2]));
			        for (int i = 0; i < data.numInstances(); i++) {
						double clsLabel = cls.classifyInstance(data.instance(i));
						
						out.println(class_att.value((int) clsLabel).replace("class",""));
						
					}
			        out.close();
			    } catch (Exception e) {
						System.out.println("\nProblem writing predictions, exiting...\n");
						e.printStackTrace();
						System.exit(1);
			    }
			} catch (Exception ex){
				System.out.println("\nUnable to  load the classifier to the model\n");
				ex.printStackTrace();
				System.exit(1);
			}
		} else {
		  System.out.println("\nFirst argument must be 'learn' or 'predict'\n");
	      System.exit(1);
		}
	
	}

	public static Instances loadData(String file) throws IOException {
		
		ArffLoader loader = new ArffLoader();
		
	    loader.setSource(new File(file));
	    Instances data = loader.getDataSet();
		return data;
	}

}