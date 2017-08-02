package test;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import pt.utl.ist.meic.domain.UserProfile;
import pt.utl.ist.meic.domain.managers.EvaluationManager;

public class TestMeasures {

	/*
	public static void testAllMeasures(){
		testPrecision();
		testRecall();
		testMAP();
		testMRR();
	}
	
	public static void testMRR() {

		final double EXPECTED = 11.0 / 18.0;
		final double SIMILARITY_THRESHOLD = 0.5;

		// START setup
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

		List<String> realFriends0 = new ArrayList<String>() {
			{
				this.add("33333");
				this.add("44444");
				this.add("3");
			}
		};
		List<String> realFriends1 = new ArrayList<String>() {
			{
				this.add("33333");
				this.add("3");
				this.add("44444");
			}
		};
		List<String> realFriends2 = new ArrayList<String>() {
			{
				this.add("3");
				this.add("44444");
				this.add("3333");
			}
		};

		u0.setRealFriendsList(realFriends0);
		u1.setRealFriendsList(realFriends1);
		u2.setRealFriendsList(realFriends2);

		List<UserProfile> profiles = new ArrayList<UserProfile>() {
			{
				this.add(u0);
				this.add(u1);
				this.add(u2);
			}
		};

		// END setup

		EvaluationManager manager = new EvaluationManager(profiles, SIMILARITY_THRESHOLD);
		DecimalFormat df = new DecimalFormat("#.####");
		if (!df.format(EXPECTED).equals(df.format(manager.getMRR()))) {
			System.out.println("--Test failed");
		} else {
			System.out.println("--Test passed");
		}
	}
	
	public static void testMAP() {

		final double EXPECTED = 19.0 / 36.0;
		final double SIMILARITY_THRESHOLD = 0.5;

		// START setup
		UserProfile u0 = new UserProfile("0");
		UserProfile u1 = new UserProfile("1");
		UserProfile u2 = new UserProfile("2");

		//map = (1+(1/2))/2 = 3/4
		u0.addSimilarityScore("3", 0.9);
		u0.addSimilarityScore("1", 0.8);
		u0.addSimilarityScore("2", 0.7);
		u0.addSimilarityScore("99999", 0.6);

		//map = 1/2
		u1.addSimilarityScore("0", 0.9);
		u1.addSimilarityScore("3", 0.8);
		u1.addSimilarityScore("2", 0.7);

		//map = 1/3
		u2.addSimilarityScore("0", 0.9);
		u2.addSimilarityScore("1", 0.8);
		u2.addSimilarityScore("3", 0.7);

		List<String> realFriends0 = new ArrayList<String>() {
			{
				this.add("3");
				this.add("44444");
				this.add("99999");
			}
		};
		List<String> realFriends1 = new ArrayList<String>() {
			{
				this.add("33333");
				this.add("3");
				this.add("44444");
			}
		};
		List<String> realFriends2 = new ArrayList<String>() {
			{
				this.add("3");
				this.add("44444");
				this.add("33333");
			}
		};

		u0.setRealFriendsList(realFriends0);
		u1.setRealFriendsList(realFriends1);
		u2.setRealFriendsList(realFriends2);

		List<UserProfile> profiles = new ArrayList<UserProfile>() {
			{
				this.add(u0);
				this.add(u1);
				this.add(u2);
			}
		};

		// END setup

		EvaluationManager manager = new EvaluationManager(profiles, SIMILARITY_THRESHOLD);
		DecimalFormat df = new DecimalFormat("#.####");
		if (!df.format(EXPECTED).equals(df.format(manager.getMAP()))) {
			System.out.println("--Test failed");
		} else {
			System.out.println("--Test passed");
		}
	}

	public static void testPrecision() {

		final double EXPECTED = 1.0 / 3.0;
		final double SIMILARITY_THRESHOLD = 0.5;

		// START setup
		UserProfile u0 = new UserProfile("0");
		UserProfile u1 = new UserProfile("1");
		UserProfile u2 = new UserProfile("2");

		// precision = 1/3
		u0.addSimilarityScore("3", 0.9);
		u0.addSimilarityScore("1", 0.8);
		u0.addSimilarityScore("2", 0.7);

		// precision = 0
		u1.addSimilarityScore("0", 0.9);
		u1.addSimilarityScore("9", 0.8);
		u1.addSimilarityScore("2", 0.7);

		// precision = 2/3
		u2.addSimilarityScore("0", 0.9);
		u2.addSimilarityScore("44444", 0.8);
		u2.addSimilarityScore("3", 0.7);

		List<String> realFriends0 = new ArrayList<String>() {
			{
				this.add("33333");
				this.add("44444");
				this.add("3");
			}
		};
		List<String> realFriends1 = new ArrayList<String>() {
			{
				this.add("33333");
				this.add("3");
				this.add("44444");
			}
		};
		List<String> realFriends2 = new ArrayList<String>() {
			{
				this.add("3");
				this.add("44444");
				this.add("33333");
			}
		};

		u0.setRealFriendsList(realFriends0);
		u1.setRealFriendsList(realFriends1);
		u2.setRealFriendsList(realFriends2);

		List<UserProfile> profiles = new ArrayList<UserProfile>() {
			{
				this.add(u0);
				this.add(u1);
				this.add(u2);
			}
		};

		// END setup
		EvaluationManager manager = new EvaluationManager(profiles, SIMILARITY_THRESHOLD);
		DecimalFormat df = new DecimalFormat("#.####");
		if (!df.format(EXPECTED).equals(df.format(manager.getPrecision()))) {
			System.out.println("--Test failed");
		} else {
			System.out.println("--Test passed");
		}
	}

	public static void testRecall() {

		final double EXPECTED = 3.0/10.0;
		final double SIMILARITY_THRESHOLD = 0.5;

		// START setup
		UserProfile u0 = new UserProfile("0");
		UserProfile u1 = new UserProfile("1");
		UserProfile u2 = new UserProfile("2");

		// recall = 1/3
		u0.addSimilarityScore("3", 0.9);
		u0.addSimilarityScore("1", 0.8);
		u0.addSimilarityScore("2", 0.7);

		// recall = 0
		u1.addSimilarityScore("0", 0.9);
		u1.addSimilarityScore("9", 0.8);
		u1.addSimilarityScore("2", 0.7);

		// recall = 2/4
		u2.addSimilarityScore("0", 0.9);
		u2.addSimilarityScore("4", 0.8);
		u2.addSimilarityScore("3", 0.7);

		List<String> realFriends0 = new ArrayList<String>() {
			{
				this.add("33333");
				this.add("44444");
				this.add("3");
			}
		};
		List<String> realFriends1 = new ArrayList<String>() {
			{
				this.add("33333");
				this.add("3");
				this.add("44444");
			}
		};
		List<String> realFriends2 = new ArrayList<String>() {
			{
				this.add("3");
				this.add("4");
				this.add("33333");
				this.add("9999");
			}
		};

		u0.setRealFriendsList(realFriends0);
		u1.setRealFriendsList(realFriends1);
		u2.setRealFriendsList(realFriends2);

		List<UserProfile> profiles = new ArrayList<UserProfile>() {
			{
				this.add(u0);
				this.add(u1);
				this.add(u2);
			}
		};

		// END setup
		EvaluationManager manager = new EvaluationManager(profiles, SIMILARITY_THRESHOLD);
		DecimalFormat df = new DecimalFormat("#.####");
		if (!df.format(EXPECTED).equals(df.format(manager.getRecall()))) {
			System.out.println("--Test failed");
		} else {
			System.out.println("--Test passed");
		}
	}
*/
	
	
}
