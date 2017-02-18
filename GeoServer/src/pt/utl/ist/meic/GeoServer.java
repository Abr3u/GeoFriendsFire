package pt.utl.ist.meic;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.thegreshams.firebase4j.error.FirebaseException;
import net.thegreshams.firebase4j.error.JacksonUtilityException;
import net.thegreshams.firebase4j.model.FirebaseResponse;
import net.thegreshams.firebase4j.service.Firebase;
import utility.FileManager;
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

	private static Map<String, UserProfile> usersProfiles;

	public static void main(String[] args) throws ParseException, IOException {

		initGlobalClustersList();
		usersProfiles = new HashMap<String, UserProfile>();
		FileManager mFileManager = new FileManager();

		// mFileManager.stuff();
		List<CheckIn> userCheckIns = mFileManager.getUserCheckInsCsv("22", false);

		Graph graph = getUserGraphFromCheckIns(userCheckIns);

		System.out.println("graph tamanho -> " + graph.vertexes.size());
		
		graph.buildSequences();
		graph.printSequences();

		UserProfile profile = new UserProfile("22");
		profile.addNewGraph("0", graph);

		// Graph graph = new Graph();
		// for(ClusterWithMean c : globalClusters){
		// graph.addVertex(c);
		// }
		// graph.getGraphContentString();

		// mFileManager.createCsvStayPoints("teste.csv");

		/*
		 * applyKmeans("clusters1"); try { writeNewClustersFirebase(2); } catch
		 * (FirebaseException | JacksonUtilityException e) {
		 * e.printStackTrace(); }
		 */

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

	private static Graph getUserGraphFromCheckIns(List<CheckIn> userCheckIns) {
		Graph graph = new Graph();
		// inserir cada checkIn no seu cluster => criar um vertice no graph
		for (CheckIn ci : userCheckIns) {
			// values are init only
			double minDistance = Double.MAX_VALUE;
			ClusterWithMean minCluster = globalClusters.get(0);
			for (ClusterWithMean cluster : globalClusters) {
				double distance = distanceBetween(ci.getDataPoint().getLatitude(), cluster.getmean().get(0),
						ci.getDataPoint().getLongitude(), cluster.getmean().get(1));

				if (distance < minDistance) {
					minDistance = distance;
					minCluster = cluster;
				}
			}
			graph.addVertex(new VertexInfo(minCluster, ci.getDate()));
		}
		return graph;
	}

	private static double distanceBetween(double lat1, double lat2, double lon1, double lon2) {

		final int R = 6371; // Radius of the earth

		Double latDistance = Math.toRadians(lat2 - lat1);
		Double lonDistance = Math.toRadians(lon2 - lon1);
		Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
		Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double distance = R * c * 1000; // convert to meters

		distance = Math.pow(distance, 2);

		return Math.sqrt(distance);
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
			firebase = new Firebase(FIREBASE_URL + "/clustersLevel" + level);
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
		if (level == 0) {
			response = firebase.delete("clusters");
		} else {
			response = firebase.delete("clustersLevel" + level);
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
