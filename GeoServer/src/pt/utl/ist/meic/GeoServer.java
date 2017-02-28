package pt.utl.ist.meic;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.plaf.synth.SynthSpinnerUI;

import net.thegreshams.firebase4j.error.FirebaseException;
import net.thegreshams.firebase4j.error.JacksonUtilityException;
import net.thegreshams.firebase4j.model.FirebaseResponse;
import net.thegreshams.firebase4j.service.Firebase;
import pt.utl.ist.meic.domain.AuxiliarVertex;
import pt.utl.ist.meic.domain.CheckIn;
import pt.utl.ist.meic.domain.Graph;
import pt.utl.ist.meic.domain.Sequence;
import pt.utl.ist.meic.domain.SequenceAuxiliar;
import pt.utl.ist.meic.domain.UserProfile;
import pt.utl.ist.meic.domain.VertexInfo;
import pt.utl.ist.meic.exceptions.CantExtendSequenceException;
import pt.utl.ist.meic.firebase.FirebaseHelper;
import pt.utl.ist.meic.utility.FileManager;
import ca.pfv.spmf.algorithms.clustering.distanceFunctions.DistanceCosine;
import ca.pfv.spmf.algorithms.clustering.distanceFunctions.DistanceFunction;
import ca.pfv.spmf.algorithms.clustering.kmeans.AlgoKMeans;
import ca.pfv.spmf.patterns.cluster.ClusterWithMean;
import ca.pfv.spmf.patterns.cluster.DoubleArray;

public class GeoServer {

	private static final String pathKmeansNewYorkCSV = "C:/Android/GeoFriendsFire/GeoServer/dataset/newYork.csv";

	private static final int NUM_CLUSTERS = 5;// kmeans
	private static final String DELIMITER = ",";

	private static final int MATCHING_MAX_SEQ_LENGTH = 20;// analisar seqs no
															// maximo de 50
															// clusters
	private static final long TRANSITION_TIME_THRESHOLD = 2 * 60 * 60 * 1000;// 2
																				// horas

	private static List<ClusterWithMean> globalClusters;
	private static Map<Integer, Double> globalPercentages;
	private static Map<Integer, Long> globalCheckIns;

	private static List<UserProfile> usersProfiles;

	private static long mTotalCheckIns = 130425;//newYork

	public static void main(String[] args) throws ParseException, IOException {

		 FileManager mFileManager = new FileManager();
		
		 initGlobalClustersList();
//		 initGlobalPercentages();
//		 initGlobalCheckIns();
		 initUserProfiles(mFileManager.getIdListFromFile());
		
		 calculateGraphs(mFileManager,"0");
		 calculateSimilarities();
		 
		try {
			FirebaseHelper.writeNewFriendsFirebase(usersProfiles);
		} catch (FirebaseException | JacksonUtilityException e) {
			e.printStackTrace();
		}

		// applyKmeans(pathKmeansNewYorkCSV);

//		try {
//			FirebaseHelper.writeNewClustersFirebase(globalClusters, 0, mTotalCheckIns);
//		} catch (FirebaseException | JacksonUtilityException e) {
//			e.printStackTrace();
//		}

		// mFileManager.createCsvSimilarities(usersProfiles,
		// "similarities10Plus.csv");

		// testUserSimilarity(mFileManager);
		// mFileManager.createCsvSimilarities(usersProfiles,
		// "similarities.csv");

		// testSequenceMatching();

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

	private static void calculateGraphs(FileManager fileManager, String level) throws ParseException, IOException {
		List<CheckIn> userCheckIns;
		Graph graph;

		for (UserProfile profile : usersProfiles) {
			userCheckIns = fileManager.getUserCheckInsCsv(profile.userId, false);
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
		// TODO: remove pedrada
		UserProfile profile = new UserProfile("578");
		usersProfiles.add(profile);
		profile = new UserProfile("842");
		usersProfiles.add(profile);
		profile = new UserProfile("1810");
		usersProfiles.add(profile);
	}

	private static void testUserSimilarity(FileManager mFileManager) throws ParseException, IOException {

		List<CheckIn> userCheckIns;
		Graph graph;

		userCheckIns = mFileManager.getUserCheckInsCsv("22", false);
		graph = getUserGraphFromCheckIns(userCheckIns);
		UserProfile profile22 = new UserProfile("22");
		profile22.addNewGraph("0", graph);
		usersProfiles.add(profile22);

		userCheckIns = mFileManager.getUserCheckInsCsv("578", false);
		graph = getUserGraphFromCheckIns(userCheckIns);
		UserProfile profile578 = new UserProfile("578");
		profile578.addNewGraph("0", graph);
		usersProfiles.add(profile578);

		userCheckIns = mFileManager.getUserCheckInsCsv("842", false);
		graph = getUserGraphFromCheckIns(userCheckIns);
		UserProfile profile842 = new UserProfile("842");
		profile842.addNewGraph("0", graph);
		usersProfiles.add(profile842);

		userCheckIns = mFileManager.getUserCheckInsCsv("1810", false);
		graph = getUserGraphFromCheckIns(userCheckIns);
		UserProfile profile1810 = new UserProfile("1810");
		profile1810.addNewGraph("0", graph);
		usersProfiles.add(profile1810);

		calculateSimilarities();

	}

	private static void calculateSimilarities() {
		for (int i = 0; i < usersProfiles.size(); i++) {
			for (int j = 0; j < usersProfiles.size(); j++) {
				if (i != j && j > i) {
					UserProfile p1 = usersProfiles.get(i);
					UserProfile p2 = usersProfiles.get(j);
					double seqScore = getUserSimilaritySequences(p1, p2, "0");
					double actScore = getUserSimilarityClusterActivity(p1, p2, "0");
					double finalScore = 0.6 * seqScore + 0.4 * actScore;
					p1.addSimilarityScore(p2.userId, finalScore);
					p2.addSimilarityScore(p1.userId, finalScore);
				}
			}
		}
	}

	private static void testSequenceMatching() {
		ClusterWithMean clusterA = new ClusterWithMean(2);
		clusterA.mId = 0;
		ClusterWithMean clusterB = new ClusterWithMean(2);
		clusterB.mId = 1;
		ClusterWithMean clusterC = new ClusterWithMean(2);
		clusterC.mId = 2;
		ClusterWithMean clusterD = new ClusterWithMean(2);
		clusterD.mId = 3;

		Calendar cal = Calendar.getInstance();
		Date now = new Date();

		cal.setTime(now);
		cal.add(Calendar.MINUTE, 60);
		Date oneHour = new Date(cal.getTimeInMillis());

		cal.setTime(now);
		cal.add(Calendar.MINUTE, 90);
		Date oneHourHalf = new Date(cal.getTimeInMillis());

		cal.setTime(now);
		cal.add(Calendar.MINUTE, 120);
		Date twoHour = new Date(cal.getTimeInMillis());

		cal.setTime(now);
		cal.add(Calendar.MINUTE, 144);
		Date twoFourHour = new Date(cal.getTimeInMillis());

		cal.setTime(now);
		cal.add(Calendar.MINUTE, 180);
		Date threeHour = new Date(cal.getTimeInMillis());

		cal.setTime(now);
		cal.add(Calendar.MINUTE, 240);
		Date fourHour = new Date(cal.getTimeInMillis());

		cal.setTime(now);
		cal.add(Calendar.MINUTE, 270);
		Date fourHalfHour = new Date(cal.getTimeInMillis());

		cal.setTime(now);
		cal.add(Calendar.MINUTE, 271);
		Date fourHalfHourTest = new Date(cal.getTimeInMillis());

		cal.setTime(now);
		cal.add(Calendar.MINUTE, 300);
		Date fiveHour = new Date(cal.getTimeInMillis());

		cal.setTime(now);
		cal.add(Calendar.MINUTE, 360);
		Date sixHour = new Date(cal.getTimeInMillis());

		cal.setTime(now);
		cal.add(Calendar.MINUTE, 408);
		Date sixEightHour = new Date(cal.getTimeInMillis());

		cal.setTime(now);
		cal.add(Calendar.MINUTE, 420);
		Date sevenHour = new Date(cal.getTimeInMillis());

		cal.setTime(now);
		cal.add(Calendar.MINUTE, 468);
		Date sevenEightHour = new Date(cal.getTimeInMillis());

		cal.setTime(now);
		cal.add(Calendar.MINUTE, 469);
		Date sevenEightHourTest = new Date(cal.getTimeInMillis());

		cal.setTime(now);
		cal.add(Calendar.MINUTE, 480);
		Date eightHour = new Date(cal.getTimeInMillis());

		Sequence test1 = new Sequence();
		test1.addVertexInfo(new VertexInfo(clusterA, now));
		test1.addVertexInfo(new VertexInfo(clusterA, oneHour));
		test1.addVertexInfo(new VertexInfo(clusterA, twoHour));
		test1.addVertexInfo(new VertexInfo(clusterA, threeHour));
		test1.addVertexInfo(new VertexInfo(clusterC, fourHour));
		test1.addVertexInfo(new VertexInfo(clusterC, fiveHour));
		test1.addVertexInfo(new VertexInfo(clusterC, sixHour));
		test1.addVertexInfo(new VertexInfo(clusterC, sevenHour));
		test1.addVertexInfo(new VertexInfo(clusterC, eightHour));

		Sequence test2 = new Sequence();
		test2.addVertexInfo(new VertexInfo(clusterA, new Date()));
		test2.addVertexInfo(new VertexInfo(clusterB, oneHour));
		test2.addVertexInfo(new VertexInfo(clusterC, fourHour));

		Sequence a = new Sequence();
		a.addVertexInfo(new VertexInfo(clusterA, new Date()));
		a.addVertexInfo(new VertexInfo(clusterB, oneHour));
		a.addVertexInfo(new VertexInfo(clusterC, threeHour));
		a.addVertexInfo(new VertexInfo(clusterA, fourHalfHour));
		a.addVertexInfo(new VertexInfo(clusterD, fourHalfHourTest));

		Sequence b = new Sequence();
		b.addVertexInfo(new VertexInfo(clusterC, new Date()));
		b.addVertexInfo(new VertexInfo(clusterA, twoHour));
		b.addVertexInfo(new VertexInfo(clusterB, fourHalfHour));
		b.addVertexInfo(new VertexInfo(clusterC, sixEightHour));
		b.addVertexInfo(new VertexInfo(clusterB, sevenEightHour));
		b.addVertexInfo(new VertexInfo(clusterD, sevenEightHourTest));

		Sequence x = new Sequence();
		x.addVertexInfo(new VertexInfo(clusterA, new Date()));
		x.addVertexInfo(new VertexInfo(clusterB, oneHourHalf));
		x.addVertexInfo(new VertexInfo(clusterC, twoHour));
		x.addVertexInfo(new VertexInfo(clusterA, threeHour));
		x.addVertexInfo(new VertexInfo(clusterB, fourHalfHour));
		x.addVertexInfo(new VertexInfo(clusterC, fourHalfHourTest));

		Sequence y = new Sequence();
		y.addVertexInfo(new VertexInfo(clusterA, new Date()));
		y.addVertexInfo(new VertexInfo(clusterD, oneHour));
		y.addVertexInfo(new VertexInfo(clusterB, oneHour));
		y.addVertexInfo(new VertexInfo(clusterC, twoFourHour));
		y.addVertexInfo(new VertexInfo(clusterA, new Date(twoFourHour.getTime() + fourHalfHour.getTime())));
		y.addVertexInfo(new VertexInfo(clusterB, new Date(twoFourHour.getTime() + sixEightHour.getTime())));
		y.addVertexInfo(new VertexInfo(clusterC, new Date(twoFourHour.getTime() + sevenEightHour.getTime())));

		Set<Sequence> set = new HashSet<>();
		set.add(test1);
		set.add(test2);

	}

	private static double getUserSimilaritySequences(UserProfile profileA, UserProfile profileB, String level) {
		if (!profileA.userId.equals(profileB.userId)) {
			Graph graphA = profileA.getGraphByLevel(level);
			Graph graphB = profileB.getGraphByLevel(level);

			long N1 = graphA.vertexes.size();
			long N2 = graphB.vertexes.size();

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
			double simmilarity = score * (1.0 / N1 * N2);
			return simmilarity;
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

			for (Map.Entry<Integer, Double> entry : percentageA.entrySet()) {
				if (percentageB.containsKey(entry.getKey())) {
					double aux = percentageB.get(entry.getKey());
					score += (aux < entry.getValue()) ? aux : entry.getValue();
				}
			}
			// System.out.println("activity score -> "+score);
			return score;
		}
		return 0;
	}

	private static Map<Integer, Long> createClusterTimeMap(Sequence seq) {
		Map<Integer, Long> toReturn = new HashMap<Integer, Long>();

		for (VertexInfo vi : seq.getClusters()) {
			if (toReturn.containsKey(vi.cluster.mId)) {
				long previousTime = toReturn.get(vi.cluster.mId);
				toReturn.put(vi.cluster.mId, previousTime + Math.abs(vi.leavTime.getTime() - vi.arrTime.getTime()));
			} else {
				toReturn.put(vi.cluster.mId, Math.abs(vi.leavTime.getTime() - vi.arrTime.getTime()));
			}
		}

		return toReturn;
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
		cluster.setMean(new DoubleArray(new double[] { 40.77761934847919,-73.92851136376517 }));
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
