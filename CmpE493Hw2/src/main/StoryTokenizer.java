package main;

import java.util.ArrayList;

public class StoryTokenizer {
	static ArrayList<String> stopWords;

	/**
	 * Sets the stopWords of this class.
	 */
	public static void setStopWords(ArrayList<String> stopWords) {
		StoryTokenizer.stopWords = stopWords;
	}
}
