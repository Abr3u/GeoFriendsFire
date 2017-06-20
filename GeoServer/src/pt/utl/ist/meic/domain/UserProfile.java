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
import pt.utl.ist.meic.utility.FileManagerAMS;

import java.util.Optional;
import java.util.stream.Collectors;

public class UserProfile {
	public String userId;
	public String username;
	public double avgPrecision;

	private Map<Integer, Graph> mGraphs;
	private Map<String, Double> similarityScores;
	private Map<EventCategory, Double> eventPercentages;

	private List<Event> mEvents;
	private List<String> realFriends;
	
	private int totalFound;

	public UserProfile(String userId) {
		this.userId = userId;
		this.avgPrecision = 0;
		this.totalFound = 0;
		this.mGraphs = new HashMap<Integer, Graph>();
		this.similarityScores = new HashMap<String, Double>();
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

	public void addNewGraph(int level, Graph graph) {
		this.mGraphs.put(level, graph);
	}

	public Graph getGraphByLevel(int level) {
		return this.mGraphs.get(level);
	}

	public void addSimilarityScore(String userId, double score) {
		// no simmilarity with yourself
		if (!this.userId.equals(userId)) {
			similarityScores.put(userId, score);
		}
	}

	public double getSimilarityScore(String userId) {
		if (this.similarityScores.containsKey(userId)) {
			return this.similarityScores.get(userId);
		}
		return 0d;
	}

	public double getMaxSimilarityScore() {
		Optional<Entry<String, Double>> opt = this.similarityScores.entrySet().stream()
				.max(Map.Entry.<String, Double>comparingByValue());
		if (opt.isPresent()) {
			return opt.get().getValue();
		} else {
			return 0d;
		}

	}

	public void printSimilarities() {
		System.out.println("Similarites for user " + userId);
		similarityScores.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed())
				.forEach(System.out::println);
	}

	public Map<String, Double> getSimilarities() {
		return this.similarityScores;
	}

	public void normalizeSimilarityScores() {
		double min = 0;
		double max = this.getMaxSimilarityScore();
		double newmax = 1;
		double newmin = 0;
		if (max > 0) {
			for (Map.Entry<String, Double> entry : similarityScores.entrySet()) {
				double aux = ((entry.getValue() - min) / (max - min)) * (newmax - newmin) + newmin;
				similarityScores.put(entry.getKey(), aux);
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
	
	public void loadRealFriendsFromGowalla(FileManagerAMS fm) {
		try {
			this.realFriends = fm.getRealFriendsFromGowalla(userId);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public long getNumberSuggestedFriends(double threshold) {
		return this.similarityScores.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed())
				.filter(x -> x.getValue() > threshold).count();
	}

	public int getNumberRealFriends() {
		return this.realFriends.size();
	}

	public List<String> getRealFriendsList() {
		return this.realFriends;
	}

	public List<String> getSuggestedFriendsList(double threshold) {
		return this.similarityScores.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed())
				.filter(x -> x.getValue() > threshold).map(x -> x.getKey()).collect(Collectors.toList());
	}
	
	public List<String> getTotalFoundList(double threshold){
		List<String> found = new ArrayList<>(getSuggestedFriendsList(threshold));
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
