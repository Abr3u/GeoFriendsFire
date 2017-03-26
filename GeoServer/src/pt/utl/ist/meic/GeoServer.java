package pt.utl.ist.meic;

import java.io.IOException;
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
import pt.utl.ist.meic.firebase.FirebaseHelper;
import pt.utl.ist.meic.utility.FileManager;

public class GeoServer {

	private static final String pathKmeansNewYorkCSV = "C:/Android/GeoFriendsFire/GeoServer/dataset/newYork.csv";

	private static final int NUM_CLUSTERS = 5;// kmeans
	private static final String DELIMITER = ",";

	private static final int MATCHING_MAX_SEQ_LENGTH = 20;// analisar seqs no
															// maximo de 50
															// clusters
	private static final long TRANSITION_TIME_THRESHOLD = 2 * 60 * 60 * 1000;// 2 horaas

	private static final double ACT_SCORE_WEIGHT = 0.75;
	private static final double SEQ_SCORE_WEIGHT = 0.25;
	private static final double THRESHOLD = 0.5;

	private static List<ClusterWithMean> globalClusters;
	private static Map<Integer, Double> globalPercentages;
	private static Map<Integer, Long> globalCheckIns;

	private static List<UserProfile> usersProfiles;

	private static long mTotalCheckIns = 130425;// newYork

	public static void main(String[] args) throws ParseException, IOException {

		FileManager mFileManager = new FileManager();
		
		List<String> idList = mFileManager.getIdListFromFileNy();
		int totalUsers = idList.size();

		String friendsPath = "friendsOf" + totalUsers + "Users.csv";
		String foundPath = "foundOf" + totalUsers + "Users.csv";
		String foundPrctPath = "foundPRCTOf" + totalUsers + "Users.csv";

		initGlobalClustersList();
		// initGlobalPercentages();
		// initGlobalCheckIns();
		initUserProfiles(idList);

		calculateGraphs(mFileManager, "0");
		calculateSimilarities();

		mFileManager.createCsvSimilarities(usersProfiles, friendsPath, THRESHOLD);
		mFileManager.createFoundCSV(friendsPath, foundPath);
		mFileManager.createFoundPrctCSV(foundPath, foundPrctPath);

		System.out.println("Precision " + mFileManager.calculatePrecision(foundPath));
		System.out.println("Recall " + mFileManager.calculateRecall(foundPath));
		
		usersProfiles.stream().forEach(x -> {
			try {
				mFileManager.calculateAveragePrecision(x);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		System.out.println("MAP "+
		usersProfiles.stream().mapToDouble(UserProfile::getAveragePrecision).sum()/usersProfiles.size());

		// try {
		// FirebaseHelper.writeNewFriendsFirebase(usersProfiles, 5, 10);
		// } catch (FirebaseException | JacksonUtilityException e) {
		// e.printStackTrace();
		// }

		// applyKmeans(pathKmeansNewYorkCSV);

		// try {
		// FirebaseHelper.writeNewClustersFirebase(globalClusters, 0,
		// mTotalCheckIns);
		// } catch (FirebaseException | JacksonUtilityException e) {
		// e.printStackTrace();
		// }

	}

	private static void calculateGraphs(FileManager fileManager, String level) throws ParseException, IOException {
		List<CheckIn> userCheckIns;
		Graph graph;

		for (UserProfile profile : usersProfiles) {
			userCheckIns = fileManager.getUserCheckInsCsv(profile.userId);
			graph = getUserGraphFromCheckIns(userCheckIns);
			profile.addNewGraph(level, graph);
		}
	}

	private static void initUserProfiles(List<String> ids) {
		usersProfiles = new ArrayList<UserProfile>();
		for (String id : ids) {
			UserProfile profile = new UserProfile(id);
			usersProfiles.add(profile);
		}
	}

	private static void calculateSimilarities() {
		// calculate seqScore for each user pair
		for (int i = 0; i < usersProfiles.size(); i++) {
			for (int j = 0; j < usersProfiles.size(); j++) {
				if (i != j && j > i) {
					UserProfile p1 = usersProfiles.get(i);
					UserProfile p2 = usersProfiles.get(j);
					double seqScore = getUserSimilaritySequences(p1, p2, "0");
					p1.addSimilarityScore(p2.userId, seqScore);
					p2.addSimilarityScore(p1.userId, seqScore);
				}
			}
		}
		// tranform seqScore [0-1]
		for (UserProfile profile : usersProfiles) {
			profile.normalizeSimilarityScores();
		}
		// take into account activity Score
		for (int i = 0; i < usersProfiles.size(); i++) {
			for (int j = 0; j < usersProfiles.size(); j++) {
				if (i != j && j > i) {
					UserProfile p1 = usersProfiles.get(i);
					UserProfile p2 = usersProfiles.get(j);
					double finalScore;
					double actScore = getUserSimilarityClusterActivity(p1, p2, "0");
					// System.out.println("actScore "+actScore);

					double seqScore1 = p1.getSimilarityScore(p2.userId);
					finalScore = calculateFinalScore(seqScore1, actScore);
					p1.addSimilarityScore(p2.userId, finalScore);

					double seqScore2 = p2.getSimilarityScore(p1.userId);
					finalScore = calculateFinalScore(seqScore2, actScore);
					p2.addSimilarityScore(p1.userId, finalScore);
				}
			}
		}
	}

	private static double calculateFinalScore(double seqScore, double actScore) {
		return SEQ_SCORE_WEIGHT * seqScore + ACT_SCORE_WEIGHT * actScore;
	}

	private static double getUserSimilaritySequences(UserProfile profileA, UserProfile profileB, String level) {
		if (!profileA.userId.equals(profileB.userId)) {
			Graph graphA = profileA.getGraphByLevel(level);
			Graph graphB = profileB.getGraphByLevel(level);

			// transformar seqs em seqs aggregadas e ir buscar as top N
			Set<Sequence> seqsA = graphA.getTopNSequences(5, graphA.getAggregatedSeqs());
			Set<Sequence> seqsB = graphB.getTopNSequences(5, graphB.getAggregatedSeqs());

			double score = 0;
			for (Sequence seqA : seqsA) {
				for (Sequence seqB : seqsB) {
					Set<Sequence> maxLengthSeqs = sequenceMatching(seqA, seqB, MATCHING_MAX_SEQ_LENGTH,
							TRANSITION_TIME_THRESHOLD);
					for (Sequence simseq : maxLengthSeqs) {
						double size = simseq.getClusters().size();
						score += size * Math.pow(2.0, size - 1);
					}
				}
			}
			return score;
		}
		return 0;
	}

	private static double getUserSimilarityClusterActivity(UserProfile profileA, UserProfile profileB, String level) {
		if (!profileA.userId.equals(profileB.userId)) {
			Graph graphA = profileA.getGraphByLevel(level);
			Graph graphB = profileB.getGraphByLevel(level);

			double score = 0;

			Map<Integer, Double> percentageA = graphA.cluster_percentage;
			Map<Integer, Double> percentageB = graphB.cluster_percentage;

			// somar diferencas
			for (Map.Entry<Integer, Double> entry : percentageA.entrySet()) {
				if (percentageB.containsKey(entry.getKey())) {
					double aux = percentageB.get(entry.getKey());
					score += Math.abs(aux - entry.getValue());
				}
			}
			// passar de range [0-2] para [0-1]
			score = transformScoreToDiffPrct(score);
			// passar para prct de igualdade
			score = 1 - score;
			return score;
		}
		return 0;
	}

	private static double transformScoreToDiffPrct(double score) {
		double min = 0;
		double max = 2;
		double newmax = 1;
		double newmin = 0;
		if (score > 0) {
			return ((score - min) / (max - min)) * (newmax - newmin) + newmin;
		} else {
			return 0;
		}
	}

	// returns set of maximum length similar sequences
	private static Set<Sequence> sequenceMatching(Sequence seqA, Sequence seqB, int matchingMaxSeqLength,
			long transitionTimeThreshold) {

		int step = 1;
		// add 1-length sequences into sequenceSet
		Set<SequenceAuxiliar> sequenceSet = add1LengthSequences(seqA, seqB);

		while (step <= matchingMaxSeqLength) {
			Set<SequenceAuxiliar> toAdd = new HashSet<SequenceAuxiliar>();
			for (SequenceAuxiliar seq : sequenceSet) {
				if (seq.mVertexes.size() == step) {
					// extend step-length seq to be (step + 1)-length
					toAdd.addAll(extendSequence(sequenceSet, seq, transitionTimeThreshold, seqA, seqB));
				}
			}
			sequenceSet.addAll(toAdd);
			// as 1-length estao todas contidas nas 2-legth de certeza
			if (step == 1) {
				sequenceSet = pruneSequences(sequenceSet);
			}
			step++;
		}
		sequenceSet = pruneSequences(sequenceSet);
		Set<Sequence> toReturn = new HashSet<Sequence>();

		// System.out.println("adicionar para devolver sequencias normais");
		for (SequenceAuxiliar seq : sequenceSet) {
			toReturn.add(seq.toNormalSequence());
			// System.out.println(seq);
		}
		return toReturn;

	}

	private static Set<SequenceAuxiliar> add1LengthSequences(Sequence a, Sequence b) {
		Set<SequenceAuxiliar> toReturn = new HashSet<SequenceAuxiliar>();

		for (int i = 0; i < a.getClusters().size(); i++) {
			VertexInfo v1 = a.getClusters().get(i);
			for (int j = 0; j < b.getClusters().size(); j++) {
				VertexInfo v2 = b.getClusters().get(j);
				if (v1.cluster.equals(v2.cluster)) {
					SequenceAuxiliar aux = new SequenceAuxiliar();
					aux.mVertexes.add(new AuxiliarVertex(v1.cluster, i, j, v1.date));
					toReturn.add(aux);
				}
			}
		}

		return toReturn;
	}

	private static Set<SequenceAuxiliar> pruneSequences(Set<SequenceAuxiliar> sequenceSet) {
		if (sequenceSet.size() > 0) {
			int maxLength = sequenceSet.iterator().next().mVertexes.size();
			for (SequenceAuxiliar seq : sequenceSet) {
				maxLength = (seq.mVertexes.size() > maxLength) ? seq.mVertexes.size() : maxLength;
			}
			final int maxLengthFinal = maxLength;// woooooooow amazing
			return sequenceSet.stream().filter(x -> x.mVertexes.size() == maxLengthFinal).collect(Collectors.toSet());
		}
		return sequenceSet;
	}

	private static Set<SequenceAuxiliar> extendSequence(Set<SequenceAuxiliar> set, SequenceAuxiliar seq,
			long transitionTimeThreshold, Sequence a, Sequence b) {

		Set<SequenceAuxiliar> toReturn = new HashSet<SequenceAuxiliar>();

		Set<SequenceAuxiliar> auxSet = new HashSet<SequenceAuxiliar>(set);
		auxSet.remove(seq);

		for (SequenceAuxiliar aux : auxSet) {
			if (seq.mVertexes.size() == 1) {
				AuxiliarVertex v1 = seq.getLastVertex();
				AuxiliarVertex v2 = aux.getLastVertex();
				// System.out.println("estou a analisar " + v1 + " com " + v2);
				if (v1.vertex.mId != v2.vertex.mId && consequentIndexes(v1, v2)) {
					// se for tamanho 1, nao comparamos sequencias para o
					// mesmo cluster
					// System.out.println("consequent and diferent!");
					// calcular o tempo perdido a ir de seq para aux em cada
					// sequencia original
					long travelTime1 = Math.abs(a.getClusters().get(v1.index1).leavTime.getTime()
							- a.getClusters().get(v2.index1).arrTime.getTime());
					long travelTime2 = Math.abs(b.getClusters().get(v1.index2).leavTime.getTime()
							- b.getClusters().get(v2.index2).arrTime.getTime());

					long delta = Math.abs(travelTime1 - travelTime2);
					if (delta <= transitionTimeThreshold) {
						SequenceAuxiliar result = new SequenceAuxiliar();
						result.mVertexes.add(v1);
						result.mVertexes.add(v2);
						// System.out.println("aumentei seq Unitaria para :: " +
						// result);
						toReturn.add(result);
					}
				}
			} else if (seq.mVertexes.size() > 1) {
				AuxiliarVertex v1 = seq.getLastVertex();
				AuxiliarVertex v2 = aux.getFirstVertex();
				// System.out.println("estou a analisar " + v1 + " com " + v2);
				if (consequentIndexes(v1, v2)) {
					// System.out.println("consequent!");
					long travelTime1;
					long travelTime2;
					long delta;
					if (v1.equals(v2)) {
						// System.out.println("same! calcular delta com
						// seguinte");

						v2 = aux.getSecondVertex();

						travelTime1 = Math.abs(a.getClusters().get(v1.index1).leavTime.getTime()
								- a.getClusters().get(v2.index1).arrTime.getTime());
						travelTime2 = Math.abs(b.getClusters().get(v1.index2).leavTime.getTime()
								- b.getClusters().get(v2.index2).arrTime.getTime());

						delta = Math.abs(travelTime1 - travelTime2);
						if (delta <= transitionTimeThreshold && !shareIndexes(v1, v2)) {
							SequenceAuxiliar result = new SequenceAuxiliar();
							result.mVertexes.addAll(seq.mVertexes);
							result.mVertexes.add(aux.getSecondVertex());
							// System.out.println("aumentei seq para :: " +
							// result);
							toReturn.add(result);
						}
					} else {
						// System.out.println("nao encaixam!");
						if (!shareIndexes(v1, v2)) {
							// System.out.println("nao partilham");
							travelTime1 = Math.abs(a.getClusters().get(v1.index1).leavTime.getTime()
									- a.getClusters().get(v2.index1).arrTime.getTime());
							travelTime2 = Math.abs(b.getClusters().get(v1.index2).leavTime.getTime()
									- b.getClusters().get(v2.index2).arrTime.getTime());

							delta = Math.abs(travelTime1 - travelTime2);
							if (delta <= transitionTimeThreshold) {
								SequenceAuxiliar result = new SequenceAuxiliar();
								result.mVertexes.addAll(seq.mVertexes);
								result.mVertexes.add(v2);
								// System.out.println("aumentei seq para :: " +
								// result);
								toReturn.add(result);
							}
						}
					}
				}
			}
		}
		return toReturn;

	}

	private static boolean shareIndexes(AuxiliarVertex v1, AuxiliarVertex v2) {
		return v1.index1 == v2.index1 || v1.index2 == v2.index2;
	}

	private static boolean consequentIndexes(AuxiliarVertex v1, AuxiliarVertex v2) {
		return v1.index1 <= v2.index1 && v1.index2 <= v2.index2;
	}

	private static Graph getUserGraphFromCheckIns(List<CheckIn> userCheckIns) {
		Graph graph = new Graph(NUM_CLUSTERS);
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
		graph.buildSequences();
		graph.buildPercentages();
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
		cluster.setMean(new DoubleArray(new double[] { 40.68596772385074, -73.99656293456547 }));
		cluster.mId = 0;
		globalClusters.add(cluster);

		cluster = new ClusterWithMean(0);
		cluster.setMean(new DoubleArray(new double[] { 40.77761934847919, -73.92851136376517 }));
		cluster.mId = 1;
		globalClusters.add(cluster);

		cluster = new ClusterWithMean(0);
		cluster.setMean(new DoubleArray(new double[] { 40.75076361515444, -73.96725029838117 }));
		cluster.mId = 2;
		globalClusters.add(cluster);

		cluster = new ClusterWithMean(0);
		cluster.setMean(new DoubleArray(new double[] { 40.72968661755094, -73.99184556681594 }));
		cluster.mId = 3;
		globalClusters.add(cluster);

		cluster = new ClusterWithMean(0);
		cluster.setMean(new DoubleArray(new double[] { 40.83586092995377, -73.89130396728055 }));
		cluster.mId = 4;
		globalClusters.add(cluster);
	}

	private static void initGlobalPercentages() {
		globalPercentages = new HashMap<Integer, Double>();
		globalPercentages.put(0, 0.08792026068621814);
		globalPercentages.put(1, 0.036818094690435114);
		globalPercentages.put(2, 0.35239409622388346);
		globalPercentages.put(3, 0.12719187272378762);
		globalPercentages.put(4, 0.3956756756756757);
	}

	private static void initGlobalCheckIns() {
		globalCheckIns = new HashMap<Integer, Long>();
		globalCheckIns.put(0, 11467l);
		globalCheckIns.put(1, 4802l);
		globalCheckIns.put(2, 45961l);
		globalCheckIns.put(3, 16589l);
		globalCheckIns.put(4, 51606l);
	}

	private static void applyKmeans(String pathToCSV) throws IOException {
		DistanceFunction distanceFunction = new DistanceCosine();

		// Apply the algorithm
		AlgoKMeans algoKMeans = new AlgoKMeans();
		System.out.println("applying K-means");
		globalClusters = algoKMeans.runAlgorithm(pathToCSV, NUM_CLUSTERS, distanceFunction, DELIMITER);
		algoKMeans.printStatistics();

		// Print the clusters found by the algorithm
		// For each cluster:
		int i = 0;
		mTotalCheckIns = 0;
		for (ClusterWithMean cluster : globalClusters) {
			System.out.println("Cluster " + i++);
			System.out.println("size -> " + cluster.getVectors().size());
			System.out.println("mean -> " + cluster.getmean().toString());
			mTotalCheckIns += cluster.getVectors().size();
		}
	}
}
