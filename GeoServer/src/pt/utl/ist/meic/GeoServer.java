package pt.utl.ist.meic;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import utility.FileManager;
import ca.pfv.spmf.algorithms.clustering.distanceFunctions.DistanceCosine;
import ca.pfv.spmf.algorithms.clustering.distanceFunctions.DistanceFunction;
import ca.pfv.spmf.algorithms.clustering.kmeans.AlgoKMeans;
import ca.pfv.spmf.patterns.cluster.ClusterWithMean;
import ca.pfv.spmf.patterns.cluster.DoubleArray;

public class GeoServer {

	private static final int NUM_CLUSTERS = 5;// kmeans
	private static final String DELIMITER = ",";

	private static final int MATCHING_MAX_SEQ_LENGTH = 20;// analisar seqs no
															// maximo de 50
															// clusters
	private static final long TRANSITION_TIME_THRESHOLD = 2 * 60 * 60 * 1000;// 2
																				// horas

	private static List<ClusterWithMean> globalClusters;

	private static Map<String, UserProfile> usersProfiles;

	public static void main(String[] args) throws ParseException, IOException {

		//testSequenceMatching();

		initGlobalClustersList();
		// usersProfiles = new HashMap<String, UserProfile>();
		FileManager mFileManager = new FileManager();

		// mFileManager.stuff();

		List<CheckIn> userCheckIns = mFileManager.getUserCheckInsCsv("22", false);

		Graph graph = getUserGraphFromCheckIns(userCheckIns);
		graph.buildSequences();
		graph.printSequences();
		UserProfile profile22 = new UserProfile("22");
		profile22.addNewGraph("0", graph);

		userCheckIns = mFileManager.getUserCheckInsCsv("578", false);
		graph = getUserGraphFromCheckIns(userCheckIns);
		graph.buildSequences();
		graph.printSequences();
		UserProfile profile578 = new UserProfile("578");
		profile578.addNewGraph("0", graph);

		System.out.println("done");

		getUserSimilarity(profile22, profile578, "0");

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
		cal.add(Calendar.MINUTE, 270);
		Date fourHalfHour = new Date(cal.getTimeInMillis());

		cal.setTime(now);
		cal.add(Calendar.MINUTE, 271);
		Date fourHalfHourTest = new Date(cal.getTimeInMillis());

		cal.setTime(now);
		cal.add(Calendar.MINUTE, 408);
		Date sixEightHour = new Date(cal.getTimeInMillis());

		cal.setTime(now);
		cal.add(Calendar.MINUTE, 468);
		Date sevenEightHour = new Date(cal.getTimeInMillis());

		cal.setTime(now);
		cal.add(Calendar.MINUTE, 469);
		Date sevenEightHourTest = new Date(cal.getTimeInMillis());

		Sequence test1 = new Sequence();
		test1.addVertexInfo(new VertexInfo(clusterA, new Date()));
		test1.addVertexInfo(new VertexInfo(clusterA, oneHour));
		test1.addVertexInfo(new VertexInfo(clusterA, twoHour));

		Sequence test2 = new Sequence();
		test2.addVertexInfo(new VertexInfo(clusterA, new Date()));
		test2.addVertexInfo(new VertexInfo(clusterA, oneHour));
		test2.addVertexInfo(new VertexInfo(clusterA, twoHour));

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

		Set<Sequence> similarSequences = sequenceMatching(test1, test2, MATCHING_MAX_SEQ_LENGTH,
				TRANSITION_TIME_THRESHOLD);
		for (Sequence seq : similarSequences) {
			System.out.println(seq);
		}
	}

	private static void getUserSimilarity(UserProfile profileA, UserProfile profileB, String level) {
		Set<Sequence> seqsA = profileA.getGraphByLevel(level).mSequences;
		Set<Sequence> seqsB = profileB.getGraphByLevel(level).mSequences;

		for (Sequence seqA : seqsA) {
			for (Sequence seqB : seqsB) {
				sequenceMatching(seqA, seqB, MATCHING_MAX_SEQ_LENGTH, TRANSITION_TIME_THRESHOLD);
				// TODO: gerar score com as sequences devolvidas
			}
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
			if (step == 1) {
				sequenceSet = pruneSequences(sequenceSet);
			}
			step++;
		}
		sequenceSet = pruneSequences(sequenceSet);
		Set<Sequence> toReturn = new HashSet<Sequence>();

		System.out.println("adicionar para devolver sequencias normais");
		for (SequenceAuxiliar seq : sequenceSet) {
			toReturn.add(seq.toNormalSequence());
			System.out.println(seq);
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
		int maxLength = sequenceSet.iterator().next().mVertexes.size();
		for (SequenceAuxiliar seq : sequenceSet) {
			maxLength = (seq.mVertexes.size() > maxLength) ? seq.mVertexes.size() : maxLength;
		}
		final int maxLengthFinal = maxLength;// woooooooow amazing
		return sequenceSet.stream().filter(x -> x.mVertexes.size() == maxLengthFinal).collect(Collectors.toSet());
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
				System.out.println("estou a analisar " + v1 + " com " + v2);
				if (v1.vertex.mId != v2.vertex.mId && consequentIndexes(v1, v2)) {
					// se for tamanho 1, nao comparamos sequencias para o
					// mesmo cluster
					System.out.println("consequent and diferent!");
					// calcular o tempo perdido a ir de seq para aux em cada
					// sequencia original
					long travelTime1 = Math.abs(a.getClusters().get(v1.index1).date.getTime()
							- a.getClusters().get(v2.index1).date.getTime());
					long travelTime2 = Math.abs(b.getClusters().get(v1.index2).date.getTime()
							- b.getClusters().get(v2.index2).date.getTime());

					long delta = Math.abs(travelTime1 - travelTime2);
					if (delta <= transitionTimeThreshold) {
						SequenceAuxiliar result = new SequenceAuxiliar();
						result.mVertexes.add(v1);
						result.mVertexes.add(v2);
						System.out.println("aumentei seq Unitaria para :: " + result);
						toReturn.add(result);
					}
				}
			} else if (seq.mVertexes.size() > 1) {
				AuxiliarVertex v1 = seq.getLastVertex();
				AuxiliarVertex v2 = aux.getFirstVertex();
				System.out.println("estou a analisar " + v1 + " com " + v2);
				if (consequentIndexes(v1, v2)) {
					System.out.println("consequent!");
					long travelTime1;
					long travelTime2;
					long delta;
					if (v1.equals(v2)) {
						System.out.println("same! calcular delta com seguinte");
						travelTime1 = Math.abs(a.getClusters().get(v1.index1).date.getTime()
								- a.getClusters().get(aux.getSecondVertex().index1).date.getTime());
						travelTime2 = Math.abs(b.getClusters().get(v1.index2).date.getTime()
								- b.getClusters().get(aux.getSecondVertex().index2).date.getTime());

						delta = Math.abs(travelTime1 - travelTime2);
						if (delta <= transitionTimeThreshold) {
							SequenceAuxiliar result = new SequenceAuxiliar();
							result.mVertexes.addAll(seq.mVertexes);
							result.mVertexes.add(aux.getSecondVertex());
							System.out.println("aumentei seq para :: " + result);
							toReturn.add(result);
						}
					} else {
						System.out.println("nao encaixam! calcular delta");
						travelTime1 = Math.abs(a.getClusters().get(v1.index1).date.getTime()
								- a.getClusters().get(v2.index1).date.getTime());
						travelTime2 = Math.abs(b.getClusters().get(v1.index2).date.getTime()
								- b.getClusters().get(v2.index2).date.getTime());

						delta = Math.abs(travelTime1 - travelTime2);
						if (delta <= transitionTimeThreshold) {
							SequenceAuxiliar result = new SequenceAuxiliar();
							result.mVertexes.addAll(seq.mVertexes);
							result.mVertexes.add(v2);
							System.out.println("aumentei seq para :: " + result);
							toReturn.add(result);
						}
					}
				}
			}
		}
		return toReturn;

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
}
