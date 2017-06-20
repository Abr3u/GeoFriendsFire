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

	private static final long SAME_TIME_DAY_THRESHOLD = 2 * 60 * 60 * 1000;// 1
	// horas

	public static void testSameTimeOfDay() {
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
		ga.addVertex(new VertexInfo(clusterC, three));
		ga.addVertex(new VertexInfo(clusterA, fourHalf));
		ga.addVertex(new VertexInfo(clusterD, fourHalfDelta));

		gb.addVertex(new VertexInfo(clusterC, now));
		gb.addVertex(new VertexInfo(clusterA, two));
		gb.addVertex(new VertexInfo(clusterB, fourHalf));
		gb.addVertex(new VertexInfo(clusterC, sixEight));
		gb.addVertex(new VertexInfo(clusterB, sevenEight));
		gb.addVertex(new VertexInfo(clusterD, sevenEightDelta));

		a.addVertexInfo(new VertexInfo(clusterA, now));
		a.addVertexInfo(new VertexInfo(clusterB, one));
		a.addVertexInfo(new VertexInfo(clusterC, three));
		a.addVertexInfo(new VertexInfo(clusterA, fourHalf));
		a.addVertexInfo(new VertexInfo(clusterD, fourHalfDelta));
		ga.addSequence(a);

		b.addVertexInfo(new VertexInfo(clusterC, now));
		b.addVertexInfo(new VertexInfo(clusterA, two));
		b.addVertexInfo(new VertexInfo(clusterB, fourHalf));
		b.addVertexInfo(new VertexInfo(clusterC, sixEight));
		b.addVertexInfo(new VertexInfo(clusterB, sevenEight));
		b.addVertexInfo(new VertexInfo(clusterD, sevenEightDelta));
		gb.addSequence(b);

		UserProfile alice = new UserProfile("alice");
		UserProfile bob = new UserProfile("bob");

		alice.addNewGraph(0, ga);
		bob.addNewGraph(0, gb);

		// transformar seqs em seqs aggregadas e ir buscar as top N
		Set<Sequence> seqsA = ga.getTopNSequences(5, ga.getAggregatedSeqs());
		Set<Sequence> seqsB = gb.getTopNSequences(5, gb.getAggregatedSeqs());

		double score = 0;
		for (Sequence seqA : seqsA) {
			for (Sequence seqB : seqsB) {
				Set<Sequence> maxLengthSeqs = sequenceMatching(seqA, seqB, MATCHING_MAX_SEQ_LENGTH,
						TRANSITION_TIME_THRESHOLD);

				System.out.println("MaxSeq Found");
				maxLengthSeqs.stream().forEach(System.out::println);

				for (Sequence simseq : maxLengthSeqs) {
					double size = simseq.getClusters().size();
					score += size * Math.pow(2.0, size - 1);
				}
			}
		}

		System.out.println("SCORE " + score);
	}

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
					// System.out.println("consequent and diferent!");
					// calcular o tempo perdido a ir de seq para aux em cada
					// sequencia original
					travelTime1 = Math.abs(a.getClusters().get(v1.index1).leavTime.getTime()
							- a.getClusters().get(v2.index1).arrTime.getTime());
					travelTime2 = Math.abs(b.getClusters().get(v1.index2).leavTime.getTime()
							- b.getClusters().get(v2.index2).arrTime.getTime());

					delta = Math.abs(travelTime1 - travelTime2);
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
				v1 = seq.getLastVertex();
				v2 = aux.getFirstVertex();

				// System.out.println("estou a analisar " + v1 + " com " + v2);
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
				} else if (consequentIndexes(v1, v2) && !shareIndexes(v1, v2)) {
					// System.out.println("consequent e nao partilham");
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
		return toReturn;
	}

	private static boolean shareIndexes(AuxiliarVertex v1, AuxiliarVertex v2) {
		return v1.index1 == v2.index1 || v1.index2 == v2.index2;
	}

	private static boolean consequentIndexes(AuxiliarVertex v1, AuxiliarVertex v2) {
		return v1.index1 <= v2.index1 && v1.index2 <= v2.index2;
	}
}
