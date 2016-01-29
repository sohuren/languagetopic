import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;
import nilgiri.math.DoubleReal;
import nilgiri.math.autodiff.DifferentialFunction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;


public class Paragraph implements Serializable {

    Annotation annotation;
    ArrayList<Question> questions = new ArrayList<Question>();
    
    HashMap<IntPair, CorefChain.CorefMention> corefMap =  new HashMap<IntPair,CorefChain.CorefMention>();
    ArrayList<EntityGraph> sentEntityGraphs = new ArrayList<EntityGraph>();
    ArrayList<SemanticGraph> sentDepGraphs = new ArrayList<SemanticGraph>();

    Tree rstTree;
    ArrayList<ArrayList<Tree>> sent2RSTNodePointers = new ArrayList<>();


    //Feature Caching
    
    transient ArrayList<ArrayList<HashMap<Integer, Double>>> q2Z2featuresType1 = new ArrayList<ArrayList<HashMap<Integer, Double>>>();
    transient ArrayList<ArrayList<ArrayList<HashMap<Integer, Double>>>> q2A2Z2featuresType2 = new ArrayList<ArrayList<ArrayList<HashMap<Integer, Double>>>>();
    transient ArrayList<ArrayList<HashMap<Integer, HashMap<Integer, Double>>>> q2Z2Multi2featuresType1 = new ArrayList<ArrayList<HashMap<Integer, HashMap<Integer, Double>>>>();
    transient ArrayList<ArrayList<ArrayList<HashMap<Integer, HashMap<Integer, Double>>>>> q2A2Z2Multi2featuresType2 = new ArrayList<ArrayList<ArrayList<HashMap<Integer, HashMap<Integer, Double>>>>>();
    transient HashMap<IntPair, HashMap<Integer, Double>> multiSentFeatures = new HashMap<>();
    transient HashMap<IntPair, HashMap<Integer, HashMap<Integer, Double>>> sentPair2RelationFeatures;
    transient HashMap<Integer, HashMap<Integer, HashMap<Integer, Double>>> QRFeatures;

    transient HashMap<IntPair, HashMap<Integer, Double>> relDist;


    //for AutoDiff computation
    transient HashMap<Integer, DifferentialFunction<DoubleReal>> logNumerators = null;
    transient HashMap<Integer, DifferentialFunction<DoubleReal>> logDenominators = null;


    void initCaches() {
        q2Z2featuresType1 = new ArrayList<ArrayList<HashMap<Integer, Double>>>();
        q2A2Z2featuresType2 = new ArrayList<ArrayList<ArrayList<HashMap<Integer, Double>>>>();
        q2Z2Multi2featuresType1 = new ArrayList<ArrayList<HashMap<Integer, HashMap<Integer, Double>>>>();
        q2A2Z2Multi2featuresType2 = new ArrayList<ArrayList<ArrayList<HashMap<Integer, HashMap<Integer, Double>>>>>();
        multiSentFeatures = new HashMap<>();
        sentPair2RelationFeatures = new HashMap<>();
        relDist = new HashMap<>();
        QRFeatures = new HashMap<>();
    }

    void cacheFeatures(int quesNum, int sentNum, HashMap<Integer, Double> features) {
        if(SentRel.TEST) return;
        if(quesNum >= q2Z2featuresType1.size())
            q2Z2featuresType1.add(new ArrayList<HashMap<Integer, Double>>());
        if(sentNum >= q2Z2featuresType1.get(quesNum).size())
            q2Z2featuresType1.get(quesNum).add(features);

        //else just ignore
    }
    void cacheFeatures(int quesNum, int ansNum, int sentNum, HashMap<Integer, Double> features) {
        if(SentRel.TEST) return;
        if(quesNum >= q2A2Z2featuresType2.size())
            q2A2Z2featuresType2.add(new ArrayList<ArrayList<HashMap<Integer, Double>>>());
        if(ansNum >= q2A2Z2featuresType2.get(quesNum).size())
            q2A2Z2featuresType2.get(quesNum).add(new ArrayList<HashMap<Integer, Double>>());
        if(sentNum >= q2A2Z2featuresType2.get(quesNum).get(ansNum).size())
            q2A2Z2featuresType2.get(quesNum).get(ansNum).add(features);

        //else just ignore
    }

    boolean checkCacheExists(int quesNum, int sentNum) {
        if(SentRel.TEST) return false;
        if(quesNum < q2Z2featuresType1.size())
            if(sentNum < q2Z2featuresType1.get(quesNum).size())
                return true;
        return false;
    }

    boolean checkCacheExists(int quesNum, int sentNum, int ansNum) {
        if(SentRel.TEST) return false;
        if(quesNum < q2A2Z2featuresType2.size())
            if(ansNum < q2A2Z2featuresType2.get(quesNum).size())
                if(sentNum < q2A2Z2featuresType2.get(quesNum).get(ansNum).size())
                    return true;

        return false;
    }



    void cacheFeaturesMulti(int quesNum, int sentNum1, int sentNum2, HashMap<Integer, Double> features) {

        if(SentRel.TEST) return;
        if(quesNum >= q2Z2Multi2featuresType1.size())
            q2Z2Multi2featuresType1.add(new ArrayList<HashMap<Integer, HashMap<Integer, Double>>>());
        if(sentNum1 >= q2Z2Multi2featuresType1.get(quesNum).size())
            q2Z2Multi2featuresType1.get(quesNum).add(new HashMap<Integer, HashMap<Integer, Double>>());
        if(!q2Z2Multi2featuresType1.get(quesNum).get(sentNum1).containsKey(sentNum2))
            q2Z2Multi2featuresType1.get(quesNum).get(sentNum1).put(sentNum2, features);

        //else just ignore
    }

    void cacheFeaturesMulti(int quesNum, int ansNum, int sentNum1, int sentNum2, HashMap<Integer, Double> features) {
        if(SentRel.TEST) return;
        if(quesNum >= q2A2Z2Multi2featuresType2.size())
            q2A2Z2Multi2featuresType2.add(new ArrayList<ArrayList<HashMap<Integer, HashMap<Integer, Double>>>>());
        if(ansNum >= q2A2Z2Multi2featuresType2.get(quesNum).size())
            q2A2Z2Multi2featuresType2.get(quesNum).add(new ArrayList<HashMap<Integer, HashMap<Integer, Double>>>());
        if(sentNum1 >= q2A2Z2Multi2featuresType2.get(quesNum).get(ansNum).size())
            q2A2Z2Multi2featuresType2.get(quesNum).get(ansNum).add(new HashMap<Integer, HashMap<Integer, Double>>());
        if(!q2A2Z2Multi2featuresType2.get(quesNum).get(ansNum).get(sentNum1).containsKey(sentNum2))
            q2A2Z2Multi2featuresType2.get(quesNum).get(ansNum).get(sentNum1).put(sentNum2, features);

        //else just ignore
    }

    boolean checkCacheExistsMulti(int quesNum, int sentNum1, int sentNum2) {
        if(SentRel.TEST) return false;
        if(quesNum < q2Z2Multi2featuresType1.size())
            if(sentNum1 < q2Z2Multi2featuresType1.get(quesNum).size())
                if(q2Z2Multi2featuresType1.get(quesNum).get(sentNum1).containsKey(sentNum2))
                    return true;
        return false;
    }

    boolean checkCacheExistsMulti(int quesNum, int ansNum, int sentNum1, int sentNum2) {
        if(SentRel.TEST) return false;
        if(quesNum < q2A2Z2Multi2featuresType2.size())
            if(ansNum < q2A2Z2Multi2featuresType2.get(quesNum).size())
                if(sentNum1 < q2A2Z2Multi2featuresType2.get(quesNum).get(ansNum).size())
                    if(q2A2Z2Multi2featuresType2.get(quesNum).get(ansNum).get(sentNum1).containsKey(sentNum2))
                        return true;
        return false;
    }


    boolean checkCacheMultiSent(int sentNum1, int sentNum2) {
        if(SentRel.TEST) return false;
        return multiSentFeatures.containsKey(new IntPair(sentNum1, sentNum2));
    }

    void cacheFeaturesMultiSent(int sentNum1, int sentNum2, HashMap<Integer, Double> features) {
        if(SentRel.TEST) return;
        multiSentFeatures.put(new IntPair(sentNum1, sentNum2), features);
    }


    boolean checkCacheRelation(int sentNum1, int sentNum2, int rel) {
        if(SentRel.TEST) return false;
        IntPair pair = new IntPair(sentNum1, sentNum2);
        return sentPair2RelationFeatures.containsKey(pair) && sentPair2RelationFeatures.get(pair).containsKey(rel);
    }


    boolean checkCacheQR(int quesNum, int rel) {
        if(SentRel.TEST) return false;
        return QRFeatures.containsKey(quesNum) && QRFeatures.get(quesNum).containsKey(rel);
    }

    void cacheFeaturesRelation(int sentNum1, int sentNum2, int rel, HashMap<Integer, Double> features) {
        if(SentRel.TEST) return;
        IntPair pair = new IntPair(sentNum1, sentNum2);
        if(!sentPair2RelationFeatures.containsKey(pair))
            sentPair2RelationFeatures.put(pair, new HashMap<Integer, HashMap<Integer, Double>>());
        sentPair2RelationFeatures.get(pair).put(rel, features);
    }

    void cacheFeaturesQR(int quesNum, int rel, HashMap<Integer, Double> features) {
        if(SentRel.TEST) return;
        if(!QRFeatures.containsKey(quesNum))
            QRFeatures.put(quesNum, new HashMap<Integer, HashMap<Integer, Double>>());
        QRFeatures.get(quesNum).put(rel, features);
    }



static ArrayList<Paragraph> readParagraphs(String inFile) throws IOException {
        
    	BufferedReader br = new BufferedReader(new FileReader(inFile));
        
    	ArrayList<Paragraph> paragraphs = new ArrayList<Paragraph>();
        
        System.err.print("Reading paragraphs... ");
        int index=0;
        
        try {
            String line = br.readLine();
            while (line != null) {
                
            	//process
                String [] values = line.replace("\\newline", " ").split("\t");
                Paragraph para = new Paragraph();


                Annotation tmpAnno = new Annotation(values[2]);
                SentRel.pipeline.annotate(tmpAnno);
                para.annotation = tmpAnno;


                int quesNum = 0;
                for(int i=3;i<values.length; i+=5) {
                    Question q = new Question();
                    if(values[i].contains("one:"))
                        q.type = 1;
                    else
                        q.type = 2;
                    Annotation annotation = new Annotation(values[i].replace("one: ", "").replace("multiple: ", ""));
                    SentRel.pipeline.annotate(annotation);
                    q.question = annotation;
                    
                    
                    // this is for the old mctest data
                    
                    for(int j = 1;j <= values.length;j++) {
                    	
                        Annotation cAnno = new Annotation(values[i+j]);
                        SentRel.pipeline.annotate(cAnno);
                        q.choices.add(cAnno);
                        
                        // entity graph
                        q.choiceGraphs.add(new EntityGraph(para, cAnno.get(CoreAnnotations.SentencesAnnotation.class).get(0), false));
                    }

                    CoreMap qSent = q.question.get(CoreAnnotations.SentencesAnnotation.class).get(0);
                    
                    //entity graph
                    q.entityGraph = new EntityGraph(para, qSent, false);

                    //depGraph
                    q.depGraph = qSent.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
//                    q.depGraph = ParseProcess.splitClauses(qSent);

                    q.number = quesNum++;

                    para.questions.add(q);
                }

                //COREF
                createCorefMap(para);

                //entity graphs
                for(CoreMap sentence : para.annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                    para.sentEntityGraphs.add(new EntityGraph(para, sentence, true));
                    para.sentDepGraphs.add(sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class));
                }

                paragraphs.add(para);
                // read next line
                line = br.readLine();
                index++;
                System.err.print("\r Reading passages from the database ... " + index);
            }
        } finally {
            br.close();
        }
        System.err.println("Read passages from " + inFile);
        return paragraphs;
    }

    

    
    
  // read stories from the file list
static ArrayList<Paragraph> readParagraphs_separate(String inFile) throws IOException {
        
    	BufferedReader br = new BufferedReader(new FileReader(inFile));
        
    	ArrayList<Paragraph> paragraphs = new ArrayList<Paragraph>();
        
        System.err.print("Reading paragraph list \n");
        
        int index = 0;
        
        try{
        		// line is the paragraph's location
            	String line = br.readLine();
            
            	while (line != null) {
            		
            		Paragraph Para = new Paragraph();
            		
            		// read this file            		
            		BufferedReader br_file = new BufferedReader(new FileReader(line));
            
            		try{
            		
            			// the first line is the comment, we can ignore it
            			// String comment = br_file.readLine();
            		
            			// now the second line is the story
            			String story = br_file.readLine();
            			Annotation tmpAnno = new Annotation(story);
            			SentRel.pipeline.annotate(tmpAnno);
            			// Paragraph Para = new Paragraph();
            			Para.annotation = tmpAnno;
            			
            			// get all the questions for this stories
            			int quesNum = 0;
            			String question = br_file.readLine();
            			
            			while(question != null)
            			{
            				// now check the question one by one
            				String [] values = question.split("\t");
            				Question q = new Question();
            				if(values[0].contains("one: "))
            					q.type = 1;
            				else
            					q.type = 2;
            				
            				Annotation annotation = new Annotation(values[0].replace("one: ", "").replace("multiple: ", ""));
            				SentRel.pipeline.annotate(annotation);
            				q.question = annotation;
            				
            				// all the possible answer
            				for(int i = 1; i < values.length; i++) {
            					Annotation cAnno = new Annotation(values[i]);
            					SentRel.pipeline.annotate(cAnno);
            					q.choices.add(cAnno);
            					// entity graph
            					q.choiceGraphs.add(new EntityGraph(Para, cAnno.get(CoreAnnotations.SentencesAnnotation.class).get(0), false));
            				}

            				CoreMap qSent = q.question.get(CoreAnnotations.SentencesAnnotation.class).get(0);
            			
            				// entity graph
            				q.entityGraph = new EntityGraph(Para, qSent, false);

            				// depGraph
            				q.depGraph = qSent.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
            				q.number = quesNum++;
            				Para.questions.add(q);
            				question = br_file.readLine();
            			}

            			// coreference now
            			createCorefMap(Para);
            			// entity graphs
            			for(CoreMap sentence : Para.annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
            				Para.sentEntityGraphs.add(new EntityGraph(Para, sentence, true));
            				Para.sentDepGraphs.add(sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class));
            				Para.sentDepGraphs.add(ParseProcess.splitClauses(sentence));
            			}
                		
            			br_file.close();
            }finally{
            	
            }
            
            paragraphs.add(Para);
            line = br.readLine();
            index++;    
            
            System.out.print("\r Reading passages from the filelist..." + index); 	 	
         
         }
            
        }finally {
            br.close();
        }
        
        System.out.println("Read passages from filelist" + inFile);
        return paragraphs;
    }
    
    
	
// Read in answers in digital form (A,B,C,D)
static ArrayList<String []> readAnswers_separate(String inFile, ArrayList<Paragraph> paragraphs) throws IOException {
    
	BufferedReader br = new BufferedReader(new FileReader(inFile));
    ArrayList<String []> answers = new ArrayList<String[]>();
    
    try {
    	
        String line = br.readLine();
        int i = 0;
        
        while (line != null) {
            
        	// process the answer now
            answers.add(line.split("\t"));	// separate by the tab

            // augment paragraphs with the answers
            for(int j = 0; j < answers.get(i).length; j++) {
                paragraphs.get(i).questions.get(j).correct = Integer.parseInt(answers.get(i)[j]); // assume the digital form  
            }
            
            // read next line
            line = br.readLine();
            i++;
        }
    }finally{
        br.close();
    }
    
    System.out.println("Read answers from " + inFile);
    
    return answers;
}
    
    
    
// Read in answers in character form (A,B,C,D). IMP: Do not convert to lowercase
static ArrayList<String []> readAnswers(String inFile, ArrayList<Paragraph> paragraphs) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(inFile));
        ArrayList<String []> answers = new ArrayList<String[]>();
        try {
            String line = br.readLine();
            int i=0;
            while (line != null) {
                //process
                answers.add(line.split("\t"));

                //augment paragraphs
                for(int j=0; j < answers.get(i).length; j++) {
                    paragraphs.get(i).questions.get(j).correct = answers.get(i)[j].charAt(0) - 65;
                }

                //read next line
                line = br.readLine();
                i++;
            }
        } finally {
            br.close();
        }
        System.err.println("Read answers from " + inFile);
        return answers;
    }


/*
    static void readRTEFile(String inFile, ArrayList<Paragraph> paragraphs) throws IOException, SAXException, ParserConfigurationException {

        //get the factory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        //Using factory get an instance of document builder
        DocumentBuilder db = dbf.newDocumentBuilder();

        //parse using builder to get DOM representation of the XML file
        Document dom = db.parse(inFile);

        //get the root element
        Element docEle = dom.getDocumentElement();

        //get a nodelist of elements
        NodeList pairElements = docEle.getElementsByTagName("pair");
        
        if(pairElements != null && pairElements.getLength() > 0) {
            for(int i = 0 ; i < pairElements.getLength();i++) {

                // get the pair element
                Element pairEle = (Element)pairElements.item(i);

                int paraNum = i/16;
                int qNum = (i%16)/4;
                int choiceNum = i%4;

                String choiceReformatted = pairEle.getElementsByTagName("h").item(0).getFirstChild().getNodeValue();
                Annotation annotation = new Annotation(choiceReformatted);
                SentRel.pipeline.annotate(annotation);

                paragraphs.get(paraNum).questions.get(qNum).choicesReformatted.add(annotation);
                if(paragraphs.get(paraNum).questions.get(qNum).choices.size() > 4)
                    paragraphs.get(paraNum).questions.get(qNum).choices.remove(choiceNum+1);

                //System.err.print("\rReading RTE statements... " + i);

            }
        }

    }
    
*/

    
    static void createCorefMap(Paragraph paragraph) {
        //IMP : modifies the corefMap in paragraph directly
        Map<Integer, CorefChain> corefGraph = paragraph.annotation.get(CorefCoreAnnotations.CorefChainAnnotation.class);

        if(corefGraph != null)
        {
        	for(CorefChain chain : corefGraph.values()) {

            //use original head (has some issues)
//            CorefChain.CorefMention head = chain.getRepresentativeMention();

            //take first mention as head
            CorefChain.CorefMention head = chain.getMentionsInTextualOrder().get(0);

            for(IntPair position : chain.getMentionMap().keySet()) {
                paragraph.corefMap.put(position, head);
            }
        }
        }
    }


    //For serializing and file IO
    static void writeParagraphsSerialized(ArrayList<Paragraph> paragraphs, String filename) {

        System.out.println("Cache the features to files");
        try (
                OutputStream file = new FileOutputStream(filename);
                OutputStream buffer = new BufferedOutputStream(file);
                ObjectOutput output = new ObjectOutputStream(buffer);
        ){
            output.writeObject(paragraphs);
        }
        catch(IOException ex){
            System.err.println("Error: cannot perform output");
        }

    }

    static List<Paragraph> readParagraphsSerialized(String filename) throws ClassNotFoundException, IOException {

        System.out.println("Reading cached file " + filename+".");
        List<Paragraph> paragraphs = null;
        
        try(
                InputStream file = new FileInputStream(filename);
                InputStream buffer = new BufferedInputStream(file);
                ObjectInput input = new ObjectInputStream (buffer);
        ){
            paragraphs = (List<Paragraph>) input.readObject();

        }catch(ClassNotFoundException ex){
            System.err.println("Cannot perform input. Class not found. " + ex);
            throw ex;
        }
        catch(IOException ex){
            System.err.println("Cannot perform input. " + ex);
            throw ex;
        }

        return paragraphs;
    }

    
    
    
    // write the current paragraph
    
    void writeParagraph(String filename, boolean printQ) throws IOException {
        
    	List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        File file = new File(filename);

        // if file doesnt exists, then create it
        if (!file.exists()) {
            file.createNewFile();
        }

        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        for(CoreMap sentence : sentences) {
            bw.write(sentence.toString() + "<s>\n");
        }
        
        bw.write("\n");

        if(printQ) {
        	
            // questions
            for (Question Q : questions) {
                bw.write(Integer.toString(Q.number+1) + ") " + (Q.type == 1 ? "<single> " : "<multiple> ")
                        + Q.question.get(CoreAnnotations.TextAnnotation.class) + "\n");
                for (int k = 0; k < Q.choices.size(); k++) {
                    if (k == Q.correct)
                        bw.write("* " + Q.choices.get(k).get(CoreAnnotations.TextAnnotation.class) + "\n");
                    else
                        bw.write(Q.choices.get(k).get(CoreAnnotations.TextAnnotation.class) + "\n");
                }
                bw.write("\n");
            }
        }

        bw.close();
    }
    
    
    
void writeParagraph_specific(String filename) throws IOException {
        
    	List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        File file = new File(filename);
        
        file.createNewFile();

        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        for(CoreMap sentence : sentences) {
            bw.write(sentence.toString() + " ");
        }
        bw.write("\n");
	
       // questions
       for(Question Q : questions) 
       {
    	   	   // write the paragraph with the spaces
               bw.write((Q.type == 1 ? "one: ":"multiple: ") + Q.question.get(CoreAnnotations.TextAnnotation.class) + "\t");
               
               for(int k = 0; k < Q.choices.size(); k++) 
               {
            	   
            	    if(k != Q.choices.size() - 1)
            	    	bw.write(Q.choices.get(k).get(CoreAnnotations.TextAnnotation.class) + "\t");  // with the spaces tab
            	    else
            	    	bw.write(Q.choices.get(k).get(CoreAnnotations.TextAnnotation.class));  		// without the spaces tab
               }
               bw.write("\n");
       }
       bw.close();
}

}