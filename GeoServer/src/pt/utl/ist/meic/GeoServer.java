package pt.utl.ist.meic;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import ca.pfv.spmf.algorithms.clustering.dbscan.AlgoDBSCAN;
import ca.pfv.spmf.algorithms.clustering.distanceFunctions.DistanceCosine;
import ca.pfv.spmf.algorithms.clustering.distanceFunctions.DistanceFunction;
import ca.pfv.spmf.algorithms.clustering.kmeans.AlgoKMeans;
import ca.pfv.spmf.algorithms.clustering.optics.AlgoOPTICS;
import ca.pfv.spmf.patterns.cluster.Cluster;
import ca.pfv.spmf.patterns.cluster.ClusterWithMean;
import ca.pfv.spmf.patterns.cluster.DoubleArray;
import net.thegreshams.firebase4j.error.FirebaseException;
import net.thegreshams.firebase4j.error.JacksonUtilityException;
import pt.utl.ist.meic.domain.UserProfile;
import pt.utl.ist.meic.domain.managers.CheckInsManager;
import pt.utl.ist.meic.domain.managers.EvaluationManager;
import pt.utl.ist.meic.domain.managers.SimilarityManager;
import pt.utl.ist.meic.domain.managers.UserProfilesManager;
import pt.utl.ist.meic.firebase.FirebaseHelper;
import pt.utl.ist.meic.firebase.models.ScalabilityMetrics;
import pt.utl.ist.meic.firebase.models.UXMetrics;
import pt.utl.ist.meic.utility.FileManager;
import test.TestMeasures;

public class GeoServer {

	private static final String pathKmeansNewYorkCSV = "C:/Android/GeoFriendsFire/GeoServer/dataset/newYorkCluster.csv";
	private static final String pathKmeansFirebaseCSV = "C:/Android/GeoFriendsFire/GeoServer/dataset/firebase.csv";
	private static String DELIMITER = ",";
	private static final int LEVEL = 2;// clusterLevel
	private static long mTotalCheckIns = 130425;// newYork CI's

	// clustering Kmeans
	private static final int NUM_CLUSTERS = 2;// kmeans
	private static final long NEW_DATA_THRESHOLD = 1000;// new locations
	private static final long TIME_PASSED_THRESHOLD = 1000 * 60 * 60 * 24 * 30; // 30
																				// days

	// simmilarity
	private static final int COMPARING_DISTANCE_THRESHOLD = 50000;// meters

	private static final int MATCHING_MAX_SEQ_LENGTH = 20;// analisar seqs no
															// maximo de 20
															// clusters
	private static final long TRANSITION_TIME_THRESHOLD = 2 * 60 * 60 * 1000;// 2
																				// horas

	private static final double ACT_SCORE_WEIGHT = 0.75;
	private static final double SEQ_SCORE_WEIGHT = 0.25;
	private static final double SIMILARITY_THRESHOLD = 0.5;

	// workflow flags
	private static final boolean CLUSTER_GOWALLA = false;
	private static final boolean CLUSTER_FIREBASE = false;
	private static final boolean EVALUATE_GOWALLA = false;
	private static final boolean EVALUATE_EVENTS = false;
	

	private static final boolean REAL_VERSION = true;
	private static final boolean EVALUATE_SCALABILITY_TRAJ_SIZE = true;
	private static final int TRAJ_SIZE = 40;
	
	private static final boolean EVALUATE_SCALABILITY_NUM_EVENTS = true;
	private static final boolean EVALUATE_UX = true;//uses num_events
	private static final int NUM_EVENTS = 50;
	
	private static final boolean TEST = false;
	private static boolean FIREBASE;

	// debug
	private static final long abril = 1491001200000l;// milisecs 1 de abril 2017

	public static void main(String[] args) {
		double initTime = System.currentTimeMillis();
		String summary = "-----SUMMARY-----\n";

		summary += (CLUSTER_GOWALLA) ? "cluster Gowalla\n":"";
		summary += (CLUSTER_FIREBASE) ? "cluster Firebase\n":"";
		summary += (EVALUATE_EVENTS) ? "evaluate Events\n":"";
		
		summary += (EVALUATE_GOWALLA) ? "evaluate Gowalla\n":"";
		summary += (EVALUATE_GOWALLA) ? "actScore " + ACT_SCORE_WEIGHT + " // seqScore " + SEQ_SCORE_WEIGHT+"\n":"";
		summary += (EVALUATE_GOWALLA) ? "level " + LEVEL + " // threshold " + SIMILARITY_THRESHOLD+"\n":"";
		
		summary += (EVALUATE_SCALABILITY_NUM_EVENTS) ? "evaluate scalability NumEvents "+NUM_EVENTS+"\n":"";
		summary += (EVALUATE_SCALABILITY_TRAJ_SIZE) ? "evaluate scalability TrajSize "+TRAJ_SIZE+"\n":"";
		summary += (EVALUATE_UX) ? "evaluate UX numEvents "+NUM_EVENTS+"\n":"";
		summary += (REAL_VERSION) ? "REAL version":"BAD version";

		FileManager mFileManager = new FileManager();
		Map<String, UserProfile> id_userProfile = new HashMap<>();
		Map<Integer, List<ClusterWithMean>> level_clusters_map = new HashMap<>();

		if (TEST) {
			TestMeasures.testAllMeasures();
			return;
		}

		if (EVALUATE_SCALABILITY_TRAJ_SIZE) {
			try {				
				String info = "------>>>>Evaluating Scalability, ";
				info += (REAL_VERSION) ? "REAL version, " : "BAD version, ";
				info += "with trajSize of " + TRAJ_SIZE;
				
				List<ScalabilityMetrics> metricsTrajSize = FirebaseHelper
						.getScalabilityMetricsTrajSizeFromFirebase(REAL_VERSION, TRAJ_SIZE);
				OptionalDouble averageBytesSpentTrajSize = metricsTrajSize.stream().map(x -> x.bytesSpent)
						.mapToLong(x -> x).average();
				OptionalDouble averageUpdatesTrajSize = metricsTrajSize.stream().map(x -> x.updates).mapToInt(x -> x)
						.average();

				System.out.println(info);
				System.out.println("Average Bytes Spent " + averageBytesSpentTrajSize.getAsDouble());
				System.out.println("Average Updates " + averageUpdatesTrajSize.getAsDouble());

			} catch (UnsupportedEncodingException | FirebaseException e) {
				e.printStackTrace();
			}
		}

		if (EVALUATE_SCALABILITY_NUM_EVENTS) {
			try {
				String info = "------>>>>>>Evaluating Scalability, ";
				info += (REAL_VERSION) ? "REAL version, " : "BAD version, ";
				info += "with numEvents of " + NUM_EVENTS;

				List<ScalabilityMetrics> metricsNumEvents = FirebaseHelper
						.getScalabilityMetricsNumEventsFromFirebase(REAL_VERSION, NUM_EVENTS);
				OptionalDouble averageBytesSpentNumEvents = metricsNumEvents.stream().map(x -> x.bytesSpent)
						.mapToLong(x -> x).average();
				OptionalDouble averageUpdatesNumEvents = metricsNumEvents.stream().map(x -> x.updates).mapToInt(x -> x)
						.average();

				System.out.println(info);
				System.out.println("Average Bytes Spent " + averageBytesSpentNumEvents.getAsDouble());
				System.out.println("Average Updates " + averageUpdatesNumEvents.getAsDouble());

			} catch (UnsupportedEncodingException | FirebaseException e) {
				e.printStackTrace();
			}
		}

		if (EVALUATE_UX) {
			List<UXMetrics> metrics;
			try {
				metrics = FirebaseHelper.getUXMetricsFromFirebase(NUM_EVENTS);
				OptionalDouble averageTime = metrics.stream().map(x -> x.timeUntilFirst).mapToLong(x -> x).average();

				System.out.println("--------->>>>>>>>Evaluating UX metrics :: numEvents " + NUM_EVENTS);
				System.out.println("Average Time Until First " + averageTime.getAsDouble() + " milisecs");
			} catch (UnsupportedEncodingException | FirebaseException e) {
				e.printStackTrace();
			}
		}

		// use gowalla to evaluate
		else if (EVALUATE_GOWALLA) {
			FIREBASE = false;
			level_clusters_map = populateLevelClustersMap();

			UserProfilesManager userProfilesManager = new UserProfilesManager(mFileManager, FIREBASE);
			id_userProfile = userProfilesManager.createUserProfiles();

			CheckInsManager checkInsManager = new CheckInsManager(mFileManager, id_userProfile, level_clusters_map,
					FIREBASE, LEVEL, NUM_CLUSTERS);
			id_userProfile = checkInsManager.populateUsersCheckIns();

			SimilarityManager similarityManager = new SimilarityManager(id_userProfile, LEVEL, SEQ_SCORE_WEIGHT,
					ACT_SCORE_WEIGHT);
			id_userProfile = similarityManager.calculateSimilaritiesFromLocations(COMPARING_DISTANCE_THRESHOLD,
					MATCHING_MAX_SEQ_LENGTH, TRANSITION_TIME_THRESHOLD);

			List<UserProfile> profiles = new ArrayList<>(id_userProfile.values());

			// writeResultsFirebase(profiles);
			writeResultsLocalStorage(mFileManager, profiles);
			EvaluationManager evaluationManager = new EvaluationManager(profiles, SIMILARITY_THRESHOLD);
			evaluationManager.evaluateResults();
		}

		else if (EVALUATE_EVENTS) {
			FIREBASE = true;
			UserProfilesManager userProfilesManager = new UserProfilesManager(mFileManager, FIREBASE);
			id_userProfile = userProfilesManager.createUserProfiles();

			try {
				id_userProfile = FirebaseHelper.populateUserEventsFromFirebase(id_userProfile);

				SimilarityManager similarityManager = new SimilarityManager(id_userProfile, LEVEL, SEQ_SCORE_WEIGHT,
						ACT_SCORE_WEIGHT);
				id_userProfile = similarityManager.calculateSimilaritiesFromEvents();
			} catch (UnsupportedEncodingException | FirebaseException e) {
				e.printStackTrace();
			}
		}

		// cluster based on gowalla data
		else if (CLUSTER_GOWALLA) {
			FIREBASE = false;
			level_clusters_map = clusterLocationsKMEANS(pathKmeansNewYorkCSV);
		}

		else if (CLUSTER_FIREBASE) {
			FIREBASE = true;
			try {
				// check time passed
				Long lastUpdated = FirebaseHelper.getLastUpdatedFromFirebase();
				Long now = System.currentTimeMillis();

				// check new data
				long storedTotalLocations = FirebaseHelper.getStoredLocationsSizeFromFirebase();
				long currentLotalLocations = FirebaseHelper.getCurrentLocationsSizeFromFirebase();
				if (now - lastUpdated >= TIME_PASSED_THRESHOLD
						|| Math.abs(currentLotalLocations - storedTotalLocations) > NEW_DATA_THRESHOLD) {

					UserProfilesManager userProfilesManager = new UserProfilesManager(mFileManager, FIREBASE);
					id_userProfile = userProfilesManager.createUserProfiles();

					mFileManager.createCSVFirebaseLocations(id_userProfile, pathKmeansFirebaseCSV);

					level_clusters_map = clusterLocationsKMEANS(pathKmeansFirebaseCSV);
				}
			} catch (FirebaseException | IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println(summary);
		double endTime = System.currentTimeMillis();
		double time = (endTime - initTime) / 1000 / 60;
		System.out.println("This task took " + time + " minutes");

	}

	public static void testStuff() {

	}

	private static Map<Integer, List<ClusterWithMean>> clusterLocationsKMEANS(String path) {
		Map<Integer, List<ClusterWithMean>> level_clusters_map = new HashMap<>();

		try {
			level_clusters_map = applyKmeans(path, DELIMITER);
			// FirebaseHelper.writeNewClustersFirebaseKMEANS(level_clusters_map.get(LEVEL),
			// LEVEL, mTotalCheckIns);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return level_clusters_map;
	}

	private static void writeResultsFirebase(List<UserProfile> profiles) {
		try {
			FirebaseHelper.writeNewFriendsFirebase(profiles, true, 5, 10);// limited
																			// so
																			// we
																			// dont
																			// surpass
																			// firebase
																			// quotas!!
		} catch (FirebaseException | JacksonUtilityException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	private static void writeResultsLocalStorage(FileManager mFileManager, List<UserProfile> profiles) {

		int totalUsers = profiles.size();
		String friendsPath = "level" + LEVEL + "friendsOf" + totalUsers + "Users.csv";
		String foundPath = "level" + LEVEL + "foundOf" + totalUsers + "Users.csv";
		String foundPrctPath = "level" + LEVEL + "foundPRCTOf" + totalUsers + "Users.csv";

		try {
			mFileManager.createCsvSimilarities(profiles, friendsPath, SIMILARITY_THRESHOLD);
			mFileManager.createFoundCSV(friendsPath, foundPath);
			mFileManager.createFoundPrctCSV(foundPath, foundPrctPath);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static Map<Integer, List<ClusterWithMean>> populateLevelClustersMap() {
		Map<Integer, List<ClusterWithMean>> level_clusters_map = new HashMap<Integer, List<ClusterWithMean>>();

		List<ClusterWithMean> clusters = new ArrayList<ClusterWithMean>();
		ClusterWithMean cluster = new ClusterWithMean(0);
		// LEVEL 0 START
		cluster.setMean(new DoubleArray(new double[] { 40.68596772385074, -73.99656293456547 }));
		cluster.mId = 0;
		clusters.add(cluster);

		cluster = new ClusterWithMean(0);
		cluster.setMean(new DoubleArray(new double[] { 40.77761934847919, -73.92851136376517 }));
		cluster.mId = 1;
		clusters.add(cluster);

		cluster = new ClusterWithMean(0);
		cluster.setMean(new DoubleArray(new double[] { 40.75076361515444, -73.96725029838117 }));
		cluster.mId = 2;
		clusters.add(cluster);

		cluster = new ClusterWithMean(0);
		cluster.setMean(new DoubleArray(new double[] { 40.72968661755094, -73.99184556681594 }));
		cluster.mId = 3;
		clusters.add(cluster);

		cluster = new ClusterWithMean(0);
		cluster.setMean(new DoubleArray(new double[] { 40.83586092995377, -73.89130396728055 }));
		cluster.mId = 4;
		clusters.add(cluster);

		level_clusters_map.put(0, clusters);
		// LEVEL 0 END

		// LEVEL 1 START
		clusters.clear();
		cluster.setMean(new DoubleArray(new double[] { 40.79996523013644, -73.90179777475407 }));
		cluster.mId = 0;
		clusters.add(cluster);

		cluster = new ClusterWithMean(0);
		cluster.setMean(new DoubleArray(new double[] { 40.75234672984151, -73.96658361000398 }));
		cluster.mId = 1;
		clusters.add(cluster);

		cluster = new ClusterWithMean(0);
		cluster.setMean(new DoubleArray(new double[] { 40.713945673741044, -73.99464156416163 }));
		cluster.mId = 2;
		clusters.add(cluster);

		level_clusters_map.put(1, clusters);
		// LEVEL 1 END

		// LEVEL 2 START
		clusters.clear();
		cluster.setMean(new DoubleArray(new double[] { 40.76490479484749, -73.94928948232359 }));
		cluster.mId = 0;
		clusters.add(cluster);

		cluster = new ClusterWithMean(0);
		cluster.setMean(new DoubleArray(new double[] { 40.721799680165596, -73.99091744437025 }));
		cluster.mId = 1;
		clusters.add(cluster);

		level_clusters_map.put(2, clusters);
		// LEVEL 2 END

		return level_clusters_map;
	}

	private static Map<Integer, List<ClusterWithMean>> applyKmeans(String pathToCSV, String delimiter)
			throws IOException {
		Map<Integer, List<ClusterWithMean>> level_clusters_map = new HashMap<>();
		DistanceFunction distanceFunction = new DistanceCosine();

		// Apply the algorithm
		AlgoKMeans algoKMeans = new AlgoKMeans();
		System.out.println("applying K-means");
		List<ClusterWithMean> clusters = algoKMeans.runAlgorithm(pathToCSV, NUM_CLUSTERS, distanceFunction, delimiter);
		level_clusters_map.put(LEVEL, clusters);
		algoKMeans.printStatistics();

		// Print the clusters found by the algorithm
		// For each cluster:
		int i = 0;
		mTotalCheckIns = 0;
		for (ClusterWithMean cluster : clusters) {
			System.out.println("Cluster " + i++);
			System.out.println("size -> " + cluster.getVectors().size());
			System.out.println("mean -> " + cluster.getmean().toString());
			mTotalCheckIns += cluster.getVectors().size();
		}
		System.out.println("Total CI " + mTotalCheckIns);
		return level_clusters_map;
	}

}
