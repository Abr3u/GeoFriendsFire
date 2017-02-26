package pt.utl.ist.meic.domain;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class UserProfile {

	private Map<String, Graph> mGraphs;
	public String userId;

	private Map<String, Double> similarityScores;

	public UserProfile(String userId) {
		this.userId = userId;
		mGraphs = new HashMap<String, Graph>();
		similarityScores = new HashMap<String, Double>();
	}

	public void addNewGraph(String level, Graph graph) {
		this.mGraphs.put(level, graph);
	}

	public Graph getGraphByLevel(String level) {
		return this.mGraphs.get(level);
	}

	public void addSimilarityScore(String userId, double score) {
		// no simmilarity with yourself
		if (!this.userId.equals(userId)) {
			similarityScores.put(userId, score);
		}
	}

	public void printSimilarities() {
		System.out.println("Similarites for user "+userId);
		similarityScores.entrySet().stream()
		.sorted(Map.Entry.<String, Double>comparingByValue().reversed())
		.forEach(System.out::println);
	}

}
