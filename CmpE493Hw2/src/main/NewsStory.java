package main;

import java.util.ArrayList;
import java.util.HashMap;

public class NewsStory {
	int storyID;
	String lewissplit;
	String  title = "";
	String body = "";
	ArrayList<String> titleTokens = new ArrayList<>();
	ArrayList<String> bodyTokens = new ArrayList<>();
	String topic = "";
	ArrayList<String> text = new ArrayList<>();
	HashMap<String, Integer> termCounts = new HashMap<>();
}