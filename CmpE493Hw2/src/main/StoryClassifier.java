package main;

import java.util.HashMap;

public class StoryClassifier {
	static HashMap<String, Integer> topicProbabilities;
	static HashMap<String, HashMap<String, Double>> termProbabilities;
	
	public static void setTermProbabilities(HashMap<String, HashMap<String, Double>> termProbs) {
		termProbabilities = termProbs;
	}

	public static void setTopicProbabilities(HashMap<String, Integer> topicProbs) {
		topicProbabilities = topicProbs;
	}

}
