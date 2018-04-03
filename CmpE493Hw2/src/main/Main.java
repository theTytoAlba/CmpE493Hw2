package main;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Main {

	public static void main(String[] args) {
		// Read stories from documents.
		ArrayList<ArrayList<NewsStory>>  documents = readStoriesFromDocuments();
		// Read the stop words.
		StoryTokenizer.setStopWords(readStopWords());
		// Tokenize the stories.
		documents = tokenizeStories(documents);
		// Create dictionary.
		ArrayList<String> dictionary = createDictionary(documents);
		// Calculate topic probabilities.
		StoryClassifier.setTopicProbabilities(calculateTopicProbabilities(documents));
		// Count terms for each topic.
		HashMap<String, HashMap<String, Integer>> termCounts = countTermsPerTopic(dictionary, documents);
		// Calculate probabilities of each term for each topic.
		StoryClassifier.setTermProbabilities(calculateTermProbabilities(termCounts));
		// Try to classify test stories.
		System.out.println("Classifying test documents...");
		StoryClassifier.classifyTestDocuments(documents);
		System.out.println("Classifying test documents DONE.");
		HashMap<String, HashMap<String, Double>> mutualInfos = calculateMutualInformation(documents, dictionary);
		Set<String> distinctiveTerms = new HashSet<>();
		for (String topic : Constants.topicsSet) {
			distinctiveTerms.addAll(mutualInfos.get(topic).keySet());
		}
		System.out.println("Updating documents with mutual information...");
		ArrayList<ArrayList<NewsStory>> updatedDocuments = updateDocumentsWithWords(documents, distinctiveTerms);
		// Create dictionary.
		System.out.println("Creating dictionary with mutual information...");
		ArrayList<String> updatedDictionary = createDictionary(updatedDocuments);
		System.out.println("Updating term counts with mutual information...");
		HashMap<String, HashMap<String, Integer>> updatedTermCounts = countTermsPerTopic(updatedDictionary, updatedDocuments);
		StoryClassifier.setTermProbabilities(calculateTermProbabilities(updatedTermCounts));
		System.out.println("Classifying test documents with mutual information...");
		StoryClassifier.classifyTestDocuments(updatedDocuments);
		System.out.println("Classifying test documents with mutual information DONE.");	
	
	}

	private static ArrayList<ArrayList<NewsStory>> updateDocumentsWithWords(ArrayList<ArrayList<NewsStory>> documents,
			Set<String> distinctiveTerms) {
		ArrayList<ArrayList<NewsStory>> updatedDocuments = new ArrayList<>();
		for (ArrayList<NewsStory> doc : documents) {
			ArrayList<NewsStory> updatedDoc = new ArrayList<>();
			for (NewsStory story : doc) {
				NewsStory updatedStory = new NewsStory();
				updatedStory.storyID = story.storyID;
				updatedStory.lewissplit = story.lewissplit;
				updatedStory.topics = story.topics;
				for (String token : story.titleTokens) {
					if (distinctiveTerms.contains(token)) {
						updatedStory.titleTokens.add(token);
					}
				}
				for (String token : story.bodyTokens) {
					if (distinctiveTerms.contains(token)) {
						updatedStory.bodyTokens.add(token);
					}
				}
				updatedDoc.add(updatedStory);
			}
			updatedDocuments.add(updatedDoc);
		}
		return updatedDocuments;
	}

	private static HashMap<String, HashMap<String, Double>> calculateMutualInformation(ArrayList<ArrayList<NewsStory>> documents,
			ArrayList<String> dictionary) {
		System.out.println("Creating term counts data...");
		HashMap<String, HashMap<String, Integer>> termCounts = new HashMap<>();
		HashMap<String, Integer> documentCounts = new HashMap<>();
		for (String topic : Constants.topicsSet) {
			termCounts.put(topic, new HashMap<String, Integer>());
			documentCounts.put(topic, 0);
		}
		for (ArrayList<NewsStory> doc : documents) {
			for (NewsStory story : doc) {
				if (!story.lewissplit.equals("TRAIN")) {
					continue;
				}
				// Get the topic.
				String currentTopic = "";
				for (String topic : Constants.topicsSet) {
					currentTopic = story.topics.contains(topic) ? topic : currentTopic; 
				}
				// Update counts for this topic with this story's terms.
				for (String term: story.termCounts.keySet()) {
					if (termCounts.get(currentTopic).containsKey(term)) {
						termCounts.get(currentTopic).put(term, termCounts.get(currentTopic).get(term) + 1);
					} else {
						termCounts.get(currentTopic).put(term, 1);	
					}
				}
				// Update total document number for topic
				documentCounts.put(currentTopic, documentCounts.get(currentTopic) + 1);
			}
		}
		int totalDocCount = 0;
		for (String topic : Constants.topicsSet) {
			totalDocCount += documentCounts.get(topic);
		}
		System.out.println("Creating term counts data DONE.");
		
		HashMap<String, HashMap<String, Double>> allMutualInfos = new HashMap<>();
		System.out.println("Calculating mutual information...");
		for (String topic : Constants.topicsSet) {
			System.out.println("Calculating mutual information for " + topic + "...");
			HashMap<String, Double> mutualInfos = new HashMap<>();
			for (String term : termCounts.get(topic).keySet()) {
				int yTermYTopic = termCounts.get(topic).get(term);
				int yTermNTopic = 0;
				for (String _topic : Constants.topicsSet) {
					if (!_topic.equals(topic) && termCounts.get(_topic).containsKey(term)) {
						yTermNTopic += termCounts.get(_topic).get(term);
					}
				}		
				int nTermYTopic = documentCounts.get(topic) - yTermYTopic;
				int nTermNTopic = 0;
				for (String _topic : Constants.topicsSet) {
					if (!_topic.equals(topic) && !termCounts.get(_topic).containsKey(term)) {
						nTermNTopic += documentCounts.get(_topic);
					}
				}
				
				double part1 = (yTermYTopic/(double)totalDocCount) 
						* Math.log((yTermYTopic*totalDocCount) / (double)((yTermYTopic + yTermNTopic)*(yTermYTopic + nTermYTopic)));
				double part2 = (nTermYTopic/(double)totalDocCount) 
						* Math.log((nTermYTopic*totalDocCount) / (double)((nTermYTopic + nTermNTopic)*(yTermYTopic + nTermYTopic)));
				double part3 = (yTermNTopic/(double)totalDocCount) 
						* Math.log((yTermNTopic*totalDocCount) / (double)((yTermYTopic + yTermNTopic)*(yTermNTopic + nTermNTopic)));
				double part4 = (nTermNTopic/(double)totalDocCount) 
						* Math.log((nTermNTopic*totalDocCount) / (double)((nTermYTopic + nTermNTopic)*(yTermNTopic + nTermNTopic)));
						
				double mutualInformation = part1 + part2 + part3 + part4;
				// The term only occurs in the documents belonging to topic, maximum mutual information.
				if (yTermNTopic == 0) {
					mutualInformation = Double.MAX_VALUE;
				}
				// There are no documents that do not contain this term in other topics, minimum mutual information.
				if (nTermNTopic == 0) {
					mutualInformation = Double.MIN_VALUE;
				}
				
				if (mutualInfos.size() < 50) {
					mutualInfos.put(term, mutualInformation);
				} else {
					for (String key : mutualInfos.keySet()) {
						if (mutualInfos.get(key) < mutualInformation) {
							mutualInfos.remove(key);
							mutualInfos.put(term, mutualInformation);
							break;
						}
					}
				}
			}
			allMutualInfos.put(topic, mutualInfos);
		}
		System.out.println("Calculating mutual information DONE.");
		//System.out.println(allMutualInfos.toString());
		return allMutualInfos;
	}

	private static HashMap<String, HashMap<String, Double>> calculateTermProbabilities(
			HashMap<String, HashMap<String, Integer>> termCounts) {
		System.out.println("Calculating probabilities of terms...");
		HashMap<String, HashMap<String, Double>> result = new HashMap<>();
		for (String topic : Constants.topicsSet) {
			System.out.println("Calculating probabilities for topic " + topic + "...");
			result.put(topic, calculateTermProbabilitiesForTopic(termCounts.get(topic)));	
		}		
		System.out.println("Calculating probabilities of terms DONE.");
		return result;
	}

	private static HashMap<String, Double> calculateTermProbabilitiesForTopic(HashMap<String, Integer> termCountsOfTopic) {
		HashMap<String, Double> probs = new HashMap<>();
		for (String term : termCountsOfTopic.keySet()) {
			// Numerator: number of times this term occurs in this topic + 1.
			int numerator = termCountsOfTopic.get(term) + 1;
			// Denominator: total number of terms in this topic + dictionary size.
			int denominator = 0;
			for (int count : termCountsOfTopic.values()) {
				denominator += count;
			}
			denominator += termCountsOfTopic.size();
			probs.put(term, Math.log(numerator/(double)denominator));
		}
		return probs;
	}

	private static HashMap<String, HashMap<String, Integer>> countTermsPerTopic(ArrayList<String> dictionary,
			ArrayList<ArrayList<NewsStory>> documents) {
		System.out.println("Counting terms for topics...");
		HashMap<String, HashMap<String, Integer>> result = new HashMap<>();
		for (String topic : Constants.topicsSet) {
			result.put(topic, countTermsForTopic(topic, dictionary, documents));	
		}
		System.out.println("Counting terms for topics DONE.");
		return result;
	}

	private static HashMap<String, Integer> countTermsForTopic(String topic, ArrayList<String> dictionary,
			ArrayList<ArrayList<NewsStory>> documents) {
		HashMap<String, Integer> result = new HashMap<>();
		System.out.println("Processing topic " + topic+ "...");
		for (ArrayList<NewsStory> doc : documents) {
			for (NewsStory story : doc) {
				// Only consider TRAIN documents from this topic.
				if (!story.lewissplit.equals("TRAIN") || !story.topics.contains(topic)) {
					continue;
				}
				// Add this story to the count.
				for (String term : story.termCounts.keySet()) {
					if (result.containsKey(term)) {
						result.put(term, result.get(term) + story.termCounts.get(term));
					} else {
						result.put(term, story.termCounts.get(term));
					}
				}
			}
		}
		return result;
	}

	/**
	 * Returns a map of the topic and the number of stories that contain that topic.
	 */
	private static HashMap<String, Double> calculateTopicProbabilities(ArrayList<ArrayList<NewsStory>> documents) {
		HashMap<String, Integer> probs = new HashMap<>();
		int storyCount = 0;
		for (String topic : Constants.topicsSet) {
			probs.put(topic, 0);
		}
		System.out.println("Calculating topic probabilities...");
		for (int i = 0; i < documents.size(); i++) {
				for (NewsStory story : documents.get(i)) {
					if (!story.lewissplit.equals("TRAIN")) {
						continue;
					}
					storyCount++;
					for (String topic : Constants.topicsSet) {
						if (story.topics.contains(topic)) {
							probs.put(topic, probs.get(topic) + 1);	
						}
					}
				}
		}
		HashMap<String, Double> result = new HashMap<>();
		for (String topic : probs.keySet()) {
			result.put(topic, Math.log(probs.get(topic)/(double)storyCount));
		}
		System.out.println("Calculating topic probabilities DONE.");
		return result;
	}

	/**
	 * Creates an array list of all unique stemmed words in all documents.
	 */
	private static ArrayList<String> createDictionary(ArrayList<ArrayList<NewsStory>> documents) {
		System.out.println("Creating dictionary...");
		ArrayList<String> dictionary = new ArrayList<>();
		for (int i = 0; i < documents.size(); i++) {
			printProgress("Processing document", i+1, 22);
			for (NewsStory story : documents.get(i)) {
				for (String word : story.termCounts.keySet()) {
					if (!dictionary.contains(word)) {
						dictionary.add(word);
					}
				}
			}
		}
		System.out.println("Creating dictionary DONE.");
		return dictionary;
	}

	/**
	 * Reads files from "Dataset/reut2-000.sgm" until "Dataset/reut2-021.sgm".
	 * Extracts stories with their title and bodies.
	 * Returns the news story arrays of every document in an array.
	 */
	private static ArrayList<ArrayList<NewsStory>> readStoriesFromDocuments() {
		ArrayList<ArrayList<NewsStory>> documents = new ArrayList<>();
		System.out.println("Reading documents...");
		// Read documents;
		for (int i = 0; i < 22; i++) {
			String fileName = "Dataset/reut2-0" + (i<10 ? "0" : "") + i + ".sgm";
			printProgress("Reading document", i+1, 22);
			documents.add(StoryExtractor.getStoriesFromDocument(fileName));
		}
		System.out.println("Reading documents DONE.");
		return documents;
	}
	
	/**
	 * Prints the pretext, prints a space, prints the progress as text like xx/yy,
	 * prints a semicolon and then prints the progress bar in form [#####----].
	 */
	private static void printProgress(String preText, int current, int total) {
		System.out.print(preText + " ");
		System.out.print((current<10 ? "0" : "") + current + "/" + total + ": [");
		for (int j = 0; j < current; j++) {
			System.out.print("#");
		}
		for (int j = current; j < total; j++) {
			System.out.print("-");
		}
		System.out.println("]");
	}
	
	/**
	 * Reads stop words from the location in Constants.
	 */
	private static ArrayList<String> readStopWords() {
		ArrayList<String> stopwords = new ArrayList<>();
		System.out.println("Reading stop words...");
		try (BufferedReader br = new BufferedReader(new FileReader(Constants.stopWordsLocation))) {
			String line;
		    while ((line = br.readLine()) != null) {
		    		stopwords.add(line.trim());
		    }
		} catch (IOException e) {
			System.out.println("Error while reading stopwords form Dataset/stopwords.txt");
			e.printStackTrace();
		}
		System.out.println("Reading stop words DONE.");
		return stopwords;
	}
	
	/**
	 * Given an array containing the story arrays for each document,
	 * iterates the array and tokenizes and stems each story.
	 */
	private static ArrayList<ArrayList<NewsStory>> tokenizeStories(ArrayList<ArrayList<NewsStory>> documents) {
		ArrayList<ArrayList<NewsStory>> tokenizedDocuments = new ArrayList<>();
		System.out.println("Tokenizing documents...");
		// Tokenize documents;
		for (int i = 0; i < documents.size(); i++) {
			printProgress("Tokenizing document", i+1, 22);
			tokenizedDocuments.add(StoryTokenizer.tokenizeStories(documents.get(i)));
		}
		System.out.println("Tokenizing documents DONE.");
		return tokenizedDocuments;
	}
}
