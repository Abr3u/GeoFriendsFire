package pt.utl.ist.meic.domain.managers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import pt.utl.ist.meic.domain.AuxiliarVertex;
import pt.utl.ist.meic.domain.COLLAB_TYPE;
import pt.utl.ist.meic.domain.Graph;
import pt.utl.ist.meic.domain.Sequence;
import pt.utl.ist.meic.domain.SequenceAuxiliar;
import pt.utl.ist.meic.domain.UserProfile;
import pt.utl.ist.meic.domain.VertexInfo;
import pt.utl.ist.meic.firebase.models.EventCategory;

public class SimilarityManager {

	private Map<String, UserProfile> id_userProfile;
	private double SEQ_SCORE_WEIGHT;
	private double ACT_SCORE_WEIGHT;

	public SimilarityManager(Map<String, UserProfile> id_userProfile, double SEQ_SCORE_WEIGHT,
			double ACT_SCORE_WEIGHT) {
		this.id_userProfile = id_userProfile;
		this.SEQ_SCORE_WEIGHT = SEQ_SCORE_WEIGHT;
		this.ACT_SCORE_WEIGHT = ACT_SCORE_WEIGHT;
	}

	
	//[START from events]
	public Map<String, UserProfile> calculateSimilaritiesFromEvents(int numClusters) {
		for (UserProfile up1 : id_userProfile.values()) {
			for (UserProfile up2 : id_userProfile.values()) {
				if (!up1.equals(up2) && up1.getSimilarityScoreByLayer(numClusters,up2.userId) == 0 && up1.getEvents().size() > 0
						&& up2.getEvents().size() > 0) {
					double score = collaborativeFilteringEventPercentage(up1.getEventPercentages(),
							up2.getEventPercentages());
					up1.addSimilarityScoreByLayer(numClusters,up2.userId, score);
					up2.addSimilarityScoreByLayer(numClusters,up1.userId, score);
				}
			}
		}
		return id_userProfile;
	}

	private double collaborativeFilteringEventPercentage(Map<EventCategory, Double> map1,
			Map<EventCategory, Double> map2) {
		List<Double> eventsA = map1.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(x -> x.getValue())
				.collect(Collectors.toList());

		List<Double> eventsB = map2.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(x -> x.getValue())
				.collect(Collectors.toList());

		double dotProduct = dotProduct(eventsA, eventsB);
		double magnitudeA = magnitude(eventsA);
		double magnitudeB = magnitude(eventsB);
		return dotProduct / (magnitudeA * magnitudeB);

	}
	
	//[END from events]
	//[START by count]

	public Map<String, UserProfile> calculateSimilaritiesByCount(int numClusters,int COMPARING_DISTANCE_THRESHOLD) {

		List<UserProfile> usersProfiles = new ArrayList<>(id_userProfile.values());

		for (int i = 0; i < usersProfiles.size(); i++) {
			for (int j = 0; j < usersProfiles.size(); j++) {
				if (i != j && j > i) {
					UserProfile p1 = usersProfiles.get(i);
					UserProfile p2 = usersProfiles.get(j);
					if (usersCloseEnough(p1.getGraph(), p2.getGraph(), COMPARING_DISTANCE_THRESHOLD)) {
						double score = getUserSimilarityByCount(p1, p2);
						p1.addSimilarityScoreByLayer(numClusters,p2.userId, score);
						p2.addSimilarityScoreByLayer(numClusters,p1.userId, score);
					}
				}
			}
			for (UserProfile profile : usersProfiles) {
				profile.normalizeSimilarityScoresByLayer(0.0, 1.0,numClusters);
			}
		}

		return id_userProfile;
	}

	private double getUserSimilarityByCount(UserProfile pA, UserProfile pB) {
		Graph graph1 = pA.getGraph();
		Graph graph2 = pB.getGraph();

		int size1 = graph1.vertexes.size();
		int size2 = graph2.vertexes.size();

		Map<Integer, Double> percentageA = graph1.cluster_percentage;
		Map<Integer, Double> percentageB = graph2.cluster_percentage;

		List<Double> countA = percentageA.entrySet().stream().sorted(Map.Entry.comparingByKey())
				.map(x -> x.getValue() * size1).collect(Collectors.toList());

		List<Double> countB = percentageB.entrySet().stream().sorted(Map.Entry.comparingByKey())
				.map(x -> x.getValue() * size2).collect(Collectors.toList());

		double score = 0d;
		for (int i = 0; i < countA.size(); i++) {
			score += (countA.get(i) < countB.get(i)) ? countA.get(i) : countB.get(i);
		}
		return score;
	}

	//[END by count]
	
	//[START geofriends]
	public Map<String, UserProfile> calculateSimilaritiesFromLocations(int numClusters,COLLAB_TYPE type,int COMPARING_DISTANCE_THRESHOLD,
			int MATCHING_MAX_SEQ_LENGTH, long TRANSITION_TIME_THRESHOLD, long SAME_TIME_DAY_THRESHOLD) {
		List<UserProfile> usersProfiles = new ArrayList<>(id_userProfile.values());

		if (this.SEQ_SCORE_WEIGHT > 0) {
			// calculate seqScore for each user pair
			for (int i = 0; i < usersProfiles.size(); i++) {
				for (int j = 0; j < usersProfiles.size(); j++) {
					if (i != j && j > i) {
						UserProfile p1 = usersProfiles.get(i);
						UserProfile p2 = usersProfiles.get(j);
						if (usersCloseEnough(p1.getGraph(), p2.getGraph(), COMPARING_DISTANCE_THRESHOLD)) {
							double seqScore = getUserSimilaritySequences(p1, p2, MATCHING_MAX_SEQ_LENGTH,
									TRANSITION_TIME_THRESHOLD, SAME_TIME_DAY_THRESHOLD);
							p1.addSimilarityScoreByLayer(numClusters,p2.userId, seqScore);
							p2.addSimilarityScoreByLayer(numClusters,p1.userId, seqScore);
						}
					}
				}
			}
			// tranform seqScore [0-1]
			for (UserProfile profile : usersProfiles) {
				profile.normalizeSimilarityScoresByLayer(0.0, 1.0,numClusters);
			}
		}
		if (this.ACT_SCORE_WEIGHT > 0) {
			// take into account activity Score
			for (int i = 0; i < usersProfiles.size(); i++) {
				for (int j = 0; j < usersProfiles.size(); j++) {
					if (i != j && j > i) {
						UserProfile p1 = usersProfiles.get(i);
						UserProfile p2 = usersProfiles.get(j);
						double finalScore;
						double actScore = getUserSimilarityClusterActivity(p1, p2,type);
						// System.out.println("actScore "+actScore);

						double seqScore1 = p1.getSimilarityScoreByLayer(numClusters,p2.userId);
						finalScore = calculateFinalScore(seqScore1, actScore);
						p1.addSimilarityScoreByLayer(numClusters,p2.userId, finalScore);

						double seqScore2 = p2.getSimilarityScoreByLayer(numClusters,p1.userId);
						finalScore = calculateFinalScore(seqScore2, actScore);
						p2.addSimilarityScoreByLayer(numClusters,p1.userId, finalScore);
					}
				}
			}
		}

		return id_userProfile;
	}

	private boolean usersCloseEnough(Graph g1, Graph g2, int COMPARING_DISTANCE_THRESHOLD) {
		return distanceBetween(g1.mCluster.getmean().get(0), g2.mCluster.getmean().get(0), g1.mCluster.getmean().get(1),
				g2.mCluster.getmean().get(1)) < COMPARING_DISTANCE_THRESHOLD;
	}

	// returns in meters
	private double distanceBetween(double lat1, double lat2, double lon1, double lon2) {

		final int R = 6371; // Radius of the earth

		Double latDistance = Math.toRadians(lat2 - lat1);
		Double lonDistance = Math.toRadians(lon2 - lon1);
		Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
		Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double distance = R * c * 1000; // convert to meters

		return distance;
	}

	//
	// [START SEQUENCE MATCHING]
	//

	private double getUserSimilaritySequences(UserProfile profileA, UserProfile profileB, int MATCHING_MAX_SEQ_LENGTH,
			long TRANSITION_TIME_THRESHOLD, long SAME_TIME_DAY_THRESHOLD) {
		if (!profileA.userId.equals(profileB.userId)) {
			Graph graphA = profileA.getGraph();
			Graph graphB = profileB.getGraph();

			// transformar seqs em seqs aggregadas e ir buscar as top N
			Set<Sequence> seqsA = graphA.getTopNSequences(50, graphA.getAggregatedSeqs());
			Set<Sequence> seqsB = graphB.getTopNSequences(50, graphB.getAggregatedSeqs());

			Set<SequenceAuxiliar> foundSeqs = new HashSet<SequenceAuxiliar>();

			double score = 0;
			for (Sequence seqA : seqsA) {
				for (Sequence seqB : seqsB) {
					foundSeqs.addAll(sequenceMatching(seqA, seqB, MATCHING_MAX_SEQ_LENGTH, TRANSITION_TIME_THRESHOLD,
							SAME_TIME_DAY_THRESHOLD));
				}
			}

			// respect privacy settings
			if (!profileA.crossings && !profileB.crossings) {
				// get longest seqs only
				foundSeqs = pruneSequences(foundSeqs);
			} else {
				// someone wants only crossings
				foundSeqs = foundSeqs.stream().filter(x -> x.sameTimeOfDay).collect(Collectors.toSet());
			}

			// convert auxiliar to normal Sequence
			Set<Sequence> normalSeqs = new HashSet<Sequence>();
			for (SequenceAuxiliar seq : foundSeqs) {
				normalSeqs.add(seq.toNormalSequence());
			}

			// calculate score
			for (Sequence s : normalSeqs) {
				double size = s.getClusters().size();
				score += size * Math.pow(2.0, size - 1);
			}
			return score;
		}
		return 0;
	}

	// returns set of maximum length similar sequences
	private Set<SequenceAuxiliar> sequenceMatching(Sequence seqA, Sequence seqB, int matchingMaxSeqLength,
			long transitionTimeThreshold, long sameTimeDayThreshold) {

		int step = 1;
		// add 1-length sequences into sequenceSet
		Set<SequenceAuxiliar> sequenceSet = add1LengthSequences(seqA, seqB);

		while (step <= matchingMaxSeqLength) {
			Set<SequenceAuxiliar> toAdd = new HashSet<SequenceAuxiliar>();
			for (SequenceAuxiliar seq : sequenceSet) {
				if (seq.mVertexes.size() == step) {
					// extend step-length seq to be (step + 1)-length
					toAdd.addAll(extendSequence(sequenceSet, seq, transitionTimeThreshold, sameTimeDayThreshold, seqA,
							seqB));
				}
			}
			sequenceSet.addAll(toAdd);
			// as 1-length estao todas contidas nas 2-legth de certeza
			if (step == 1) {
				sequenceSet = pruneSequences(sequenceSet);
			}
			step++;
		}

		return sequenceSet;

	}

	private Set<SequenceAuxiliar> add1LengthSequences(Sequence a, Sequence b) {
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

	private Set<SequenceAuxiliar> pruneSequences(Set<SequenceAuxiliar> sequenceSet) {
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

	private Set<SequenceAuxiliar> extendSequence(Set<SequenceAuxiliar> set, SequenceAuxiliar seq,
			long transitionTimeThreshold,long sameDayThreshold, Sequence a, Sequence b) {

		Set<SequenceAuxiliar> toReturn = new HashSet<SequenceAuxiliar>();

		Set<SequenceAuxiliar> auxSet = new HashSet<SequenceAuxiliar>(set);
		auxSet.remove(seq);

		AuxiliarVertex v1;
		AuxiliarVertex v2;
		long travelTime1;
		long travelTime2;
		long delta;

		for (SequenceAuxiliar aux : auxSet) {
			v1 = seq.getLastVertex();
			v2 = aux.getFirstVertex();
			
			if(v1.equals(v2) && aux.mVertexes.size()>=2) {
				v2 = aux.getSecondVertex();
			}
			
			if (consequentIndexes(v1, v2)) {

				travelTime1 = Math.abs(a.getClusters().get(v1.index1).leavTime.getTime()
						- a.getClusters().get(v2.index1).arrTime.getTime());
				travelTime2 = Math.abs(b.getClusters().get(v1.index2).leavTime.getTime()
						- b.getClusters().get(v2.index2).arrTime.getTime());

				delta = Math.abs(travelTime1 - travelTime2);

				Date dateV1 = a.getClusters().get(v1.index1).date;
				Date dateV2 = b.getClusters().get(v2.index2).date;

				if (delta <= transitionTimeThreshold && !shareIndexes(v1, v2)) {
					SequenceAuxiliar result = new SequenceAuxiliar();
					result.mVertexes.addAll(seq.mVertexes);
					result.mVertexes.add(v2);
					result.sameTimeOfDay = sameTimeOfDay(dateV1, dateV2,sameDayThreshold);
					toReturn.add(result);
				}
			}
		}
		return toReturn;
	}

	private boolean shareIndexes(AuxiliarVertex v1, AuxiliarVertex v2) {
		return v1.index1 == v2.index1 || v1.index2 == v2.index2;
	}

	private boolean consequentIndexes(AuxiliarVertex v1, AuxiliarVertex v2) {
		return v1.index1 <= v2.index1 && v1.index2 <= v2.index2;
	}

	private static boolean sameTimeOfDay(Date d1, Date d2, long sameTimeDayThreshold) {
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal1.setTime(d1);
		cal2.setTime(d2);

		boolean sameDay = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
				&& cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
		if (!sameDay) {
			return false;
		}

		long delta = Math.abs(d1.getTime() - d2.getTime());

		if (delta > sameTimeDayThreshold) {
			return false;
		}
		return true;
	}

	//
	// [END SEQUENCE MATCHING]
	//

	//
	// [START ACTIVITY MATCHING]
	//

	private double getUserSimilarityClusterActivity(UserProfile profileA, UserProfile profileB, COLLAB_TYPE type) {
		if (!profileA.userId.equals(profileB.userId)) {
			Graph graphA = profileA.getGraph();
			Graph graphB = profileB.getGraph();

			return collaborativeFilteringClusterPercentage(graphA, graphB,type);
		}
		return 0;
	}

	private double collaborativeFilteringClusterPercentage(Graph graphA, Graph graphB, COLLAB_TYPE type) {
		Map<Integer, Double> percentageA = graphA.cluster_percentage;
		Map<Integer, Double> percentageB = graphB.cluster_percentage;

		List<Double> actA = new LinkedList<Double>();
		List<Double> actB = new LinkedList<Double>();
		
		if (type.equals(COLLAB_TYPE.cosine)) {
			actA = percentageA.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(x -> x.getValue())
					.collect(Collectors.toList());

			actB = percentageB.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(x -> x.getValue())
					.collect(Collectors.toList());
		}

		if (type.equals(COLLAB_TYPE.pearson)) {
			//subtract mean to normalize activity prcts
			double meanA = percentageA.entrySet().stream().sorted(Map.Entry.comparingByKey()).mapToDouble(x -> x.getValue()).sum() / percentageA.size();
			double meanB = percentageB.entrySet().stream().sorted(Map.Entry.comparingByKey()).mapToDouble(x -> x.getValue()).sum() / percentageB.size();
			
			actA = percentageA.entrySet().stream().sorted(Map.Entry.comparingByKey())
					.map(x -> x.getValue() - meanA).collect(Collectors.toList());

			actB = percentageB.entrySet().stream().sorted(Map.Entry.comparingByKey())
					.map(x -> x.getValue() - meanB).collect(Collectors.toList());
		}

		double dotProduct = dotProduct(actA, actB);
		double magnitudeA = magnitude(actA);
		double magnitudeB = magnitude(actB);
		return dotProduct / (magnitudeA * magnitudeB);
	}

	private double dotProduct(List<Double> x, List<Double> y) {
		double dotProduct = 0;
		for (int i = 0; i < x.size(); i++) {
			dotProduct += (x.get(i) * y.get(i));
		}
		return dotProduct;
	}

	private double magnitude(List<Double> x) {
		return Math.sqrt(dotProduct(x, x));
	}

	//
	// [END ACTIVITY MATCHING]
	//

	private double calculateFinalScore(double seqScore, double actScore) {
		return SEQ_SCORE_WEIGHT * seqScore + ACT_SCORE_WEIGHT * actScore;
	}
	
	//[END GeoFriends]

}
