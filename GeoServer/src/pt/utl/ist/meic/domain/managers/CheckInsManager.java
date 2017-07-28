package pt.utl.ist.meic.domain.managers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import ca.pfv.spmf.patterns.cluster.ClusterWithMean;
import net.thegreshams.firebase4j.error.FirebaseException;
import pt.utl.ist.meic.domain.CheckIn;
import pt.utl.ist.meic.domain.Graph;
import pt.utl.ist.meic.domain.UserProfile;
import pt.utl.ist.meic.domain.VertexInfo;
import pt.utl.ist.meic.firebase.FirebaseHelper;
import pt.utl.ist.meic.utility.FileManager;

public class CheckInsManager {
	
	public static Map<String, UserProfile> populateUserCheckInsFromFirebase(List<ClusterWithMean> clusters,Map<String,UserProfile> profiles,int numClusters) throws UnsupportedEncodingException, FirebaseException {
		List<CheckIn> userCheckIns;
		Graph graph;

		for (UserProfile profile : profiles.values()) {
			userCheckIns = FirebaseHelper.getUserCheckIns(profile.userId);
			graph = getUserGraphFromCheckIns(clusters, userCheckIns, numClusters);
			profile.setGraph(graph);
		}
		return profiles;
	}
	
	//TODO remove
	public static Map<String, UserProfile> populateUserCheckInsFromGowalla(FileManager fm,List<ClusterWithMean> clusters, 
			Map<String,UserProfile> profiles,int numClusters) throws ParseException, IOException {
		List<CheckIn> userCheckIns;
		Graph graph;

		for (UserProfile profile : profiles.values()) {
			userCheckIns = fm.getUserCheckInsCsv(profile.userId);
			graph = getUserGraphFromCheckIns(clusters, userCheckIns, numClusters);
			profile.setGraph(graph);
		}
		return profiles;
	}

	private static Graph getUserGraphFromCheckIns(List<ClusterWithMean> clusters, List<CheckIn> userCheckIns, int numClusters) {
		Graph graph = new Graph(numClusters);
		// inserir cada checkIn no seu cluster => criar um vertice no graph
		for (CheckIn ci : userCheckIns) {
			// values are init only
			double minDistance = Double.MAX_VALUE;
			ClusterWithMean minCluster = clusters.get(0);// init with first
			for (ClusterWithMean cluster : clusters) {
				double distance = distanceBetween(ci.getDataPoint().getLatitude(), cluster.getmean().get(0),
						ci.getDataPoint().getLongitude(), cluster.getmean().get(1));

				if (distance < minDistance) {
					minDistance = distance;
					minCluster = cluster;
				}
			}
			graph.addVertex(new VertexInfo(minCluster, ci.getDate()));
		}
		graph.buildSequences();
		graph.buildPercentages();
		return graph;
	}
	
	// returns in meters
		private static double distanceBetween(double lat1, double lat2, double lon1, double lon2) {

			final int R = 6371; // Radius of the earth

			Double latDistance = Math.toRadians(lat2 - lat1);
			Double lonDistance = Math.toRadians(lon2 - lon1);
			Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(lat1))
					* Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
			Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
			double distance = R * c * 1000; // convert to meters

			return distance;
		}
	
}
