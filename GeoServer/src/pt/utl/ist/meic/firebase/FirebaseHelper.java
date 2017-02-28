package pt.utl.ist.meic.firebase;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ca.pfv.spmf.algorithms.clustering.distanceFunctions.DistanceCosine;
import ca.pfv.spmf.algorithms.clustering.distanceFunctions.DistanceFunction;
import ca.pfv.spmf.algorithms.clustering.kmeans.AlgoKMeans;
import ca.pfv.spmf.patterns.cluster.ClusterWithMean;
import net.thegreshams.firebase4j.error.FirebaseException;
import net.thegreshams.firebase4j.error.JacksonUtilityException;
import net.thegreshams.firebase4j.model.FirebaseResponse;
import net.thegreshams.firebase4j.service.Firebase;
import pt.utl.ist.meic.domain.UserProfile;

public class FirebaseHelper {
	
	private static final String FIREBASE_URL = "https://geofriendsfire.firebaseio.com";
	
	
	
	
	public static void writeNewClustersFirebase(List<ClusterWithMean> globalClusters, int level,long totalCheckIns)
			throws FirebaseException, JacksonUtilityException, UnsupportedEncodingException {

		deleteClustersFirebase(level);

		Firebase firebase = new Firebase(FIREBASE_URL + "/clusters" + level);
		
		for (int i = 0; i < globalClusters.size(); i++) {
			// "PUT cluster to /clusters
			Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
			dataMap.put("mean", globalClusters.get(i).getmean().toString());
			dataMap.put("size", globalClusters.get(i).getVectors().size());
			dataMap.put("sizePerc", new Double(globalClusters.get(i).getVectors().size()) / totalCheckIns);
			
			
			FirebaseResponse response = firebase.put("cluster" + i, dataMap);
			System.out.println("\n\nResult of PUT cluster:\n" + response.getRawBody().toString());
			System.out.println("\n");
		}

	}


	private static void deleteClustersFirebase(int level) throws FirebaseException, UnsupportedEncodingException {
		Firebase firebase = new Firebase(FIREBASE_URL);
		FirebaseResponse response = firebase.delete("clusters" + level);
		System.out.println(response.getBody().toString());
	}
	
	public static void writeNewFriendsFirebase(List<UserProfile> profiles)
			throws FirebaseException, JacksonUtilityException, UnsupportedEncodingException {

		deleteFriendsFirebase();

		Firebase firebase = new Firebase(FIREBASE_URL + "/friends");
		for (UserProfile profile : profiles) {
			// "POST cluster to /clusters
			Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
			dataMap.putAll(profile.getSimilarities());
			
			FirebaseResponse response = firebase.put("user"+profile.userId, dataMap);
			System.out.println("\n\nResult of PUT friends:\n" + response.getRawBody().toString());
			System.out.println("\n");
		}

	}


	private static void deleteFriendsFirebase() throws FirebaseException, UnsupportedEncodingException {
		Firebase firebase = new Firebase(FIREBASE_URL);
		FirebaseResponse response = firebase.delete("friends");
		System.out.println(response.getBody().toString());
	}
}
