package pt.utl.ist.meic.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import pt.utl.ist.meic.firebase.models.Event;
import pt.utl.ist.meic.firebase.models.EventCategory;
import pt.utl.ist.meic.utility.FileManager;

import java.util.Optional;
import java.util.stream.Collectors;

public class UserProfile {
	public String userId;
	public String username;
	public boolean crossings;
	public double avgPrecision;

	private Graph mGraph;
	private Map<Integer, Map<String,Double>> numClustersSimScore;
	private Map<String, Double> multiLayerSimilarityScores;
	private Map<String, Double> similarityScoresCrossings;
	private Map<EventCategory, Double> eventPercentages;

	private List<Event> mEvents;
	private List<String> realFriends;

	public UserProfile(String userId) {
		this.userId = userId;
		this.avgPrecision = 0;
		this.crossings = false;
		this.numClustersSimScore = new HashMap<Integer, Map<String,Double>>();
		this.similarityScoresCrossings = new HashMap<String, Double>();
		this.multiLayerSimilarityScores = new HashMap<String, Double>();
		this.mEvents = new ArrayList<Event>();
		this.realFriends = new ArrayList<String>();
		eventPercentages = new HashMap<EventCategory, Double>() {
			{
				for (int i = 0; i < EventCategory.values().length; i++) {
					this.put(EventCategory.values()[i], 0d);
				}
			}
		};
	}
	
	public void calculateMultiLayerScore() {		
		for(Map.Entry<Integer, Map<String,Double>> entry : numClustersSimScore.entrySet()) {
			double factor = Math.pow(2, entry.getKey());
			//iterate scores in this layer
			for(Map.Entry<String, Double> scores : entry.getValue().entrySet()) {
				String user = scores.getKey();
				if(multiLayerSimilarityScores.containsKey(user)) {
					double prevScore = multiLayerSimilarityScores.get(user);
					double newScore = factor * scores.getValue();
					multiLayerSimilarityScores.put(user,prevScore + newScore);
				}else {
					double newScore = factor * scores.getValue();
					multiLayerSimilarityScores.put(user,newScore);
				}
			}
		}
		
		//normalize [0,1]
		double sumFactors = 0d;
		for(Integer numCluster : numClustersSimScore.keySet()) {
			sumFactors += (Math.pow(2, numCluster));
		}
		
		for(Map.Entry<String, Double> multiScore : multiLayerSimilarityScores.entrySet()) {
			multiScore.setValue(multiScore.getValue() / sumFactors);
		}
	}

	public void addEvent(Event e) {
		this.mEvents.add(e);
	}

	public List<Event> getEvents() {
		return this.mEvents;
	}

	public void calculateEventPercentages() {
		int total = mEvents.size();
		if (total > 0) {
			for (Event e : mEvents) {
				if (eventPercentages.containsKey(e.category)) {
					eventPercentages.put(e.category, eventPercentages.get(e.category) + 1);
				} else {
					eventPercentages.put(e.category, 1d);
				}
			}
			for (Map.Entry<EventCategory, Double> entry : eventPercentages.entrySet()) {
				eventPercentages.put(entry.getKey(), entry.getValue() / total);
			}
		}
	}

	public Map<EventCategory, Double> getEventPercentages() {
		return this.eventPercentages;
	}

	public void setGraph(Graph graph) {
		this.mGraph = graph;
	}
	
	public Graph getGraph() {
		return this.mGraph;
	}

	public void addSimilarityScoreByLayer(int numClusters, String userId, double score) {
		// no simmilarity with yourself
		if (!this.userId.equals(userId)) {
			if(numClustersSimScore.containsKey(numClusters)) {
				Map<String,Double> content = numClustersSimScore.get(numClusters);
				content.put(userId, score);
				numClustersSimScore.put(numClusters, content);
			}else {
				Map<String,Double> map = new HashMap<>();
				map.put(userId, score);
				numClustersSimScore.put(numClusters, map);
			}
		}
	}

	public double getSimilarityScoreByLayer(int numClusters, String userId) {
		
		if (this.numClustersSimScore.get(numClusters).containsKey(userId)) {
			return this.numClustersSimScore.get(numClusters).get(userId);
		}
		return 0d;
	}

	public double getMaxSimilarityScore(int numClusters) {
		Optional<Entry<String, Double>> opt = this.numClustersSimScore.get(numClusters).entrySet().stream()
				.max(Map.Entry.<String, Double>comparingByValue());
		if (opt.isPresent()) {
			return opt.get().getValue();
		} else {
			return 0d;
		}

	}
	
	public void addSimilarityScoreCrossings(String userId, double score) {
		// no simmilarity with yourself
		if (!this.userId.equals(userId)) {
			similarityScoresCrossings.put(userId, score);
		}
	}

	public double getSimilarityScoreCrossings(String userId) {
		if (this.similarityScoresCrossings.containsKey(userId)) {
			return this.similarityScoresCrossings.get(userId);
		}
		return 0d;
	}

	public double getMaxSimilarityScoreCrossings() {
		Optional<Entry<String, Double>> opt = this.similarityScoresCrossings.entrySet().stream()
				.max(Map.Entry.<String, Double>comparingByValue());
		if (opt.isPresent()) {
			return opt.get().getValue();
		} else {
			return 0d;
		}

	}

	public void printSimilaritiesByLayer(int numClusters) {
		System.out.println("Similarites for user " + userId);
		numClustersSimScore.get(numClusters).entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed())
				.forEach(System.out::println);
	}

	public Map<String, Double> getSimilaritiesByLayer(int numClusters) {
		return this.numClustersSimScore.get(numClusters);
	}
	
	public Map<String, Double> getSimilaritiesCrossings() {
		return this.similarityScoresCrossings;
	}

	public void normalizeSimilarityScoresByLayer(double newMin, double newMax, int numClusters) {
		double min = 0;
		double max = this.getMaxSimilarityScore(numClusters);
		if (max > 0) {
			for (Map.Entry<String, Double> entry : numClustersSimScore.get(numClusters).entrySet()) {
				double aux = ((entry.getValue() - min) / (max - min)) * (newMax - newMin) + newMin;
				numClustersSimScore.get(numClusters).put(entry.getKey(), aux);
			}
		}
	}
	
	public void normalizeSimilarityScoresCrossings(int numClusters) {
		double min = 0;
		double max = this.getMaxSimilarityScore(numClusters);
		double newmax = 1;
		double newmin = 0;
		if (max > 0) {
			for (Map.Entry<String, Double> entry : similarityScoresCrossings.entrySet()) {
				double aux = ((entry.getValue() - min) / (max - min)) * (newmax - newmin) + newmin;
				similarityScoresCrossings.put(entry.getKey(), aux);
			}
		}
	}

	public void setAveragePrecision(double avgPrecision) {
		this.avgPrecision = avgPrecision;
	}

	public double getAveragePrecision() {
		return this.avgPrecision;
	}

	//TODO: remove!!!
	public void setRealFriendsList(List<String> realFriends){
		this.realFriends = realFriends;
	}
	
	public void loadRealFriendsFromGowalla(FileManager fm) {
		try {
			this.realFriends = fm.getRealFriendsFromGowalla(userId);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public long getNumberSuggestedFriends(double threshold, int numClusters) {
		return this.numClustersSimScore.get(numClusters).entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed())
				.filter(x -> x.getValue() > threshold).count();
	}

	public int getNumberRealFriends() {
		return this.realFriends.size();
	}

	public List<String> getRealFriendsList() {
		return this.realFriends;
	}

	public List<String> getSuggestedFriendsListMultiLayer(double threshold) {
		return this.multiLayerSimilarityScores.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed())
				.filter(x -> x.getValue() > threshold).map(x -> x.getKey()).collect(Collectors.toList());
	}
	
	public List<String> getSuggestedFriendsList(double threshold, int numClusters) {
		return this.numClustersSimScore.get(numClusters).entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed())
				.filter(x -> x.getValue() > threshold).map(x -> x.getKey()).collect(Collectors.toList());
	}
	
	public List<String> getTotalFoundListMultiLayer(double threshold){
		List<String> found = new ArrayList<>(getSuggestedFriendsListMultiLayer(threshold));
		found.retainAll(realFriends);
		return found;
	}
	
	public List<String> getTotalFoundList(double threshold, int numClusters){
		List<String> found = new ArrayList<>(getSuggestedFriendsList(threshold, numClusters));
		found.retainAll(realFriends);
		return found;
	}
	
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserProfile other = (UserProfile) obj;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

}
