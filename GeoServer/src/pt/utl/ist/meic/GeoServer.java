package pt.utl.ist.meic;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ca.pfv.spmf.algorithms.clustering.distanceFunctions.DistanceCosine;
import ca.pfv.spmf.algorithms.clustering.distanceFunctions.DistanceFunction;
import ca.pfv.spmf.algorithms.clustering.kmeans.AlgoKMeans;
import ca.pfv.spmf.patterns.cluster.ClusterWithMean;
import ca.pfv.spmf.patterns.cluster.DoubleArray;
import net.thegreshams.firebase4j.error.FirebaseException;
import net.thegreshams.firebase4j.error.JacksonUtilityException;
import pt.utl.ist.meic.domain.AuxiliarVertex;
import pt.utl.ist.meic.domain.CheckIn;
import pt.utl.ist.meic.domain.Graph;
import pt.utl.ist.meic.domain.Sequence;
import pt.utl.ist.meic.domain.SequenceAuxiliar;
import pt.utl.ist.meic.domain.UserProfile;
import pt.utl.ist.meic.domain.VertexInfo;
import pt.utl.ist.meic.domain.managers.CheckInsManager;
import pt.utl.ist.meic.domain.managers.SimilarityManager;
import pt.utl.ist.meic.domain.managers.UserProfilesManager;
import pt.utl.ist.meic.firebase.FirebaseHelper;
import pt.utl.ist.meic.firebase.models.Event;
import pt.utl.ist.meic.firebase.models.EventCategory;
import pt.utl.ist.meic.firebase.models.User;
import pt.utl.ist.meic.utility.FileManager;

public class GeoServer {

	private static final String pathKmeansNewYorkCSV = "C:/Android/GeoFriendsFire/GeoServer/dataset/newYork.csv";
	private static String DELIMITER = ";";

	private static final int LEVEL = 0;// clusterLevel
	private static final int NUM_CLUSTERS = 5;// kmeans

	private static final int COMPARING_DISTANCE_THRESHOLD = 50000;// meters

	private static final int MATCHING_MAX_SEQ_LENGTH = 20;// analisar seqs no
															// maximo de 20
															// clusters
	private static final long TRANSITION_TIME_THRESHOLD = 2 * 60 * 60 * 1000;// 2
																				// horas

	private static final double ACT_SCORE_WEIGHT = 0.75;
	private static final double SEQ_SCORE_WEIGHT = 0.25;
	private static final double SIMILARITY_THRESHOLD = 0.2;

	// workflow flags
	private static final boolean FIREBASE = false;
	private static final boolean CLUSTERING_WORKFLOW = false;
	private static final boolean EVENT_SIMILARITY_WORKFLOW = false;

	private static long mTotalCheckIns = 130425;// newYork CI's

	public static void main(String[] args) {

		FileManager mFileManager = new FileManager();
		Map<String, UserProfile> id_userProfile = new HashMap<>();
		Map<Integer, List<ClusterWithMean>> level_clusters_map = new HashMap<>();

		if (CLUSTERING_WORKFLOW) {
			try {
				level_clusters_map = applyKmeans(pathKmeansNewYorkCSV, DELIMITER);
				FirebaseHelper.writeNewClustersFirebase(level_clusters_map.get(LEVEL), LEVEL, mTotalCheckIns);
			} catch (FirebaseException e) {
				e.printStackTrace();
			} catch (JacksonUtilityException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		level_clusters_map = populateLevelClustersMap();
		UserProfilesManager userProfilesManager = new UserProfilesManager(mFileManager, FIREBASE);
		id_userProfile = userProfilesManager.createUserProfiles();

		if (EVENT_SIMILARITY_WORKFLOW) {
			try {
				id_userProfile = FirebaseHelper.populateUserEventsFromFirebase(id_userProfile);
				SimilarityManager similarityManager = new SimilarityManager(id_userProfile, LEVEL,
						COMPARING_DISTANCE_THRESHOLD, MATCHING_MAX_SEQ_LENGTH, TRANSITION_TIME_THRESHOLD,
						SEQ_SCORE_WEIGHT, ACT_SCORE_WEIGHT);
				id_userProfile = similarityManager.calculateSimilaritiesFromEvents();
			} catch (FirebaseException | UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		} else {
			CheckInsManager checkInsManager = new CheckInsManager(mFileManager, id_userProfile, level_clusters_map,
					FIREBASE, LEVEL, NUM_CLUSTERS);
			id_userProfile = checkInsManager.populateUsersCheckIns();

			SimilarityManager similarityManager = new SimilarityManager(id_userProfile, LEVEL,
					COMPARING_DISTANCE_THRESHOLD, MATCHING_MAX_SEQ_LENGTH, TRANSITION_TIME_THRESHOLD, SEQ_SCORE_WEIGHT,
					ACT_SCORE_WEIGHT);
			id_userProfile = similarityManager.calculateSimilaritiesFromLocations();
		}

		int totalUsers = id_userProfile.size();
		String friendsPath = "friendsOf" + totalUsers + "Users.csv";
		String foundPath = "foundOf" + totalUsers + "Users.csv";
		String foundPrctPath = "foundPRCTOf" + totalUsers + "Users.csv";
		List<UserProfile> profiles = new ArrayList<>(id_userProfile.values());

		//writeResultsFirebase(profiles);
		writeResultsLocalStorage(mFileManager, profiles, friendsPath, foundPath, foundPrctPath);
		evaluateResults(mFileManager, friendsPath, foundPath);

	}

	private static void writeResultsFirebase(List<UserProfile> profiles) {
		try {
			FirebaseHelper.writeNewFriendsFirebase(profiles,true, 5, 10);//limited so we dont surpass firebase quotas!!
		} catch (FirebaseException | JacksonUtilityException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	private static void writeResultsLocalStorage(FileManager mFileManager, List<UserProfile> profiles,
			String friendsPath, String foundPath, String foundPrctPath) {
		try {
			mFileManager.createCsvSimilarities(profiles, friendsPath, SIMILARITY_THRESHOLD);
			mFileManager.createFoundCSV(friendsPath, foundPath);
			mFileManager.createFoundPrctCSV(foundPath, foundPrctPath);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void evaluateResults(FileManager mFileManager, String friendsPath, String foundPath) {
		try {
			System.out.println("Precision " + mFileManager.calculatePrecision(friendsPath, foundPath));
			System.out.println("Recall " + mFileManager.calculateRecall(foundPath));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// id_userProfile.values().stream().forEach(x -> {
		// try {
		// mFileManager.calculateAveragePrecision(x, friendsPath);
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// });
		// System.out.println(
		// "MAP " +
		// id_userProfile.values().stream().mapToDouble(UserProfile::getAveragePrecision).sum()
		// / id_userProfile.values().size());
		//
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
