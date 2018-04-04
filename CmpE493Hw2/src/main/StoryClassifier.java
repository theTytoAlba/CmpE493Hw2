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
		int correct = 0;
		int total = 0;
		for (ArrayList<NewsStory> doc : documents) {
			for (NewsStory story : doc) {
				if (story.lewissplit.equals("TEST")) {
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
					}
					total++;
				}
			}
		}
		System.out.println("Correctness: " + correct + "/" + total + "=" + correct/(double)total);
	}

	private static double calculateProbForTopic(String topic, NewsStory story) {
		double result = topicProbabilities.get(topic);
		// Process title
		for (String term : story.titleTokens) {
			if (termProbabilities.get(topic).containsKey(term)) {
				result += termProbabilities.get(topic).get(term);
			}
		}
		// Process body
		for (String term : story.bodyTokens) {
			if (termProbabilities.get(topic).containsKey(term)) {
				result += termProbabilities.get(topic).get(term);
			}
		}
		return result;
	}

}
