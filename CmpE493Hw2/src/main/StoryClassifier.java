package main;

import java.util.ArrayList;
import java.util.HashMap;

public class StoryClassifier {
	static HashMap<String, Double> topicProbabilities;
	static HashMap<String, HashMap<String, Double>> termProbabilities;
	
	public static void setTermProbabilities(HashMap<String, HashMap<String, Double>> termProbs) {
		termProbabilities = termProbs;
	}

	public static void setTopicProbabilities(HashMap<String, Double> topicProbs) {
		topicProbabilities = topicProbs;
	}

	public static void classifyTestDocuments(ArrayList<ArrayList<NewsStory>> documents) {
		HashMap<String, Integer> precCorrect = new HashMap<>();
		HashMap<String, Integer> precFalse = new HashMap<>();
		HashMap<String, Integer> recFalse = new HashMap<>();
		for (String top : Constants.topicsSet) {
			precCorrect.put(top, 0)	;
			precFalse.put(top, 0)	;
			recFalse.put(top, 0)	;
		}
		int correct = 0;
		int total = 0;
		for (ArrayList<NewsStory> doc : documents) {
			for (NewsStory story : doc) {
				if (!story.lewissplit.equals("TEST")) {
					continue;
				}
				String currentType = Constants.topicsSet.get(0);
				double currentProb = calculateProbForTopic(Constants.topicsSet.get(0), story);
				for (int i = 1; i < 5; i++) {
					double newProb = calculateProbForTopic(Constants.topicsSet.get(i), story);
					if (newProb > currentProb) {
						currentProb = newProb;
						currentType = Constants.topicsSet.get(i);
					}
				}
				if (story.topic.equals(currentType)) {
					correct++;
					precCorrect.put(currentType, precCorrect.get(currentType) + 1);
				} else {
					precFalse.put(currentType, precFalse.get(currentType) + 1);
					recFalse.put(story.topic, precFalse.get(story.topic) + 1);
				}
				total++;
			}
		}
		System.out.println();
		System.out.println("Correctly classified: " + correct + "/" + total + "=" + correct/(double)total);
		System.out.println();
		System.out.println("Correcly classified documents by topic: " + precCorrect.toString());
		System.out.println("Falsely classified documents by their classified topic: " +precFalse.toString());
		System.out.println("Falsely classified documents by their actual topic: " + recFalse.toString());
		System.out.println();
		for (String top : Constants.topicsSet) {
			System.out.println("Precision for topic " + top + ": " +  (precCorrect.get(top)/(double)(precCorrect.get(top) + precFalse.get(top))));
			System.out.println("Recall for topic " + top + ": " +(precCorrect.get(top)/(double)(precCorrect.get(top) + recFalse.get(top))));
		}
		System.out.println();
	}

	private static double calculateProbForTopic(String topic, NewsStory story) {
		double result = topicProbabilities.get(topic);
		for (String term : story.termCounts.keySet()) {
			if (termProbabilities.get(topic).containsKey(term)) {
				result += termProbabilities.get(topic).get(term) * story.termCounts.get(term);
			}
		}
		return result;
	}

}
