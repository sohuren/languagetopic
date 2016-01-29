import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.IntPair;
import org.fun4j.compiler.Parser;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;


public class mctestmain {

	static boolean DEBUG = true;
    static boolean AnnotatedEval = false;
    static boolean readParagraphsFresh = true;
    static boolean Dist = false;
    
    
    static boolean TEST = false;   // turn on to evaluate on test file instead of dev
    static boolean USE_DEV;        // read from params - whether to use dev in training
    static boolean MIXED_SINGLE_MULTI = true;
    
    
    // use it from the other class, not in this class 
    static boolean NOREL = false; // control the model 3 or model 2
    static boolean NO_QR_FEATURES = false;
    static boolean SWD_ONLY = false;
    static boolean RST_ON = false; // control whether we use the rst information
    
    
    // IMP: only works with serialized data (for now)
    static boolean USE_ALL = false; // use both 160 and 500 for training
    
    
    //params
    static String paramsFile = "params.properties"; 	// Modified if passed in as first argument
    
    static String wordVectorFile;
    static String wordListFile;
    static String fileNum;
    static String trainParagraphDataFile;
    static String trainAnswersDataFile;
    static String devParagraphDataFile;
    static String devAnswersDataFile;
    static String annotatedSentencesFile;
    static String stopWordsFile;
    static String RTETrainFile, RTEDevFile;

    static String testSentence = "He left the house but after half an hour he came back and this time he was dressed up as superhero and he also had a sword.";

    //global variables
    
    static ArrayList<ArrayList<Annotation>> paragraph2SentencesAnnotated;

    
    public static void main(String[] args) throws Exception {

        //read in params from properties file first
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
            fileNum = prop.getProperty("fileNum");
            TEST = prop.getProperty("test").equals("test");
            SentRel.MULTI_SENTENCE = prop.getProperty("mode").equals("multi");
            Evaluate.MULTI_SENTENCE_EVAL = SentRel.MULTI_SENTENCE;
            SentRel.maxSentRange = Integer.parseInt(prop.getProperty("maxSentRange"));
            SentRel.LAMBDA_OPT = Double.parseDouble(prop.getProperty("lambda_opt"));
            USE_DEV = prop.getProperty("useDev").equals("true");
            
            
            Dist = prop.getProperty("dist").equals("dist");
            
            
            trainParagraphDataFile="data/mc" + fileNum + ".train.tsv";
            trainAnswersDataFile="data/mc" + fileNum + ".train.ans";
            devParagraphDataFile="data/mc" + fileNum + "."+(TEST?"test":"dev")+".tsv";
            devAnswersDataFile="data/mc" + fileNum + "."+(TEST?"test":"dev")+".ans";
            
            
            // we can obtain this from searching the internet and get it from RTE 
            
            // RTETrainFile="data/Statements/mc" + fileNum + ".train.statements.pairs";
            // RTEDevFile="data/Statements/mc" + fileNum + ".dev.statements.pairs";
            // annotatedSentencesFile = prop.getProperty("annotatedSentencesFile");
            
            stopWordsFile = prop.getProperty("stopWordsFile");
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

        
        //TODO: move this to readParaFresh finally
        // Paragraph tmp = new Paragraph();
        // RST.readRSTTree("rst/train160/0.txt.tree", tmp);

        
        //initialize
        if(readParagraphsFresh) {
            SentRel.initStanfordPipeline();
            readParagraphsFresh();
        }else{
            try{
                SentRel.trainParagraphs = (ArrayList<Paragraph>) Paragraph.readParagraphsSerialized("trainParagraphs" + fileNum + ".ser");
                SentRel.devParagraphs = (ArrayList<Paragraph>) Paragraph.readParagraphsSerialized((TEST ? "test":"dev")+"Paragraphs" + fileNum + ".ser");

                if(USE_ALL) {
                    ArrayList<Paragraph> tmpParagraphs;
                    tmpParagraphs = (ArrayList<Paragraph>) Paragraph.readParagraphsSerialized("trainParagraphs" + (660 - Integer.parseInt(fileNum)) + ".ser");
                    SentRel.trainParagraphs.addAll(tmpParagraphs);

                    tmpParagraphs = (ArrayList<Paragraph>) Paragraph.readParagraphsSerialized("devParagraphs" + (660 - Integer.parseInt(fileNum)) + ".ser");
                    SentRel.trainParagraphs.addAll(tmpParagraphs);
                }

                if(TEST && USE_DEV) {
                    ArrayList<Paragraph> tmpParagraphs = (ArrayList<Paragraph>) Paragraph.readParagraphsSerialized("devParagraphs" + fileNum + ".ser");
                    SentRel.trainParagraphs.addAll(tmpParagraphs);
                }
            }
            catch (Exception e) {
                //read fresh paragraphs
                SentRel.initStanfordPipeline();
                readParagraphsFresh();
            }
        }
        
        if(SentRel.RTE) {
            Paragraph.readRTEFile(RTETrainFile, SentRel.trainParagraphs);
            Paragraph.readRTEFile(RTEDevFile, SentRel.devParagraphs);
        }

        if(AnnotatedEval)
            paragraph2SentencesAnnotated = Evaluate.readAnnotatedSentences(annotatedSentencesFile);

        
        //IMP: write paragraphs to file
        //hacky: producing data for brat
        /*
        for(int i=0;i<SentRel.devParagraphs.size(); i++) {
            String filename = "test160brat/"+i+".txt";
            SentRel.devParagraphs.get(i).writeParagraph(filename, true);
        }        
        

        for(int i=0;i<SentRel.devParagraphs.size(); i++) {
            String filename = "test160/"+i+".txt";
            SentRel.devParagraphs.get(i).writeParagraph(filename, false); // here, we set it as false
        }
        */
        
        /*
        for(int i=0;i<SentRel.trainParagraphs.size(); i++) {
            String filename = "train500brat/"+i+".txt";
            SentRel.trainParagraphs.get(i).writeParagraph(filename, true);
        }
        
        //hacky: producing data for RST input
           
        for(int i=0;i<SentRel.trainParagraphs.size(); i++) {
            String filename = "train500/"+i+".txt";
            SentRel.trainParagraphs.get(i).writeParagraph(filename, false); // here, we set it as false
        }     
       
        
        System.exit(0);
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
        
        
        
        
//        System.err.println("Train:");
//        Evaluate.evaluate(SentRel.trainParagraphs);
//

        
        //System.err.println("Dev:");
        Evaluate.evaluate(SentRel.devParagraphs);
        
        
//        if(!AnnotatedEval) {
//            ArrayList<Paragraph> combo = new ArrayList<Paragraph>();
//            combo.addAll(SentRel.trainParagraphs);
//            combo.addAll(SentRel.devParagraphs);
//            System.err.println("Combo:");
//            Evaluate.evaluate(combo);
//        }

//        Evaluate.rankSentencesAcrossParagraphs(SentRel.devParagraphs);

        // print out imp. debug info
        
        /*
         
         
        System.out.println("Features: ");
        for(String weight : SentRel.feature2Weight.keySet())
            System.out.println(weight + " : " + SentRel.feature2Weight.get(weight));
        
        System.out.println("");
        System.out.println();

        
        // print the probabilities of various sentence pairs

        int paranum = 0;
        for(Paragraph p : SentRel.trainParagraphs) {
            System.out.println("##### Train Paragraph Sentences : " + (paranum++) + " #####");
            for(int quesNum = 0; quesNum < p.questions.size(); quesNum++) {
                System.out.println("????? Question : "+quesNum+" ?????");
                //get the relation distribution relDistZ
                int N = p.annotation.get(CoreAnnotations.SentencesAnnotation.class).size();
                HashMap<IntPair, Double> sentDist = SentRel.sentDist(p, quesNum);
                for(IntPair key : sentDist.keySet()) {
                    System.out.println(key + " : " + sentDist.get(key));
                }
            }
        }

        paranum = 0;
        for(Paragraph p : SentRel.devParagraphs) {
            System.out.println("##### Test Paragraph Sentences : " + (paranum++) + " #####");
            for(int quesNum = 0; quesNum < p.questions.size(); quesNum++) {
                System.out.println("????? Question : "+quesNum+" ?????");
                //get the relation distribution relDistZ
                int N = p.annotation.get(CoreAnnotations.SentencesAnnotation.class).size();
                HashMap<IntPair, Double> sentDist = SentRel.sentDist(p, quesNum);
                for(IntPair key : sentDist.keySet()) {
                    System.out.println(key + " : " + sentDist.get(key));
                }
            }
        }


        paranum = 0;
        for(Paragraph p : SentRel.trainParagraphs) {
            System.out.println("##### Train Paragraph : " + (paranum++) + " #####");
            for(int quesNum = 0; quesNum < p.questions.size(); quesNum++) {
                System.out.println("????? Question : "+quesNum+" ?????");
                //get the relation distribution relDistZ
                int N = p.annotation.get(CoreAnnotations.SentencesAnnotation.class).size();
                for(int z1=0;z1 < N; z1++)
                    for (int z2=z1; z2 < N; z2++) {
                        System.out.println(Integer.toString(z1)+" "+z2 + " : " + Relation.relDistQZ(p, quesNum, new int[]{z1,z2}));
                    }
//                for (IntPair pair : p.relDist.keySet())
//                    System.out.println(pair + " : " + p.relDist.get(pair));
            }
        }

        paranum = 0;
        for(Paragraph p : SentRel.devParagraphs) {
            System.out.println("##### Test Paragraph : " + (paranum++) + " #####");
            for(int quesNum = 0; quesNum < p.questions.size(); quesNum++) {
                System.out.println("????? Question : "+quesNum+" ?????");
                //get the relation distribution relDistZ
                int N = p.annotation.get(CoreAnnotations.SentencesAnnotation.class).size();
                for(int z1=0;z1 < N; z1++)
                    for (int z2=z1; z2 < N; z2++) {
                        System.out.println(Integer.toString(z1)+" "+z2 + " : " + Relation.relDistQZ(p, quesNum, new int[]{z1,z2}));
                    }
//                for (IntPair pair : p.relDist.keySet())
//                    System.out.println(pair + " : " + p.relDist.get(pair));
            }
        }

         */
        
        System.out.println("Done.");
    }


    
    static void readParagraphsFresh() throws IOException {
        
    	SentRel.trainParagraphs = Paragraph.readParagraphs(trainParagraphDataFile);
        SentRel.trainAnswers = Paragraph.readAnswers(trainAnswersDataFile, SentRel.trainParagraphs);
        SentRel.devParagraphs = Paragraph.readParagraphs(devParagraphDataFile);
        SentRel.devAnswers = Paragraph.readAnswers(devAnswersDataFile, SentRel.devParagraphs);

        
        //read in RST
        /*
        String prefix = "rst/";
        int i = 0;
        for(Paragraph paragraph : SentRel.trainParagraphs) {
            RST.readRSTTree(prefix+"train"+fileNum+"/"+(i++)+".txt.tree", paragraph);
            System.out.println("read rst now " + Double.toString(i));
        }
        
        i =0;
        for(Paragraph paragraph : SentRel.devParagraphs) {
            RST.readRSTTree(prefix+(TEST? "test":"dev")+fileNum+"/"+(i++)+".txt.tree", paragraph);
        }
         */
        
        Paragraph.writeParagraphsSerialized(SentRel.trainParagraphs, "trainParagraphs"+fileNum+".ser");
        Paragraph.writeParagraphsSerialized(SentRel.devParagraphs, (TEST? "test":"dev")+"Paragraphs" + fileNum +".ser");

        if(TEST && USE_DEV) {
            
        	ArrayList<Paragraph> tmpParagraphs = Paragraph.readParagraphs(devParagraphDataFile.replace("test", "dev"));
            SentRel.trainAnswers.addAll(Paragraph.readAnswers(devAnswersDataFile.replace("test","dev"), tmpParagraphs));
            SentRel.trainParagraphs.addAll(tmpParagraphs); // imp : trainparas include devparas now
            
            /*
            i = 0;
            for(Paragraph paragraph : tmpParagraphs) {
                RST.readRSTTree(prefix+"dev"+fileNum+"/"+(i++)+".txt.tree", paragraph);
            }
			*/
            
            Paragraph.writeParagraphsSerialized(tmpParagraphs, "devParagraphs"+fileNum+".ser");
        }
    }
}
