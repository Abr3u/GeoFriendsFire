package test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import pt.utl.ist.meic.domain.UserProfile;
import pt.utl.ist.meic.domain.managers.EvaluationManager;

public class TestMeasures {

	public static void testMRR(){
		
		final double SIMILARITY_THRESHOLD = 0.5;
		
		UserProfile u0 = new UserProfile("0");
		UserProfile u1 = new UserProfile("1");
		UserProfile u2 = new UserProfile("2");		
		
		u0.addSimilarityScore("3", 0.9);
		u0.addSimilarityScore("1", 0.8);
		u0.addSimilarityScore("2", 0.7);
		
		u1.addSimilarityScore("0", 0.9);
		u1.addSimilarityScore("3", 0.8);
		u1.addSimilarityScore("2", 0.7);
		
		u2.addSimilarityScore("0", 0.9);
		u2.addSimilarityScore("1", 0.8);
		u2.addSimilarityScore("3", 0.7);		
		
		List<String> realFriends0 = new ArrayList<String>(){{
			this.add("33333");
			this.add("44444");
			this.add("3");
		}};
		List<String> realFriends1 = new ArrayList<String>(){{
			this.add("33333");
			this.add("3");
			this.add("44444");
		}};
		List<String> realFriends2 = new ArrayList<String>(){{
			this.add("3");
			this.add("44444");
			this.add("3333");
		}};

		u0.setRealFriendsList(realFriends0);
		u1.setRealFriendsList(realFriends1);
		u2.setRealFriendsList(realFriends2);
		
		
		List<UserProfile> profiles = new ArrayList<UserProfile>(){{
			this.add(u0);
			this.add(u1);
			this.add(u2);
		}};
		
		//END setup
		
		EvaluationManager manager = new EvaluationManager(profiles, SIMILARITY_THRESHOLD);
		double expected = 11.0/18.0;
		if(expected != manager.getMRR()){
			System.out.println("Test failed");
		}else{
			System.out.println("Test passed");
		}
	}
	
}
