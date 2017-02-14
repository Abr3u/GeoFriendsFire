package pt.utl.ist.meic;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.thegreshams.firebase4j.error.FirebaseException;
import net.thegreshams.firebase4j.error.JacksonUtilityException;
import net.thegreshams.firebase4j.model.FirebaseResponse;
import net.thegreshams.firebase4j.service.Firebase;

import ca.pfv.spmf.algorithms.clustering.distanceFunctions.DistanceCosine;
import ca.pfv.spmf.algorithms.clustering.distanceFunctions.DistanceFunction;
import ca.pfv.spmf.algorithms.clustering.kmeans.AlgoKMeans;
import ca.pfv.spmf.patterns.cluster.ClusterWithMean;
import ca.pfv.spmf.patterns.cluster.DoubleArray;

public class GeoServer {

	// Delimiter used in CSV file
	private static final String DELIMITER = ",";
	private static final String FIREBASE_URL = "https://geofriendsfire.firebaseio.com";

	private static final int NUM_CLUSTERS = 5;// kmeans

	private static List<ClusterWithMean> globalClusters;
	private static List<ClusterWithMean> userClusters;

	public static void main(String[] args) throws ParseException, IOException {

		// getUserCheckInsCSV(pathGlobalCSV, "2");
		// System.out.println("got checkIns -> " + userCheckIns.size());
		// createCSV("user2.csv");

		//initGlobalClustersList();
		//getDistancesGlobalClusters();
		
		applyKmeans("clusters1");
		try {
			writeNewClustersFirebase(2);
		} catch (FirebaseException | JacksonUtilityException e) {
			e.printStackTrace();
		}
		
		/*
		 * try { writeDistancesFirebase(); } catch (FirebaseException |
		 * JacksonUtilityException e) { e.printStackTrace(); }
		 */

		/*
		 * applyKmeansUser("2"); try { writeUserClustersFirebase("2"); } catch
		 * (FirebaseException | JacksonUtilityException e) {
		 * e.printStackTrace(); }
		 */

		// applyDBSCAN();
		// readClustersFromDisk();
		// System.out.println("clusters -> "+clusters.size());

	}

	private static void initGlobalClustersList() {
		globalClusters = new ArrayList<ClusterWithMean>();

		ClusterWithMean cluster = new ClusterWithMean(0);
		cluster.setMean(new DoubleArray(new double[] { 40.801093651170824, -73.89930599057355 }));
		cluster.mId = 0;
		globalClusters.add(cluster);

		cluster = new ClusterWithMean(0);
		cluster.setMean(new DoubleArray(new double[] { 40.60970277282131, -74.00150411120603 }));
		cluster.mId = 1;
		globalClusters.add(cluster);

		cluster = new ClusterWithMean(0);
		cluster.setMean(new DoubleArray(new double[] { 40.75669592004704, -73.960743752147 }));
		cluster.mId = 2;
		globalClusters.add(cluster);

		cluster = new ClusterWithMean(0);
		cluster.setMean(new DoubleArray(new double[] { 40.69826256184573, -73.99754039600585 }));
		cluster.mId = 3;
		globalClusters.add(cluster);

		cluster = new ClusterWithMean(0);
		cluster.setMean(new DoubleArray(new double[] { 40.73388423756347, -73.98810826426154 }));
		cluster.mId = 4;
		globalClusters.add(cluster);
	}

	private static void applyKmeans(String pathToCSV) throws IOException {
		DistanceFunction distanceFunction = new DistanceCosine();

		// Apply the algorithm
		AlgoKMeans algoKMeans = new AlgoKMeans();
		globalClusters = algoKMeans.runAlgorithm(pathToCSV, NUM_CLUSTERS, distanceFunction, DELIMITER);
		algoKMeans.printStatistics();

		// Print the clusters found by the algorithm
		// For each cluster:
		int i = 0;
		for (ClusterWithMean cluster : globalClusters) {
			System.out.println("Cluster " + i++);
			System.out.println("size -> " + cluster.getVectors().size());
			System.out.println("mean -> " + cluster.getmean().toString());
		}

		// writeClustersToDisk();
	}

	private static void applyKmeansUser(String userId) throws IOException {
		DistanceFunction distanceFunction = new DistanceCosine();

		// Apply the algorithm
		AlgoKMeans algoKMeans = new AlgoKMeans();
		userClusters = algoKMeans.runAlgorithm("user" + userId + ".csv", NUM_CLUSTERS, distanceFunction, DELIMITER);
		algoKMeans.printStatistics();

		// Print the clusters found by the algorithm
		// For each cluster:
		int i = 0;
		for (ClusterWithMean cluster : userClusters) {
			System.out.println("Cluster " + i++);
			System.out.println("size -> " + cluster.getVectors().size());
			System.out.println("mean -> " + cluster.getmean().toString());
		}

		// writeClustersToDisk();
	}

	private static void writeNewClustersFirebase(int level)
			throws FirebaseException, JacksonUtilityException, UnsupportedEncodingException {

		deleteClustersFirebase(level);

		Firebase firebase;
		if (level == 0) {
			firebase = new Firebase(FIREBASE_URL + "/clusters");
		} else {
			firebase = new Firebase(FIREBASE_URL+"/clustersLevel"+level);
		}
		for (int i = 0; i < globalClusters.size(); i++) {
			// "POST cluster to /clusters
			Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
			dataMap.put("clusterMean", globalClusters.get(i).getmean().toString());
			FirebaseResponse response = firebase.post("cluster" + i, dataMap);
			System.out.println("\n\nResult of POST cluster:\n" + response.getRawBody().toString());
			System.out.println("\n");
		}

	}

	private static void writeUserClustersFirebase(String userId)
			throws FirebaseException, JacksonUtilityException, UnsupportedEncodingException {

		deleteUserClustersFirebase(userId);

		Firebase firebase = new Firebase(FIREBASE_URL + "/usersClusters/" + userId);
		for (int i = 0; i < userClusters.size(); i++) {
			// "POST cluster to /clusters
			Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
			dataMap.put("clusterMean", userClusters.get(i).getmean().toString());
			FirebaseResponse response = firebase.post("cluster" + i, dataMap);
			System.out.println("\n\nResult of POST cluster:\n" + response.getRawBody().toString());
			System.out.println("\n");
		}
	}

	private static void deleteClustersFirebase(int level) throws FirebaseException, UnsupportedEncodingException {
		Firebase firebase = new Firebase(FIREBASE_URL);
		FirebaseResponse response;
		if(level == 0){
			response = firebase.delete("clusters");
		}else{
			response = firebase.delete("clustersLevel"+level);
		}
		System.out.println(response.getBody().toString());
	}

	private static void deleteUserClustersFirebase(String userId)
			throws FirebaseException, UnsupportedEncodingException {
		Firebase firebase = new Firebase(FIREBASE_URL);
		FirebaseResponse response = firebase.delete("userClusters/" + userId);
		System.out.println(response.getBody().toString());
	}

	private static void deleteDistancesFirebase() throws FirebaseException, UnsupportedEncodingException {
		Firebase firebase = new Firebase(FIREBASE_URL);
		FirebaseResponse response = firebase.delete("clustersDistances");
		System.out.println(response.getBody().toString());
	}

}
