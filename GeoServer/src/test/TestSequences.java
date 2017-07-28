package test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;
import java.util.stream.Collectors;

import ca.pfv.spmf.patterns.cluster.ClusterWithMean;
import pt.utl.ist.meic.domain.AuxiliarVertex;
import pt.utl.ist.meic.domain.Graph;
import pt.utl.ist.meic.domain.Sequence;
import pt.utl.ist.meic.domain.SequenceAuxiliar;
import pt.utl.ist.meic.domain.UserProfile;
import pt.utl.ist.meic.domain.VertexInfo;

public class TestSequences {

	private static final int MATCHING_MAX_SEQ_LENGTH = 20;// analisar seqs no
	// maximo de 20
	// clusters
	private static final long TRANSITION_TIME_THRESHOLD = 2 * 60 * 60 * 1000;// 2
	// horas

	private static final long SAME_TIME_DAY_THRESHOLD = 1 * 60 * 60 * 1000;// meia hora

	public static void testExtendSequence() {
		// setup
		Calendar calendar = Calendar.getInstance();

		// dates full
		Date now = new Date();
		calendar.setTime(now);
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		Date one = calendar.getTime();
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		Date two = calendar.getTime();
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		Date three = calendar.getTime();
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		Date four = calendar.getTime();
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		Date five = calendar.getTime();
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		Date six = calendar.getTime();
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		Date seven = calendar.getTime();
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		Date eight = calendar.getTime();
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		Date nine = calendar.getTime();
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		Date ten = calendar.getTime();
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		Date eleven = calendar.getTime();

		// dates halfs
		calendar.setTime(one);
		calendar.add(Calendar.MINUTE, 30);
		Date oneHalf = calendar.getTime();
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		Date twoHalf = calendar.getTime();
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		Date threeHalf = calendar.getTime();
		calendar.add(Calendar.HOUR_OF_DAY, 3);
		Date sixHalf = calendar.getTime();
		calendar.add(Calendar.HOUR_OF_DAY, 4);
		Date tenHalf = calendar.getTime();

		// auxiliar dates
		calendar.setTime(four);
		calendar.add(Calendar.MINUTE, 30);
		Date fourHalf = calendar.getTime();
		calendar.add(Calendar.MINUTE, 2);
		Date fourHalfDelta = calendar.getTime();
		calendar.add(Calendar.MINUTE, 28);

		calendar.setTime(fourHalf);
		calendar.add(Calendar.MINUTE, 138);
		Date sixEight = calendar.getTime();
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		Date sevenEight = calendar.getTime();
		calendar.add(Calendar.MINUTE, 2);
		Date sevenEightDelta = calendar.getTime();

		ClusterWithMean clusterA = new ClusterWithMean(0);
		clusterA.mId = 0;
		ClusterWithMean clusterB = new ClusterWithMean(0);
		clusterB.mId = 1;
		ClusterWithMean clusterC = new ClusterWithMean(0);
		clusterC.mId = 2;
		ClusterWithMean clusterD = new ClusterWithMean(0);
		clusterD.mId = 3;

		Graph ga = new Graph(3);
		Graph gb = new Graph(3);

		Sequence a = new Sequence();
		Sequence b = new Sequence();

		ga.addVertex(new VertexInfo(clusterA, now));
		ga.addVertex(new VertexInfo(clusterB, one));
		ga.addVertex(new VertexInfo(clusterC, nine));
		ga.addVertex(new VertexInfo(clusterA, ten));
		ga.addVertex(new VertexInfo(clusterD, eleven));

		gb.addVertex(new VertexInfo(clusterA, now));
		gb.addVertex(new VertexInfo(clusterB, one));
		gb.addVertex(new VertexInfo(clusterC, three));
		gb.addVertex(new VertexInfo(clusterA, four));
		gb.addVertex(new VertexInfo(clusterD, five));

		a.addVertexInfo(new VertexInfo(clusterA, now));
		a.addVertexInfo(new VertexInfo(clusterB, one));
		a.addVertexInfo(new VertexInfo(clusterC, nine));
		a.addVertexInfo(new VertexInfo(clusterA, ten));
		a.addVertexInfo(new VertexInfo(clusterD, eleven));
		ga.addSequence(a);

		b.addVertexInfo(new VertexInfo(clusterA, now));
		b.addVertexInfo(new VertexInfo(clusterB, one));
		b.addVertexInfo(new VertexInfo(clusterC, three));
		b.addVertexInfo(new VertexInfo(clusterA, four));
		b.addVertexInfo(new VertexInfo(clusterD, five));
		gb.addSequence(b);

		UserProfile alice = new UserProfile("alice");
		alice.crossings = false;
		UserProfile bob = new UserProfile("bob");
		bob.crossings = false;

		alice.setGraph(ga);
		bob.setGraph(gb);

		// transformar seqs em seqs aggregadas e ir buscar as top N
		Set<Sequence> seqsA = ga.getTopNSequences(5, ga.getAggregatedSeqs());
		Set<Sequence> seqsB = gb.getTopNSequences(5, gb.getAggregatedSeqs());

		Set<SequenceAuxiliar> foundSeqs = new HashSet<SequenceAuxiliar>();
		
		double score = 0;
		
		for (Sequence seqA : seqsA) {
			for (Sequence seqB : seqsB) {
				foundSeqs.addAll(sequenceMatching(seqA, seqB, MATCHING_MAX_SEQ_LENGTH,
						TRANSITION_TIME_THRESHOLD));
			}
		}
		System.out.println("Found seqs");
		foundSeqs.stream().forEach(System.out::println);

		//respect privacy settings 
		if (!alice.crossings && !bob.crossings) {
			// get longest seqs only
			foundSeqs = pruneSequences(foundSeqs);
			System.out.println("All sequences");
		} else {
			// someone wants only crossings
			foundSeqs = foundSeqs.stream().filter(x -> x.sameTimeOfDay).collect(Collectors.toSet());
			System.out.println("Only crossings");
		}
		
		// convert auxiliar to normal Sequence
		Set<Sequence> normalSeqs = new HashSet<Sequence>();
		for (SequenceAuxiliar seq : foundSeqs) {
			normalSeqs.add(seq.toNormalSequence());
		}
		
		//calculate score
		for (Sequence s : normalSeqs) {
			double size = s.getClusters().size();
			score += size * Math.pow(2.0, size - 1);
		}
		System.out.println("SCORE " + score);
		
		alice.addSimilarityScore(bob.userId, score);
		bob.addSimilarityScore(alice.userId, score);
	}

	public static void testSameTimeOfDay() {

		Calendar calendar = Calendar.getInstance();

		// dates full
		Date now = new Date();
		calendar.setTime(now);
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		Date one = calendar.getTime();
		calendar.add(Calendar.HOUR_OF_DAY, 1);

		// dates halfs
		calendar.setTime(one);
		calendar.add(Calendar.MINUTE, 30);
		Date oneHalf = calendar.getTime();
		calendar.add(Calendar.HOUR_OF_DAY, 1);

		calendar.setTime(oneHalf);
		calendar.add(Calendar.MINUTE, 1);
		Date oneHalfDelta = calendar.getTime();

		System.out.println(sameTimeOfDay(now, now));
		System.out.println(sameTimeOfDay(one, now));
		System.out.println(sameTimeOfDay(oneHalf, one));
		System.out.println(sameTimeOfDay(oneHalfDelta, one));

	}

	private static boolean sameTimeOfDay(Date d1, Date d2) {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		// System.out.println("STD "+sdf.format(d1)+" "+sdf.format(d2));
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal1.setTime(d1);
		cal2.setTime(d2);

		boolean sameDay = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
				&& cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
		if (!sameDay) {
			// System.out.println("FALSE");
			// System.out.println();
			return false;
		}

		long delta = Math.abs(d1.getTime() - d2.getTime());

		if (delta > SAME_TIME_DAY_THRESHOLD) {
			// System.out.println("FALSE");
			// System.out.println();
			return false;
		}
		// System.out.println("TRUE");
		// System.out.println();
		return true;
	}

	private static Set<SequenceAuxiliar> sequenceMatching(Sequence seqA, Sequence seqB, int matchingMaxSeqLength,
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

		return sequenceSet;

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

		AuxiliarVertex v1;
		AuxiliarVertex v2;
		long travelTime1;
		long travelTime2;
		long delta;

		for (SequenceAuxiliar aux : auxSet) {
			if (seq.mVertexes.size() == 1) {
				v1 = seq.getLastVertex();
				v2 = aux.getLastVertex();
				// System.out.println("estou a analisar " + v1 + " com " + v2);
				if (v1.vertex.mId != v2.vertex.mId && consequentIndexes(v1, v2)) {
					// se for tamanho 1, nao comparamos sequencias para o
					// mesmo cluster

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
						result.sameTimeOfDay = sameTimeOfDay(dateV1, dateV2);
						toReturn.add(result);
					}
				}
			} else if (seq.mVertexes.size() > 1) {
				v1 = seq.getLastVertex();
				v2 = aux.getFirstVertex();

				if (v1.equals(v2)) {

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
						result.sameTimeOfDay = sameTimeOfDay(dateV1, dateV2);
						toReturn.add(result);
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
}
