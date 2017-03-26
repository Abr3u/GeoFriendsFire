package pt.utl.ist.meic.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

public class UserProfile {

	private Map<String, Graph> mGraphs;
	public String userId;
	public double avgPrecision;

	private Map<String, Double> similarityScores;

	public UserProfile(String userId) {
		this.userId = userId;
		this.avgPrecision = 0;
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
		if(this.similarityScores.containsKey(userId)){
			return this.similarityScores.get(userId);
		}
		return 0d;
	}
	
	public double getMaxSimilarityScore(){
		Optional<Entry<String, Double>> opt =  this.similarityScores.entrySet().stream()
				.max(Map.Entry.<String,Double>comparingByValue());
		if(opt.isPresent()){
			return opt.get().getValue();
		}else{
			return 0d;
		}
		
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
		if(max > 0){
			for(Map.Entry<String, Double> entry : similarityScores.entrySet()){
				double aux = ((entry.getValue() - min)/(max-min))*(newmax-newmin)+newmin;
				similarityScores.put(entry.getKey(), aux);
			}
		}
	}

	public void setAveragePrecision(double avgPrecision){
		this.avgPrecision = avgPrecision;
	}
	
	public double getAveragePrecision(){
		return this.avgPrecision;
	}
	
}
