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

public class FirebaseHelper {
	
	private static final String FIREBASE_URL = "https://geofriendsfire.firebaseio.com";
	
	private static void writeNewClustersFirebase(List<ClusterWithMean> globalClusters, int level)
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
}
