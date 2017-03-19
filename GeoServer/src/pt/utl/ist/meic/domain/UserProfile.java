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
	
	public double getSimilarityScore(String userId){
		return this.similarityScores.get(userId);
	}
	
	public double getMaxSimilarityScore(){
		return this.similarityScores.entrySet().stream()
				.max(Map.Entry.<String,Double>comparingByValue())
				.get()
				.getValue();
	}
	
	
	public void printSimilarities() {
		System.out.println("Similarites for user "+userId);
		similarityScores.entrySet().stream()
		.sorted(Map.Entry.<String, Double>comparingByValue().reversed())
		.forEach(System.out::println);
	}
	
	public Map<String,Double> getSimilarities(){
		return this.similarityScores;
	}

	
	public void normalizeSimilarityScores() {
		double min = 0;
		double max = this.getMaxSimilarityScore();
		double newmax = 1;
		double newmin = 0;
		System.out.println("User "+this.userId+" max SeqScore "+max);
		for(Double score : similarityScores.values()){
			score = ((score - min)/(max-min))*(newmax-newmin)+newmin;
		}
	}

}
