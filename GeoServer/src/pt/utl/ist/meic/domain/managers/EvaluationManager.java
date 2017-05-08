package pt.utl.ist.meic.domain.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import pt.utl.ist.meic.domain.UserProfile;

public class EvaluationManager {
	
	double SIMILARITY_THRESHOLD;
	private List<UserProfile> profiles;

	public EvaluationManager(List<UserProfile> profiles,double threshold) {
		this.profiles = profiles;
		this.SIMILARITY_THRESHOLD = threshold;
	}
	
	public void evaluateResults(){
		calculatePrecision(profiles);
		calculateRecall(profiles);
		calculateMAP(profiles);
		calculateNDCG(profiles);
	}
	
	private void calculateNDCG(List<UserProfile> profiles) {
		// TODO Auto-generated method stub

	}

	private void calculateMAP(List<UserProfile> profiles) {
		List<String> top10 = new ArrayList<>();
		List<String> realFriends = new ArrayList<>();
		List<Double> precisions = new ArrayList<>();

		for (UserProfile profile : profiles) {
			double found = 0;
			precisions = new ArrayList<Double>();
			realFriends = profile.getRealFriendsList();
			top10 = profile.getSuggestedFriendsList(SIMILARITY_THRESHOLD).stream().limit(10)
					.collect(Collectors.toList());
			for (int i = 0; i < top10.size(); i++) {
				if (realFriends.contains(top10.get(i))) {
					found++;
					double precision = found / (i + 1);
					precisions.add(precision);
				}
			}
			if (found == 0) {
				profile.setAveragePrecision(0);
			} else {
				double avgPrecision = precisions.stream().reduce(0.0, Double::sum) / precisions.size();
				profile.setAveragePrecision(avgPrecision);
			}
		}
		System.out.println("MAP " + profiles.stream().mapToDouble(UserProfile::getAveragePrecision).sum()
				/ profiles.size());
	}

	private void calculateRecall(List<UserProfile> profiles) {
		double totalFound = 0;
		double totalFriends = 0;
		for(UserProfile up : profiles){
			totalFound += up.getTotalFoundList(SIMILARITY_THRESHOLD).size();
			totalFriends += up.getRealFriendsList().size();
		}
		double recall = totalFound / totalFriends;
		System.out.println("Recall " + recall);
	}

	private void calculatePrecision(List<UserProfile> profiles) {
		double totalFound = 0;
		double totalSuggested = 0;
		for(UserProfile up : profiles){
			totalFound += up.getTotalFoundList(SIMILARITY_THRESHOLD).size();
			totalSuggested += up.getSuggestedFriendsList(SIMILARITY_THRESHOLD).size();
		}
		double precision = totalFound / totalSuggested;
		System.out.println("Precision " + precision);
	}

	
}
