package main;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class reads a given document and creates a
 * NewsStory array with body and title fields.
 */
public class StoryExtractor {
	/**
	 * Given the file name, extracts the stories using body and title tags
	 * within text tags. Returns an array of NewsStory objects.
	 */
	public static ArrayList<NewsStory> getStoriesFromDocument(String fileName) {
		// Tokenize by tags and lines first.
		ArrayList<String> tagTokens = null;
		try {
			tagTokens = tokenizeByTagsAndStrings(fileName);
		} catch (IOException e) {
			System.out.println("Failed to tokenize document " + fileName + " by tags and strings.");
			e.printStackTrace();
			return null;
		}
		// Organize tokens to merge non-token entries.
		tagTokens = organizeTagsAndStrings(tagTokens);
		// Extract texts, ids, topics of stories.
		ArrayList<NewsStory> storyInfos = extractTextsIDsTopics(tagTokens);
		// Extract title and body of texts.
		ArrayList<NewsStory> stories = extractTitleAndBody(storyInfos);
		// Return result
		return stories;
	}
	
	/**
	 * Given story texts, extracts the title and body texts for each story.
	 * Creates NewsStory objects with them and returns an array.
	 */
	private static ArrayList<NewsStory> extractTitleAndBody(ArrayList<NewsStory> storyInfos) {
		ArrayList<NewsStory> stories = new ArrayList<>();
		for (NewsStory info : storyInfos) {
			NewsStory story = new NewsStory();
			// Set the id.
			story.storyID = info.storyID;
			// Set the topics.
			story.topics = info.topics;
			for (int i = 0; i < info.text.size(); i++) {
				// Find the title
				if(info.text.get(i).equals("<TITLE>")) {
					// Find the end of the title.
					for (int j = i+1; j < info.text.size(); j++) {
						if(info.text.get(j).equals("</TITLE>")) {
							break;
						}
						story.title += " " + info.text.get(j);
					}		
				}
				
				// Find the body
				if(info.text.get(i).equals("<BODY>")) {
					// Find the end of the body.
					for (int j = i+1; j < info.text.size(); j++) {
						if(info.text.get(j).equals("</BODY>")) {
							// fix '<' character.
							story.title = story.title.replaceAll("&lt;", "<");
							story.body = story.body.replaceAll("&lt;", "<");
							// Fix end of file character.
							story.title = story.title.replaceAll("&#3;", "");
							story.body = story.body.replaceAll("&#3;", "");
							// Add story to story list.
							stories.add(story);
							break;
						}
						story.body += " " + info.text.get(j);
					}		
				}
			}
		}
		return stories;
	}
	
	/**
	 * Given an organized string array of tags and non-tags,
	 * creates an array of news stories where
	 * the field _text_ is story's content (between text tags)
	 * the field _storyID_ is the story's id (given as NEWID)
	 * the field _topics_ is the array of string topics.
	 */
	private static ArrayList<NewsStory> extractTextsIDsTopics(ArrayList<String> tokens) {
		ArrayList<NewsStory> stories = new ArrayList<>();
		NewsStory story = new NewsStory();
		for (int i = 0; i < tokens.size(); i++) {
			// Find the id.
			if (tokens.get(i).contains("NEWID=\"")) {
				// Possible new story. Reset everything.
				story = new NewsStory();
				// Extract id.
				String currentId = "";
				for (int j = tokens.get(i).indexOf("NEWID=\"") + 7; 
						j < tokens.get(i).length(); j++) {
					if (tokens.get(i).charAt(j) != '"') {
						currentId += tokens.get(i).charAt(j);
					} else {
						break;
					}
				}
				try {
					story.storyID = Integer.parseInt(currentId);	
				} catch (Exception e) {
					System.out.println("Error while getting the id of the story.");
				}
			}
			// Find the topics
			if (tokens.get(i).equals("<TOPICS>")) {
				// Add topics between <D> and </D> until the end of the topics section.
				for (int j = i+1; j + 1 < tokens.size(); j++) {
					if (tokens.get(j).equals("</TOPICS>")) {
						break;
					}
					if (tokens.get(j).equals("<D>") && !tokens.get(j+1).equals("</D>")) {
						story.topics.add(tokens.get(j+1));
					}
				}
			}
			
			// Find the beginning of the text.
			if (tokens.get(i).equals("<TEXT>")) {
				// Find the ending of the text and create the news story from tokens in between.
				for (int j = i+1; j < tokens.size(); j++) {
					if (tokens.get(j).equals("</TEXT>")) {
						stories.add(story);
						break;
					}
					story.text.add(tokens.get(j));
				}
			}
		}
		return stories;
	}
	
	/**
	 * Reads the file line by line and returns a String array.
	 * The returned array will have elements as follows:
	 * - <TAG> and </TAG> types.
	 * - Substrings of a line:
	 *   - Part up to first tag
	 *   - Part between two tags
	 *   - Part after last tag until the end of line.
	 */
	private static ArrayList<String> tokenizeByTagsAndStrings(String fileName) throws FileNotFoundException, IOException {
		ArrayList<String> tokens = new ArrayList<>();
		// Ready pattern to find tags.
		Pattern tagPattern = Pattern.compile("<(.*?)>");
		// Process document line by line.
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			String line;
		    while ((line = br.readLine()) != null) {
				// Get first match.
				Matcher m = tagPattern.matcher(line);
				while (m.find()) {
					// If there is text before match, add as another token.
					if (m.start() != 0) {
						tokens.add(line.substring(0, m.start()));
					}
					// Add the match.
					tokens.add(m.group());
					// Update the line.
					line = line.substring(m.end());
					m = tagPattern.matcher(line);
				}
				// Add the remaining text as token.
				if (!line.isEmpty()) {
					tokens.add(line);	
				}
		    }
		}
		return tokens;
	}
	
	/**
	 * Takes in an array list of strings with tags and non-tags.
	 * Merges every non-tag entry into one element.
	 * Preserves the order.
	 */
	private static ArrayList<String> organizeTagsAndStrings(ArrayList<String> tokens) {
		ArrayList<String> organizedTokens = new ArrayList<>();
		// Initial token should always be a tag (the DOCTYPE tag).
		organizedTokens.add(tokens.get(0));
		tokens.remove(0);
		boolean isPreviousTokenTag = true;
		// Process remaining tokens.
		for (String token : tokens) {
			if (isTag(token)) {
				// If it is a tag, add it directly
				organizedTokens.add(token);
				isPreviousTokenTag = true;
			} else if (isPreviousTokenTag) {
				// If previous token was a tag, add this one as the next token.
				organizedTokens.add(token);
				isPreviousTokenTag = false;
			} else {
				// If both previous and this tokens are not tags, merge them.
				String prev = organizedTokens.get(organizedTokens.size()-1);
				organizedTokens.remove(organizedTokens.size()-1);
				organizedTokens.add(prev + "\n" + token);
			}
		}
		return organizedTokens;
	}		
	
	/**
	 * Takes in a string.
	 * Returns true if it starts with < and ends with >.
	 * Returns false otherwise.
	 */
	private static boolean isTag(String token) {
		return token.charAt(0) == '<' && token.charAt(token.length()-1) == '>';
	}
}
