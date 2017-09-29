package pt.utl.ist.meic;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

import ca.pfv.spmf.algorithms.clustering.distanceFunctions.DistanceCosine;
import ca.pfv.spmf.algorithms.clustering.distanceFunctions.DistanceFunction;
import ca.pfv.spmf.algorithms.clustering.kmeans.AlgoKMeans;
import ca.pfv.spmf.patterns.cluster.ClusterWithMean;
import net.thegreshams.firebase4j.error.FirebaseException;
import net.thegreshams.firebase4j.error.JacksonUtilityException;
import pt.utl.ist.meic.domain.COLLAB_TYPE;
import pt.utl.ist.meic.domain.UserProfile;
import pt.utl.ist.meic.domain.managers.CheckInsManager;
import pt.utl.ist.meic.domain.managers.EvaluationManager;
import pt.utl.ist.meic.domain.managers.SimilarityManager;
import pt.utl.ist.meic.domain.managers.UserProfilesManager;
import pt.utl.ist.meic.firebase.FirebaseHelper;
import pt.utl.ist.meic.firebase.models.FirebaseCluster;
import pt.utl.ist.meic.firebase.models.ScalabilityMetrics;
import pt.utl.ist.meic.firebase.models.UXMetrics;
import pt.utl.ist.meic.utility.FileManager;
import test.TestSequences;

public class GeoServer {

	private static final String pathKmeansAmsCSV = "C:/Android/GeoFriendsFire/GeoServer/dataset/AMSCluster.csv";
	private static final String pathKmeansFirebaseCSV = "C:/Android/GeoFriendsFire/GeoServer/dataset/firebase.csv";
	private static String DELIMITER = ",";

	// clustering Kmeans
	private static final int NUM_CLUSTERS = 4;// kmeans
	private static final long NEW_DATA_THRESHOLD = 1000;// new locations
	private static final long TIME_PASSED_THRESHOLD = 1000 * 60 * 60 * 24 * 30; // 30
																				// days

	// simmilarity
	private static final int COMPARING_DISTANCE_THRESHOLD = 50000;// meters

	private static final int MATCHING_MAX_SEQ_LENGTH = 20;// analisar seqs no
															// maximo de 20
															// clusters
	private static final long TRANSITION_TIME_THRESHOLD = 210 * 60 * 1000;// 3.5 horas
	private static final long SAME_TIME_DAY_THRESHOLD = 30 * 60 * 1000;// 30 minutos

	// workflow flags
	private static final boolean CLUSTER_GOWALLA = false;
	private static final boolean CLUSTER_FIREBASE = false;
	private static final boolean EVALUATE_EVENTS = false;

	private static final boolean DATA_PREPROCESSING = false;
	
	private static final boolean EVALUATE_BY_COUNT = false;
	private static final boolean EVALUATE_GOWALLA = false;
	private static final boolean EVALUATE_GOWALLA_MULTI_LAYER = false;//NUM_CLUSTERS = 0
	private static final int NUM_LAYERS = 2;
	private static final COLLAB_TYPE COLLABORATION_TYPE = COLLAB_TYPE.cosine;
	private static final double ACT_SCORE_WEIGHT = 0.75;
	private static final double SEQ_SCORE_WEIGHT = 0.25;
	private static final double SIMILARITY_THRESHOLD = 0.5;

	private static final boolean EVALUATE_SCALABILITY_TRAJ_SIZE = false;//uses realVersion
	private static final int TRAJ_SIZE = 50;

	private static final boolean EVALUATE_SCALABILITY_NUM_EVENTS = true;//uses realVersion
	private static final boolean EVALUATE_UX = false;// uses num_events + realVersion
	private static final int NUM_EVENTS = 150;

	private static final boolean REAL_VERSION = false;
	private static final boolean TEST = false;

	// debug
	private static final long abril = 1491001200000l;// milisecs 1 de abril 2017
	
	public static void main(String[] args) {
		double initTime = System.currentTimeMillis();
		String summary = "-----SUMMARY-----\n";

		summary += (DATA_PREPROCESSING) ? "data preprocessing\n" : "";
		summary += (CLUSTER_GOWALLA) ? "cluster Gowalla\n" : "";
		summary += (CLUSTER_FIREBASE) ? "cluster Firebase\n" : "";
		summary += (EVALUATE_EVENTS) ? "evaluate Events\n" : "";

		summary += (EVALUATE_GOWALLA) ? "evaluate Gowalla\n" : "";
		summary += (EVALUATE_GOWALLA) ? "actScore " + ACT_SCORE_WEIGHT + " // seqScore " + SEQ_SCORE_WEIGHT + "\n"
				: "";
		summary += (EVALUATE_GOWALLA) ? "threshold " + SIMILARITY_THRESHOLD + "\n" : "";

		summary += (EVALUATE_SCALABILITY_NUM_EVENTS) ? "evaluate scalability NumEvents " + NUM_EVENTS + "\n" : "";
		summary += (EVALUATE_SCALABILITY_TRAJ_SIZE) ? "evaluate scalability TrajSize " + TRAJ_SIZE + "\n" : "";
		summary += (EVALUATE_UX) ? "evaluate UX numEvents " + NUM_EVENTS + "\n" : "";
		summary += (REAL_VERSION) ? "REAL version" : "BAD version";

		Map<String, UserProfile> id_userProfile = new HashMap<>();
		List<FirebaseCluster> globalFirebaseClusters = new ArrayList<>();
		List<ClusterWithMean> globalClustersWithMean = new ArrayList<>();

		if (TEST) {
			double x8 = ((8d - 0) / (24d - 0)) * (1 - 0) + 0;
			double x24 = ((24d - 0) / (24d - 0)) * (1 - 0) + 0;
			double y24 = ((24d - 0) / (64d - 0)) * (1 - 0) + 0;
			double y64 = ((64d - 0) / (64d - 0)) * (1 - 0) + 0;
			double z8 = ((8d - 0) / (64d - 0)) * (1 - 0) + 0;
			double z64 = ((64d - 0) / (64d - 0)) * (1 - 0) + 0;
			System.out.println(x8 + " ; "+x24);
			System.out.println(y24 + " ; "+y64);
			System.out.println(z8 + " ; "+z64);
//			try {
//				FileManager fm = new FileManager();
//				String pathSuggested = "C:/Android/GeoFriendsFire/GeoServer/friendsOf562Users.csv";
//				System.out.println("Average Friends Gowalla");
//				System.out.println(fm.getAverageRelevantFriendsGowalla());
//				System.out.println("Average Suggested Friends GeoFriends");
//				System.out.println(fm.getAverageSuggestedFriendsGeoFriends(pathSuggested));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
		
		if(DATA_PREPROCESSING) {
			FileManager fileManager = new FileManager();
			
			try {
				//checkIns
				fileManager.createAMSCheckins();
				fileManager.createCheckinsForClustering();
				//users
				Set<String> userIds = fileManager.createUserIdsAmsAms();
				fileManager.createAmsAmsRelevantFriendsAndCount(userIds);
			} catch (IOException | ParseException e) {
				e.printStackTrace();
			}
		}
		// use gowalla dataset (AMS) to suggest friends and evaluate such suggestions
		if (EVALUATE_GOWALLA) {
			try {
				globalFirebaseClusters = FirebaseHelper.getGlobalClusters(NUM_CLUSTERS);
			} catch (UnsupportedEncodingException | FirebaseException e1) {
				e1.printStackTrace();
			}
			
			globalClustersWithMean = globalFirebaseClusters.stream().map(x->x.mean).collect(Collectors.toList());
			id_userProfile = UserProfilesManager.createUserProfilesGowalla(new FileManager());

			try {
				id_userProfile = CheckInsManager.populateUserCheckInsFromGowalla(new FileManager(), globalClustersWithMean,
						id_userProfile, NUM_CLUSTERS);
			} catch (ParseException | IOException e) {
				e.printStackTrace();
			}

			SimilarityManager similarityManager = new SimilarityManager(id_userProfile, SEQ_SCORE_WEIGHT,
					ACT_SCORE_WEIGHT);
			id_userProfile = similarityManager.calculateSimilaritiesFromLocations(NUM_CLUSTERS,COLLABORATION_TYPE,COMPARING_DISTANCE_THRESHOLD,
					MATCHING_MAX_SEQ_LENGTH, TRANSITION_TIME_THRESHOLD, SAME_TIME_DAY_THRESHOLD);

			List<UserProfile> profiles = new ArrayList<>(id_userProfile.values());

			// writeResultsFirebase(profiles);
			writeResultsLocalStorage(new FileManager(), profiles, NUM_CLUSTERS);
			EvaluationManager evaluationManager = new EvaluationManager(profiles, SIMILARITY_THRESHOLD,NUM_CLUSTERS);
			evaluationManager.evaluateResults();
		}
		if (EVALUATE_GOWALLA_MULTI_LAYER) {
			
			id_userProfile = UserProfilesManager.createUserProfilesGowalla(new FileManager());
			
			for(int i = 1; i<= NUM_LAYERS ; i++) {
				int numClusters = i*2;
				try {
					globalFirebaseClusters = FirebaseHelper.getGlobalClusters(numClusters);
				} catch (UnsupportedEncodingException | FirebaseException e1) {
					e1.printStackTrace();
				}
				
				globalClustersWithMean = globalFirebaseClusters.stream().map(x->x.mean).collect(Collectors.toList());
				
				try {
					id_userProfile = CheckInsManager.populateUserCheckInsFromGowalla(new FileManager(), globalClustersWithMean,
							id_userProfile, numClusters);
				} catch (ParseException | IOException e) {
					e.printStackTrace();
				}

				SimilarityManager similarityManager = new SimilarityManager(id_userProfile, SEQ_SCORE_WEIGHT,
						ACT_SCORE_WEIGHT);
				id_userProfile = similarityManager.calculateSimilaritiesFromLocations(numClusters,COLLABORATION_TYPE,COMPARING_DISTANCE_THRESHOLD,
						MATCHING_MAX_SEQ_LENGTH, TRANSITION_TIME_THRESHOLD, SAME_TIME_DAY_THRESHOLD);
			}
			
			
			System.out.println("Multi Layer Score");
			List<UserProfile> profiles = new ArrayList<>(id_userProfile.values());
			profiles.stream().forEach(UserProfile::calculateMultiLayerScore);

			// writeResultsFirebase(profiles);
			writeResultsLocalStorage(new FileManager(), profiles,NUM_CLUSTERS);
			EvaluationManager evaluationManager = new EvaluationManager(profiles, SIMILARITY_THRESHOLD,NUM_CLUSTERS);
			evaluationManager.evaluateResults();
		}
		
		if(EVALUATE_BY_COUNT) {
			try {
				globalFirebaseClusters = FirebaseHelper.getGlobalClusters(NUM_CLUSTERS);
			} catch (UnsupportedEncodingException | FirebaseException e1) {
				e1.printStackTrace();
			}
			
			globalClustersWithMean = globalFirebaseClusters.stream().map(x->x.mean).collect(Collectors.toList());
			id_userProfile = UserProfilesManager.createUserProfilesGowalla(new FileManager());

			try {
				id_userProfile = CheckInsManager.populateUserCheckInsFromGowalla(new FileManager(), globalClustersWithMean,
						id_userProfile, NUM_CLUSTERS);
			} catch (ParseException | IOException e) {
				e.printStackTrace();
			}

			SimilarityManager similarityManager = new SimilarityManager(id_userProfile, SEQ_SCORE_WEIGHT,
					ACT_SCORE_WEIGHT);
			id_userProfile = similarityManager.calculateSimilaritiesByCount(NUM_CLUSTERS,COMPARING_DISTANCE_THRESHOLD);

			List<UserProfile> profiles = new ArrayList<>(id_userProfile.values());

			// writeResultsFirebase(profiles);
			// writeResultsLocalStorage(mFileManager, profiles);
			EvaluationManager evaluationManager = new EvaluationManager(profiles, SIMILARITY_THRESHOLD,NUM_CLUSTERS);
			evaluationManager.evaluateResults();
		}

		if (EVALUATE_SCALABILITY_TRAJ_SIZE) {
			try {
				String info = "------>>>>Evaluating Scalability, ";
				info += (REAL_VERSION) ? "REAL version, " : "BAD version, ";
				info += "with trajSize of " + TRAJ_SIZE;

				List<ScalabilityMetrics> metricsTrajSize = FirebaseHelper
						.getScalabilityMetricsTrajSizeFromFirebase(REAL_VERSION, TRAJ_SIZE);
				OptionalDouble averageBytesUploadTrajSize = metricsTrajSize.stream().map(x -> x.bytesUpload)
						.mapToLong(x -> x).average();
				OptionalDouble averageBytesDownloadTrajSize = metricsTrajSize.stream().map(x -> x.bytesDownload)
						.mapToLong(x -> x).average();
				OptionalDouble averageUpdatesTrajSize = metricsTrajSize.stream().map(x -> x.updates).mapToInt(x -> x)
						.average();

				System.out.println(info);
				System.out.println(metricsTrajSize.size() + " measures");
				System.out.println("Average Bytes Upload " + averageBytesUploadTrajSize.getAsDouble());
				System.out.println("Average Bytes Download " + averageBytesDownloadTrajSize.getAsDouble());
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
				OptionalDouble averageBytesUploadNumEvents = metricsNumEvents.stream().map(x -> x.bytesUpload)
						.mapToLong(x -> x).average();
				OptionalDouble averageBytesDownloadNumEvents = metricsNumEvents.stream().map(x -> x.bytesDownload)
						.mapToLong(x -> x).average();
				OptionalDouble averageUpdatesNumEvents = metricsNumEvents.stream().map(x -> x.updates).mapToInt(x -> x)
						.average();

				System.out.println(info);
				System.out.println(metricsNumEvents.size() + " measures");
				System.out.println("Average Bytes Upload " + averageBytesUploadNumEvents.getAsDouble());
				System.out.println("Average Bytes Download " + averageBytesDownloadNumEvents.getAsDouble());
				System.out.println("Average Updates " + averageUpdatesNumEvents.getAsDouble());

			} catch (UnsupportedEncodingException | FirebaseException e) {
				e.printStackTrace();
			}
		}

		if (EVALUATE_UX) {
			List<UXMetrics> metrics;
			try {
				metrics = FirebaseHelper.getUXMetricsFromFirebase(REAL_VERSION, NUM_EVENTS);
				OptionalDouble averageTime = metrics.stream().map(x -> x.timeUntilFirst).mapToLong(x -> x).average();

				System.out.println("--------->>>>>>>>Evaluating UX metrics :: numEvents " + NUM_EVENTS);
				System.out.println(metrics.size() + " measures");
				System.out.println("Average Time Until First " + averageTime.getAsDouble() + " milisecs");
			} catch (UnsupportedEncodingException | FirebaseException e) {
				e.printStackTrace();
			}
		}

		//suggest friends based on created events
		if (EVALUATE_EVENTS) {
			try {
				id_userProfile = UserProfilesManager.createUserProfilesFromFirebase();
				id_userProfile = FirebaseHelper.populateUserEventsFromFirebase(id_userProfile);
			} catch (UnsupportedEncodingException | FirebaseException e1) {
				e1.printStackTrace();
			}

			SimilarityManager similarityManager = new SimilarityManager(id_userProfile, SEQ_SCORE_WEIGHT,
					ACT_SCORE_WEIGHT);
			id_userProfile = similarityManager.calculateSimilaritiesFromEvents(NUM_CLUSTERS);
		}

		// cluster based on gowalla data
		else if (CLUSTER_GOWALLA) {
			globalClustersWithMean = clusterLocationsKMEANS(pathKmeansAmsCSV,NUM_CLUSTERS);
			long totalPoints = globalClustersWithMean.stream().mapToInt(x -> x.getVectors().size()).sum();
			System.out.println("TP "+totalPoints);
			try {
				FirebaseHelper.writeNewClustersFirebase(globalClustersWithMean, totalPoints, NUM_CLUSTERS);
			} catch (UnsupportedEncodingException | FirebaseException | JacksonUtilityException e) {
				e.printStackTrace();
			}
		}

		//cluster based on firebase data
		else if (CLUSTER_FIREBASE) {
			try {
				// check time passed
				Long lastUpdated = FirebaseHelper.getLastUpdatedFromFirebase();
				Long now = System.currentTimeMillis();

				// check new data
				long storedTotalLocations = FirebaseHelper.getStoredLocationsSizeFromFirebase();
				long currentLotalLocations = FirebaseHelper.getCurrentLocationsSizeFromFirebase();
				if (now - lastUpdated >= TIME_PASSED_THRESHOLD
						|| Math.abs(currentLotalLocations - storedTotalLocations) > NEW_DATA_THRESHOLD) {

					id_userProfile = UserProfilesManager.createUserProfilesFromFirebase();

					new FileManager().createCSVFirebaseLocations(id_userProfile, pathKmeansFirebaseCSV);

					globalClustersWithMean = clusterLocationsKMEANS(pathKmeansFirebaseCSV, NUM_CLUSTERS);

//					 try {
//					 FirebaseHelper.writeNewClustersFirebaseKMEANS(globalClusters, 10656);
//					 } catch (UnsupportedEncodingException | FirebaseException |
//					 JacksonUtilityException e) {
//					 e.printStackTrace();
//					 }
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

	private static List<ClusterWithMean> clusterLocationsKMEANS(String path, int numClusters) {
		List<ClusterWithMean> clusters = new ArrayList();

		try {
			clusters = applyKmeans(path, DELIMITER,numClusters);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return clusters;
	}

	private static void writeResultsFirebase(List<UserProfile> profiles) {
		try {
			FirebaseHelper.writeNewFriendsFirebase(profiles,NUM_CLUSTERS, true, 5, 10);// limited
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

	private static void writeResultsLocalStorage(FileManager fileManager, List<UserProfile> profiles, int numClusters) {

		int totalUsers = profiles.size();
		String friendsPath = "friendsOf" + totalUsers + "Users.csv";
		String foundPath = "foundOf" + totalUsers + "Users.csv";
		String foundPrctPath = "foundPRCTOf" + totalUsers + "Users.csv";

		try {
			fileManager.createCsvSimilarities(profiles, friendsPath, SIMILARITY_THRESHOLD,numClusters);
			fileManager.createFoundCSV(friendsPath, foundPath);
			fileManager.createFoundPrctCSV(foundPath, foundPrctPath);
			System.out.println("Average Suggested Friends GeoFriends");
			System.out.println(fileManager.getAverageSuggestedFriendsGeoFriends(friendsPath));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static List<ClusterWithMean> applyKmeans(String pathToCSV, String delimiter, int numClusters) throws IOException {
		DistanceFunction distanceFunction = new DistanceCosine();

		// Apply the algorithm
		AlgoKMeans algoKMeans = new AlgoKMeans();
		System.out.println("applying K-means");
		List<ClusterWithMean> clusters = algoKMeans.runAlgorithm(pathToCSV, numClusters, distanceFunction, delimiter);
		algoKMeans.printStatistics();

		// Print the clusters found by the algorithm
		// For each cluster:
		int i = 0;
		int mTotalCheckIns = 0;
		for (ClusterWithMean cluster : clusters) {
			System.out.println("Cluster " + i++);
			System.out.println("size -> " + cluster.getVectors().size());
			System.out.println("mean -> " + cluster.getmean().toString());
			mTotalCheckIns += cluster.getVectors().size();
		}
		System.out.println("Total CI " + mTotalCheckIns);
		return clusters;
	}

}
