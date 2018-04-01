package main;
import java.util.ArrayList;

public class Main {

	public static void main(String[] args) {
		// Read stories from documents.
		ArrayList<ArrayList<NewsStory>>  documents = readStoriesFromDocuments();
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
			//TODO: documents.add(StoryExtractor.getStoriesFromDocument(fileName));
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
}
