import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;

import java.util.*;

public class Baseline {

	
    static Map<String, Integer> getCountMap(Paragraph paragraph) {
        
    	HashMap<String, Integer> countMap = new HashMap<String, Integer>();
        List<CoreMap> sentences = paragraph.annotation.get(CoreAnnotations.SentencesAnnotation.class);

        for(CoreMap sentence : sentences) {
            List<CoreLabel> words = sentence.get(CoreAnnotations.TokensAnnotation.class);
            for(CoreLabel wordLabel : words) {
                String word = Tools.getText(wordLabel);
                if (countMap.containsKey(word))
                    countMap.put(word, countMap.get(word) + 1);
                else
                    countMap.put(word, 1);
            }
        }    
        return countMap;
    }
    
    
    // get the frequency for each answer in case of anonimized version
    
    static double Max_frequency(Paragraph paragraph, int quesNum, int ansNum) {
        
    	Set<String> aWords = Tools.getWordSet(paragraph.questions.get(quesNum).choices.get(ansNum));
        
        String answers = ""; 
        Iterator iterator = aWords.iterator();
        
        while(iterator.hasNext())
        {
        	  String element = (String)iterator.next();
        	  answers = answers + " " + element;
        }
        
        List<CoreMap> sentences = paragraph.annotation.get(CoreAnnotations.SentencesAnnotation.class);
        
        double count = 0;
        
        for(CoreMap sentence : sentences) {
            
        	List<CoreLabel> words = sentence.get(CoreAnnotations.TokensAnnotation.class);
            
            // each word in the sentence
        	String sen = "";
        	
        	for(CoreLabel wordLabel : words) {
            	
                String word = Tools.getText(wordLabel);
                sen = sen + " " + word;	
            }
        	
        	count += Tools.findString(answers, sen);	
        }
        
        return count;
        
    }

    	
    static double Max_frequency_exclusive(Paragraph paragraph, int quesNum, int ansNum) {
            
    	Set<String> aWords = Tools.getWordSet(paragraph.questions.get(quesNum).choices.get(ansNum));
    	Set<String> qWords = Tools.getWordSet(paragraph.questions.get(quesNum).question);
    	
        String answers = "";
        String question = ""; 
        
        Iterator iterator = aWords.iterator();
        while(iterator.hasNext())
        {
        	  String element = (String) iterator.next();
        	  answers = answers + " " + element;
        }
        
        iterator = qWords.iterator();
        while(iterator.hasNext())
        {
        	  String element = (String) iterator.next();
        	  question = question + " " + element;
        }
        
        List<CoreMap> sentences = paragraph.annotation.get(CoreAnnotations.SentencesAnnotation.class);
        
        int count = 0;
        
        for(CoreMap sentence : sentences) {
            
        	List<CoreLabel> words = sentence.get(CoreAnnotations.TokensAnnotation.class);
            
            // each word in the sentence
        	String sen = "";
        	
        	for(CoreLabel wordLabel : words) {
            	
                String word = Tools.getText(wordLabel);
                sen = sen + " " + word;	
            }
        	
        	count += Tools.findString(answers, sen);	
        }
        
        if(Tools.findString(answers, question) == 0)	return count;
        else											return 0;
        
    }	
    
    
    static double Word_embedding(Paragraph paragraph, int quesNum, int ansNum) 
    {
    	
    	// HashMap<String, ArrayList<Double>> SentRel.wordVec;
    
    	Set<String> pWords = Tools.getWordSet(paragraph);
        Set<String> qWords = Tools.getWordSet(paragraph.questions.get(quesNum).question);
        Set<String> aWords = Tools.getWordSet(paragraph.questions.get(quesNum).choices.get(ansNum));

        qWords.retainAll(pWords);
        qWords.removeAll(SentRel.stopWords);

        aWords.retainAll(pWords);
        aWords.removeAll(SentRel.stopWords);
        
        ArrayList<String> question_answer = null; 
        String element;
        Iterator iterator = aWords.iterator();
        
        while(iterator.hasNext())
        {
        	  element = (String) iterator.next();
        	  question_answer.add(element);
        }
        
        iterator = qWords.iterator();
        while(iterator.hasNext())
        {
        	  element = (String) iterator.next();
        	  question_answer.add(element);
        }
        
        List<CoreMap> sentences = paragraph.annotation.get(CoreAnnotations.SentencesAnnotation.class);
        double simi = - Double.MAX_VALUE;
        
        // compare for all the sentences  
        
        for(CoreMap sentence : sentences) {
            
        	List<CoreLabel> words = sentence.get(CoreAnnotations.TokensAnnotation.class);
            
        	ArrayList<String> sentence_words = null;
            
        	for(int i = 0 ; i < words.size(); i++)
        	{
        		CoreLabel wordLabel = words.get(i);
        		String word = Tools.getText(wordLabel);
        		sentence_words.add(word);
        	}
        	
        	double simi_current = Tools.GetWordVectorSimiSum(SentRel.wordVec, question_answer, sentence_words);
        	if (simi_current > simi )
        	{
        		simi = simi_current;
        	}
           			
        }
        	
        return 	simi;
    }
    
    static double IC(Map<String, Integer> countMap, String word) {
        
    	return Math.log(1 + 1./countMap.get(word));
    
    }

    static double ITF(String word) {
        
    	Integer cnt = SentRel.word2Cnt.get(word);
        if(cnt==null)
            return 0;
        return Math.log(1 + 1./cnt);
        
    }


    static double slidingWindow(Paragraph paragraph, int quesNum, int ansNum) {
        
    	Map<String, Integer> countMap = getCountMap(paragraph);

        Set<String> qWords = Tools.getWordSet(paragraph.questions.get(quesNum).question);

        Set<String> aWords = Tools.getWordSet(paragraph.questions.get(quesNum).choices.get(ansNum));
        aWords.addAll(qWords);

        int slideSize = aWords.size();
        
        double maxValue = - Double.MAX_VALUE;
        ArrayList<String> wordSeq = Tools.getWordSeq(paragraph);

        for(int i=0;i<wordSeq.size(); i++) {
            double sum = 0;
            for(int j=0;j<Math.min(slideSize, wordSeq.size()-i);j++) {
                String w = wordSeq.get(i+j);
                if(aWords.contains(w))
                    sum += IC(countMap, w);
            }
            if(sum > maxValue)
                maxValue = sum;
        }

        return maxValue;
    }

    static double distanceBased(Paragraph paragraph, int quesNum, int ansNum) {
        
    	Set<String> pWords = Tools.getWordSet(paragraph);
        Set<String> qWords = Tools.getWordSet(paragraph.questions.get(quesNum).question);
        Set<String> aWords = Tools.getWordSet(paragraph.questions.get(quesNum).choices.get(ansNum));

        qWords.retainAll(pWords);
        qWords.removeAll(SentRel.stopWords);

        aWords.retainAll(pWords);
        aWords.removeAll(SentRel.stopWords);

        
        if(qWords.size()==0 || aWords.size()==0)
            return 1.;

        ArrayList<String> wordSeq = Tools.getWordSeq(paragraph);
        double avg = 0;
        HashMap<String, Double> minD = new HashMap<>();

        for(int i=0;i<wordSeq.size(); i++)
            if(qWords.contains(wordSeq.get(i))) {
                String qWord = wordSeq.get(i);
                for(int j = 0; j < wordSeq.size() ; j++) {
                    if((i-j >=0 && aWords.contains(wordSeq.get(i-j)))
                            || (i+j < wordSeq.size() && aWords.contains(wordSeq.get(i + j)))) {
                        if(!minD.containsKey(qWord) || j < minD.get(qWord))
                            minD.put(qWord, (double)j);
                        break;
                    }
                }
            }

        assert minD.size() == qWords.size();

        for(double val : minD.values())
            avg += val;

        avg /= minD.size();
        return avg / (wordSeq.size()-1);
        
    }

    
    static HashMap<Integer, Double> swdFeatures(Paragraph paragraph, int sentNum, int quesNum, int ansNum) {

        
    	HashMap<Integer, Double> features = new HashMap<>();

        double sw = slidingWindow(paragraph, quesNum, ansNum);
        double D = distanceBased(paragraph, quesNum, ansNum);
        double exclusive_fre = Max_frequency_exclusive(paragraph, quesNum, ansNum);
        double frequency = Max_frequency(paragraph, quesNum, ansNum);
        double word_embeding = Word_embedding(paragraph, quesNum, ansNum); 
        
        Tools.addFeatureIncrement(features, "SW", sw);
        Tools.addFeatureIncrement(features, "Dist", D);
        Tools.addFeatureIncrement(features, "SW+D", sw - D); 						// the weight should change automatically
        Tools.addFeatureIncrement(features, "Frequency", frequency); 				// the weight should change automatically
        Tools.addFeatureIncrement(features, "Frequence_Ex", exclusive_fre); 		// the weight should change automatically
        Tools.addFeatureIncrement(features, "Word_Embedding", word_embeding); 		// the weight should change automatically
        
        return features;
    }

}
