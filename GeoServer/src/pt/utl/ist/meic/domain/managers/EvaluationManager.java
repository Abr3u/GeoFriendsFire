package pt.utl.ist.meic.domain.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import pt.utl.ist.meic.domain.UserProfile;
import pt.utl.ist.meic.firebase.models.User;

public class EvaluationManager {

	private Double mrr;
	private Double map;
	private Double recall;
	private Double precision;

	double SIMILARITY_THRESHOLD;
	int NUM_CLUSTERS;
	private List<UserProfile> profiles;

	public EvaluationManager(List<UserProfile> profiles, double threshold, int numClusters) {
		this.profiles = profiles;
		this.NUM_CLUSTERS = numClusters;
		this.SIMILARITY_THRESHOLD = threshold;
	}

	public void evaluateResults() {
		calculatePrecision(profiles, NUM_CLUSTERS);
		calculateRecall(profiles, NUM_CLUSTERS);
		calculateMAP(profiles, NUM_CLUSTERS);
		calculateMRR(profiles, NUM_CLUSTERS);

	}

	/*
	 * public double getMRR() { if (this.mrr == null) { calculateMRR(this.profiles);
	 * } return this.mrr; }
	 * 
	 * public Double getMAP() { if (this.map == null) { calculateMAP(this.profiles);
	 * } return this.map; }
	 * 
	 * public Double getPrecision() { if (this.precision == null) {
	 * calculatePrecision(this.profiles); } return this.precision; }
	 * 
	 * public Double getRecall() { if (this.recall == null) {
	 * calculateRecall(this.profiles); } return this.recall; }
	 */
	private Double calculateMRR(List<UserProfile> profiles, int numClusters) {

		double sumFirstFound = 0;
		List<String> suggested = new ArrayList<String>();
		List<String> realFriends = new ArrayList<String>();

		for (UserProfile profile : profiles) {
			double firstFound = 0;
			realFriends = profile.getRealFriendsList();

			if (numClusters == 0) {
				suggested = profile.getSuggestedFriendsListMultiLayer(SIMILARITY_THRESHOLD).stream().limit(10)
						.collect(Collectors.toList());
			} else {
				suggested = profile.getSuggestedFriendsList(SIMILARITY_THRESHOLD, numClusters).stream().limit(10)
						.collect(Collectors.toList());
			}

			for (int i = 0; i < suggested.size(); i++) {
				if (realFriends.contains(suggested.get(i))) {
					firstFound = 1.0 / (i + 1.0);
					sumFirstFound += firstFound;
					break;
				}
			}
		}

		Double mrr = sumFirstFound / profiles.size();
		this.mrr = mrr;
		System.out.println("MRR  " + mrr);
		return mrr;

	}

	private double calculateMAP(List<UserProfile> profiles, int numClusters) {
		List<String> top10 = new ArrayList<>();
		List<String> realFriends = new ArrayList<>();

		for (UserProfile profile : profiles) {
			double found = 0;
			double sumPrecisions = 0;
			realFriends = profile.getRealFriendsList();

			if (numClusters == 0) {
				top10 = profile.getSuggestedFriendsListMultiLayer(SIMILARITY_THRESHOLD).stream().limit(10)
						.collect(Collectors.toList());
			} else {
				top10 = profile.getSuggestedFriendsList(SIMILARITY_THRESHOLD, numClusters).stream().limit(10)
						.collect(Collectors.toList());
			}

			for (int i = 0; i < top10.size(); i++) {
				if (realFriends.contains(top10.get(i))) {
					found++;
					double precision = found / (i + 1);
					sumPrecisions += precision;
				}
			}
			if (found == 0) {
				profile.setAveragePrecision(0);
			} else {
				double avgPrecision = sumPrecisions / found;
				profile.setAveragePrecision(avgPrecision);
			}
		}

		Double map = profiles.stream().mapToDouble(UserProfile::getAveragePrecision).sum() / profiles.size();
		this.map = map;
		System.out.println("MAP " + map);
		return map;
	}

	private double calculateRecall(List<UserProfile> profiles, int numClusters) {
		double totalFound = 0;
		double totalFriends = 0;
		for (UserProfile up : profiles) {
			if (numClusters == 0) {
				totalFound += up.getTotalFoundListMultiLayer(SIMILARITY_THRESHOLD).size();
			} else {
				totalFound += up.getTotalFoundList(SIMILARITY_THRESHOLD, numClusters).size();
			}
			totalFriends += up.getRealFriendsList().size();
		}
		Double recall = totalFound / totalFriends;
		this.recall = recall;
		System.out.println("Recall " + recall);
		return recall;
	}

	private double calculatePrecision(List<UserProfile> profiles, int numClusters) {
		double totalFound = 0;
		double totalSuggested = 0;
		for (UserProfile up : profiles) {
			if (numClusters == 0) {
				totalFound += up.getTotalFoundListMultiLayer(SIMILARITY_THRESHOLD).size();
				totalSuggested += up.getSuggestedFriendsListMultiLayer(SIMILARITY_THRESHOLD).size();
			} else {
				totalFound += up.getTotalFoundList(SIMILARITY_THRESHOLD, numClusters).size();
				totalSuggested += up.getSuggestedFriendsList(SIMILARITY_THRESHOLD, numClusters).size();
			}
		}
		Double precision = totalFound / totalSuggested;
		this.precision = precision;
		System.out.println("Precision " + precision);
		return precision;
	}

}
