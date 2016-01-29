import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.IntPair;
import org.fun4j.compiler.Parser;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;


public class mctest_main {

	
    static boolean readParagraphsFresh = true;
    static boolean Dist = false;
    static boolean TEST = false;   // turn on to evaluate on test file instead of dev
    static boolean USE_DEV;        // read from params - whether to use dev in training
    
    static boolean MIXED_SINGLE_MULTI = false;
    
    // use it from the other class, not in this class 
    static boolean NOREL = false; 	// control the model 3 or model 2
    static boolean NO_QR_FEATURES = false;
    static boolean SWD_ONLY = false;
    
    // params
    static String paramsFile = "settings2.properties"; 	// Modified if passed in as first argument
    
    static String wordVectorFile;
    static String wordListFile;
    
    static String trainParagraphDataFile;
    static String trainAnswersDataFile;
    static String devParagraphDataFile;
    static String devAnswersDataFile;
    static String testParagraphDataFile;
    static String testAnswersDataFile;    
    static String stopWordsFile;
    

    static String testSentence = "Carly Fiorina dominated a smaller, less-glamorous debate stage Thursday when"
    		+ "she joined two other low-polling Republican presidential candidates to discuss "
    		+ "everything from national security to technology and Hillary Clinton";


    public static void main(String[] args) throws Exception {

        // read in params from properties file first
        Properties prop = new Properties();
        InputStream input = null;
        
        try {
        	
            if(args.length>0)
                paramsFile = args[0];

            input = new FileInputStream(paramsFile);

            // load a properties file
            prop.load(input);

            
            // get the property values
            wordVectorFile = prop.getProperty("wordVectorFile");
            wordListFile = prop.getProperty("wordListFile");
            TEST = prop.getProperty("test").equals("test");
            
            SentRel.MULTI_SENTENCE = prop.getProperty("mode").equals("newest");
            Evaluate.MULTI_SENTENCE_EVAL = SentRel.MULTI_SENTENCE;
            
            SentRel.maxSentRange = Integer.parseInt(prop.getProperty("maxSentRange"));
            SentRel.LAMBDA_OPT = Double.parseDouble(prop.getProperty("lambda_opt"));
            USE_DEV = prop.getProperty("useDev").equals("true");
            
            
            Dist = prop.getProperty("dist").equals("dist");
            stopWordsFile = prop.getProperty("stopWordsFile");
            
            
            
            trainParagraphDataFile = prop.getProperty("trainParagraphDataFile");
            trainAnswersDataFile = prop.getProperty("trainAnswersDataFile");
            devParagraphDataFile = prop.getProperty("devParagraphDataFile");
            devAnswersDataFile = prop.getProperty("devAnswersDataFile");
            testParagraphDataFile = prop.getProperty("testParagraphDataFile");
            testAnswersDataFile = prop.getProperty("testAnswersDataFile");
            
            
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
 
        //initialize
        if(readParagraphsFresh) {
            SentRel.initStanfordPipeline();
            readParagraphsFresh();
        }else{
        	
            try{	
            	
            	// cache file is now fine
            	SentRel.trainParagraphs = (ArrayList<Paragraph>) Paragraph.readParagraphsSerialized("trainParagraphs.ser"); 	// train data is always the train data 
                SentRel.devParagraphs = (ArrayList<Paragraph>) Paragraph.readParagraphsSerialized((TEST ? "test":"dev") + "Paragraphs.ser");  // test and dev is the always the test or dev data 

                
                if(TEST && USE_DEV) {
                    ArrayList<Paragraph> tmpParagraphs = (ArrayList<Paragraph>) Paragraph.readParagraphsSerialized("devParagraphs.ser"); //read the dev data
                    SentRel.trainParagraphs.addAll(tmpParagraphs); //add it to the train
                }
            }catch (Exception e) {
                // read fresh paragraphs
                SentRel.initStanfordPipeline();
                readParagraphsFresh();
            }
        }
        
        
        // write the paragraph to the specific format
        
        /*
        for(int i=0;i<SentRel.trainParagraphs.size(); i++) {
            String filename = "train5002/"+i+".txt";
            SentRel.trainParagraphs.get(i).writeParagraph_specific(filename); // here, we set it as false
        }
        
        for(int i=0;i<SentRel.devParagraphs.size(); i++) {
            String filename = "dev5002/"+i+".txt";
            SentRel.devParagraphs.get(i).writeParagraph_specific(filename); // here, we set it as false
        }
        System.out.println("the writing is finished");
        */
        
        
        SentRel.initialize();  	// initialize features, etc.
        
        if (!Dist) // train the new model now
        {
        	Opt.MStep();
        	
        	// save the model
            PrintWriter writer = new PrintWriter("models.dat", "UTF-8");
            for(int i = 0; i < SentRel.weights.size(); i++)
            {
            	writer.println(Double.toString(SentRel.weights.get(i)));
            }
            writer.close();
            
        }else{			// read the model
        	
        	try (BufferedReader br = new BufferedReader(new FileReader("models.dat"))){
        		String line;
        		int i = 0;
        		while((line = br.readLine()) != null){
        			SentRel.weights.set(i, Double.parseDouble(line));
        			i++;
        		}
        		br.close();
        	}
        }
        
        if(!Dist)
        {
        	System.err.println("Train Accuracy");
            Evaluate.evaluate(SentRel.trainParagraphs);
        }
        
        Evaluate.evaluate(SentRel.devParagraphs);
        
        
        /*
        System.out.println("Features: ");
        for(String weight : SentRel.feature2Weight.keySet())
            System.out.println(weight + " : " + SentRel.feature2Weight.get(weight));
        
        System.out.println("");
        System.out.println();
		*/
        
        System.out.println("Finished");
        
}


static void readParagraphsFresh() throws IOException {
        
    	
		SentRel.trainParagraphs = Paragraph.readParagraphs_separate(trainParagraphDataFile); // here, we still use the original interfaces
        SentRel.trainAnswers = Paragraph.readAnswers_separate(trainAnswersDataFile, SentRel.trainParagraphs);
        
        SentRel.devParagraphs = Paragraph.readParagraphs_separate((TEST? testParagraphDataFile: devParagraphDataFile) );
        SentRel.devAnswers = Paragraph.readAnswers_separate((TEST? testAnswersDataFile: devAnswersDataFile), SentRel.devParagraphs);
        
        
        Paragraph.writeParagraphsSerialized(SentRel.trainParagraphs, "trainParagraphs.ser");
        Paragraph.writeParagraphsSerialized(SentRel.devParagraphs, (TEST? "test":"dev") + "Paragraphs.ser");

        
        if(TEST && USE_DEV) {
            
        	ArrayList<Paragraph> tmpParagraphs = Paragraph.readParagraphs_separate(devParagraphDataFile);
            SentRel.trainAnswers.addAll(Paragraph.readAnswers_separate(devAnswersDataFile, tmpParagraphs));
            SentRel.trainParagraphs.addAll(tmpParagraphs);
           
            Paragraph.writeParagraphsSerialized(tmpParagraphs, "devParagraphs.ser");
        }
    }
}
