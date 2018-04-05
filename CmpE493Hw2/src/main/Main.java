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
		System.out.println("Creating dictionary...");
		ArrayList<String> dictionary = createDictionary(documents);
		System.out.println("Creating dictionary DONE.");
		// Calculate topic probabilities.
		StoryClassifier.setTopicProbabilities(calculateTopicProbabilities(documents));
		// Count terms for each topic.
		HashMap<String, HashMap<String, Integer>> termCounts = countTermsPerTopic(dictionary, documents);
		// Calculate probabilities of each term for each topic.
		System.out.println("Calculating probabilities of terms...");
		StoryClassifier.setTermProbabilities(calculateTermProbabilities(termCounts, dictionary));
		System.out.println("Calculating probabilities of terms DONE.");
		// Try to classify test stories.
		System.out.println("Classifying test documents...");
		StoryClassifier.classifyTestDocuments(documents);
		// Calculate mutual information.
		HashMap<String, HashMap<String, Double>> mutualInfos = calculateMutualInformation(documents, dictionary);
		// Merge the distinctive words to create new vocabulary.
		Set<String> distinctiveTerms = new HashSet<>();
		for (String topic : Constants.topicsSet) {
			distinctiveTerms.addAll(mutualInfos.get(topic).keySet());
		}
		// Discard all other words.
		ArrayList<ArrayList<NewsStory>> updatedDocuments = updateDocumentsWithWords(documents, distinctiveTerms);
		// Update the dictionary.
		ArrayList<String> updatedDictionary = createDictionary(updatedDocuments);
		// Recount the terms.
		HashMap<String, HashMap<String, Integer>> updatedTermCounts = countTermsPerTopic(updatedDictionary, updatedDocuments);
		// Recalculate the probabilities
		StoryClassifier.setTermProbabilities(calculateTermProbabilities(updatedTermCounts, updatedDictionary));
		// Classify with the updated probabilities.
		System.out.println("Classifying test documents with mutual information...");
		StoryClassifier.classifyTestDocuments(updatedDocuments);	
	}

	/**
	 * Discards all tokens that are not in the distinctive terms set.
	 */
	private static ArrayList<ArrayList<NewsStory>> updateDocumentsWithWords(ArrayList<ArrayList<NewsStory>> documents,
			Set<String> distinctiveTerms) {
		ArrayList<ArrayList<NewsStory>> updatedDocuments = new ArrayList<>();
		for (ArrayList<NewsStory> doc : documents) {
			ArrayList<NewsStory> updatedDoc = new ArrayList<>();
			for (NewsStory story : doc) {
				NewsStory updatedStory = new NewsStory();
				// Keep the fixed information
				updatedStory.storyID = story.storyID;
				updatedStory.lewissplit = story.lewissplit;
				updatedStory.topic = story.topic;
				// Update title
				for (String token : story.titleTokens) {
					if (distinctiveTerms.contains(token)) {
						updatedStory.titleTokens.add(token);
						if (	updatedStory.termCounts.containsKey(token)) {
							updatedStory.termCounts.put(token, updatedStory.termCounts.get(token) + 1);
						} else {
							updatedStory.termCounts.put(token, 1);
						}
					}
				}
				// Update body
				for (String token : story.bodyTokens) {
					if (distinctiveTerms.contains(token)) {
						updatedStory.bodyTokens.add(token);
						if (	updatedStory.termCounts.containsKey(token)) {
							updatedStory.termCounts.put(token, updatedStory.termCounts.get(token) + 1);
						} else {
							updatedStory.termCounts.put(token, 1);
						}
					}
				}
				updatedDoc.add(updatedStory);
			}
			updatedDocuments.add(updatedDoc);
		}
		return updatedDocuments;
	}

	/**
	 * Calculates the most distinctive 50 words of each topic.
	 */
	private static HashMap<String, HashMap<String, Double>> calculateMutualInformation(ArrayList<ArrayList<NewsStory>> documents,
			ArrayList<String> dictionary) {
		// Initialize the arrays.
		HashMap<String, HashMap<String, Integer>> termCounts = new HashMap<>();
		HashMap<String, Integer> documentCounts = new HashMap<>();
		for (String topic : Constants.topicsSet) {
			termCounts.put(topic, new HashMap<String, Integer>());
			documentCounts.put(topic, 0);
		}
		// Fill in the arrays.
		for (ArrayList<NewsStory> doc : documents) {
			for (NewsStory story : doc) {
				// Only use training documents.
				if (!story.lewissplit.equals("TRAIN")) {
					continue;
				}
				// Get the topic.
				String currentTopic = story.topic; 
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
		// Calculate total document count.
		int totalDocCount = 0;
		for (String topic : Constants.topicsSet) {
			totalDocCount += documentCounts.get(topic);
		}
		// Calculate mutual information for each term, using the values found above.
		HashMap<String, HashMap<String, Double>> allMutualInfos = new HashMap<>();
		System.out.println("Calculating mutual information...");
		for (String topic : Constants.topicsSet) {
			HashMap<String, Double> mutualInfos = new HashMap<>();
			for (String term : dictionary) {
				// The stories containing this term and are from this topic.
				int yTermYTopic = (termCounts.get(topic).containsKey(term) ? termCounts.get(topic).get(term) : 1);
				// The stories containing this term and are not from this topic.
				int yTermNTopic = 1;
				for (String _topic : Constants.topicsSet) {
					if (!_topic.equals(topic) && termCounts.get(_topic).containsKey(term)) {
						yTermNTopic += termCounts.get(_topic).get(term);
					}
				}		
				// The stories not containing this term and are from this topic.
				int nTermYTopic = documentCounts.get(topic) - yTermYTopic;
				// The stories not containing this term and are not from this topic.
				int nTermNTopic = totalDocCount - documentCounts.get(topic) - yTermNTopic;
				
				// Prepare the parts of the sum
				double part1 = (yTermYTopic/(double)totalDocCount) 
						* Math.log((yTermYTopic*totalDocCount) / (double)((yTermYTopic + yTermNTopic)*(yTermYTopic + nTermYTopic)));
				double part2 = (nTermYTopic/(double)totalDocCount) 
						* Math.log((nTermYTopic*totalDocCount) / (double)((nTermYTopic + nTermNTopic)*(yTermYTopic + nTermYTopic)));
				double part3 = (yTermNTopic/(double)totalDocCount) 
						* Math.log((yTermNTopic*totalDocCount) / (double)((yTermYTopic + yTermNTopic)*(yTermNTopic + nTermNTopic)));
				double part4 = (nTermNTopic/(double)totalDocCount) 
						* Math.log((nTermNTopic*totalDocCount) / (double)((nTermYTopic + nTermNTopic)*(yTermNTopic + nTermNTopic)));
						
				// Calculate mutual information of the term.
				double mutualInformation = part1 + part2 + part3 + part4;
				
				// Only keep 50 words with the highest mutual information values.
				if (mutualInfos.size() < 50) {
					mutualInfos.put(term, mutualInformation);
				} else {
					String minKey = (String) mutualInfos.keySet().toArray()[0];
					double minVal = mutualInfos.get(minKey);
					for (String key : mutualInfos.keySet()) {
						if (minVal > mutualInfos.get(key)) {
							minVal = mutualInfos.get(key);
							minKey = key;
						}	
					}
					if (minVal < mutualInformation) {
						mutualInfos.remove(minKey);
						mutualInfos.put(term, mutualInformation);
					}
				}
			}
			allMutualInfos.put(topic, mutualInfos);
		}
		System.out.println("Calculating mutual information DONE.");
		return allMutualInfos;
	}

	/**
	 * Returns a map containing the term probabilities for each topic.
	 */
	private static HashMap<String, HashMap<String, Double>> calculateTermProbabilities(
			HashMap<String, HashMap<String, Integer>> termCounts, ArrayList<String> dictionary) {
		HashMap<String, HashMap<String, Double>> result = new HashMap<>();
		for (String topic : Constants.topicsSet) {
			result.put(topic, calculateTermProbabilitiesForTopic(termCounts.get(topic), dictionary));	
		}		
		return result;
	}

	/**
	 * Returns a map containing the term probabilities for a specified topic.
	 */
	private static HashMap<String, Double> calculateTermProbabilitiesForTopic(HashMap<String, Integer> termCountsOfTopic, ArrayList<String> dictionary) {
		HashMap<String, Double> probs = new HashMap<>();
		for (String term : dictionary) {
			// Numerator: number of times this term occurs in this topic + 1.
			int numerator = (termCountsOfTopic.containsKey(term) ? termCountsOfTopic.get(term) : 0) + 1;
			// Denominator: total number of terms in this topic + dictionary size.
			int denominator = 0;
			for (int count : termCountsOfTopic.values()) {
				denominator += count;
			}
			denominator += dictionary.size();
			probs.put(term, Math.log(numerator/(double)denominator));
		}
		return probs;
	}

	/**
	 * Returns a map containing the counts of terms for each topic.
	 */
	private static HashMap<String, HashMap<String, Integer>> countTermsPerTopic(ArrayList<String> dictionary,
			ArrayList<ArrayList<NewsStory>> documents) {
		HashMap<String, HashMap<String, Integer>> result = new HashMap<>();
		for (String topic : Constants.topicsSet) {
			result.put(topic, countTermsForTopic(topic, dictionary, documents));	
		}
		return result;
	}
	
	/**
	 * Returns a map containing the counts of terms for the specified topic.
	 */
	private static HashMap<String, Integer> countTermsForTopic(String topic, ArrayList<String> dictionary,
			ArrayList<ArrayList<NewsStory>> documents) {
		HashMap<String, Integer> result = new HashMap<>();
		for (ArrayList<NewsStory> doc : documents) {
			for (NewsStory story : doc) {
				// Only consider TRAIN documents from this topic.
				if (!story.lewissplit.equals("TRAIN") || !story.topic.equals(topic)) {
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
		HashMap<String, Integer> topicCounts = new HashMap<>();
		int storyCount = 0;
		// Initialize the counts map.
		for (String topic : Constants.topicsSet) {
			topicCounts.put(topic, 0);
		}
		// Count the stories of each topic
		for (int i = 0; i < documents.size(); i++) {
				for (NewsStory story : documents.get(i)) {
					// Only consider the training documents.
					if (!story.lewissplit.equals("TRAIN")) {
						continue;
					}
					storyCount++;
					
					for (String topic : Constants.topicsSet) {
						if (story.topic.equals(topic)) {
							topicCounts.put(topic, topicCounts.get(topic) + 1);	
						}
					}
				}
		}
		// Calculate probabilities
		HashMap<String, Double> topicProbabilities = new HashMap<>();
		for (String topic : topicCounts.keySet()) {
			topicProbabilities.put(topic, Math.log(topicCounts.get(topic)/(double)storyCount));
		}
		return topicProbabilities;
	}

	/**
	 * Creates an array list of all unique stemmed words in all documents.
	 */
	private static ArrayList<String> createDictionary(ArrayList<ArrayList<NewsStory>> documents) {
		ArrayList<String> dictionary = new ArrayList<>();
		for (int i = 0; i < documents.size(); i++) {
			for (NewsStory story : documents.get(i)) {
				// Only consider files for training.
				if (!story.lewissplit.equals("TRAIN")) {
					continue;
				}
				for (String word : story.termCounts.keySet()) {
					if (!dictionary.contains(word)) {
						dictionary.add(word);
					}
				}
			}
		}
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
		try (BufferedReader br = new BufferedReader(new FileReader(Constants.stopWordsLocation))) {
			String line;
		    while ((line = br.readLine()) != null) {
		    		stopwords.add(line.trim());
		    }
		} catch (IOException e) {
			System.out.println("Error while reading stopwords form Dataset/stopwords.txt");
			e.printStackTrace();
		}
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
