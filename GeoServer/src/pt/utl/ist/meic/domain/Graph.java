package pt.utl.ist.meic.domain;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ca.pfv.spmf.patterns.cluster.ClusterWithMean;

public class Graph {

	private static final int MIN_SEQUENCE_CHECKINS = 3;

	private final long MEANINGLESS_THRESHOLD_MILI = 20 * 1000;// 20 secs

	public List<VertexInfo> vertexes;
	public Set<Sequence> mSequences;

	public Map<Integer, Double> cluster_percentage;
	
	public ClusterWithMean mCluster;//cluster with highest activity

 	public Graph(int numClusters) {
		vertexes = new ArrayList<VertexInfo>();
		mSequences = new HashSet<Sequence>();
		cluster_percentage = new HashMap<Integer, Double>(){{
			for(int i=0;i<numClusters;i++){
				this.put(i, 0d);
			}
			}};
	}

	public void addVertex(VertexInfo vertex) {
		this.vertexes.add(vertex);
	}

	public void getGraphContentString() {
		for (VertexInfo v : vertexes) {
			System.out.println("cluster -> " + v.cluster.mId);
			System.out.println("date -> " + v.date);
		}
	}

	public void buildSequences() {
		getDailySequences();
		removeSequencesUniCluster();
		removeShortSequences();
	}

	public void buildPercentages() {
		int total = vertexes.size();
		for (VertexInfo vi : vertexes) {
			if (cluster_percentage.containsKey(vi.cluster.mId)) {
				cluster_percentage.put(vi.cluster.mId, cluster_percentage.get(vi.cluster.mId) + 1);
			} else {
				cluster_percentage.put(vi.cluster.mId, 1d);
			}
		}
		for (Map.Entry<Integer, Double> entry : cluster_percentage.entrySet()) {
			cluster_percentage.put(entry.getKey(), entry.getValue() / total);
		}
		
		//set mCluster as the cluster with highest activity
		int maxActivityId = cluster_percentage.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
		for(VertexInfo vi : vertexes){
			if(vi.cluster.mId == maxActivityId){
				mCluster = vi.cluster;
			}
		}
		
	}

	private void removeShortSequences() {
		mSequences = mSequences.stream().filter(s -> s.getClusters().size() >= MIN_SEQUENCE_CHECKINS)
				.collect(Collectors.toSet());
		// System.out.println(mSequences.size()+" big enough");
	}

	private void removeSequencesUniCluster() {
		List<Sequence> toRemove = new ArrayList<Sequence>();
		for (Sequence seq : mSequences) {
			if (seq.getClusters().size() > 0) {
				VertexInfo vi = seq.getClusters().get(0);
				if (seq.getClusters().stream().allMatch(vi::equals)) {
					toRemove.add(seq);
				}
			}
		}
		mSequences.removeAll(toRemove);
		// System.out.println(mSequences.size()+" multi cluster");
	}

	private void getDailySequences() {
		int count = 0;
		int i, j;
		for (i = 0; i < vertexes.size(); i++) {
			for (j = i + 1; j < vertexes.size(); j++) {
				VertexInfo current = vertexes.get(i);
				VertexInfo next = vertexes.get(j);

				Calendar cal1 = Calendar.getInstance();
				Calendar cal2 = Calendar.getInstance();
				cal1.setTime(current.date);
				cal2.setTime(next.date);
				boolean sameDay = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
						&& cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);

				if (!sameDay) {
					// estes dois pontos nao sao do mesmo dia -> criar uma
					// sequencia do conjunto de pontos entre eles
					Sequence sequence = new Sequence();
					count++;
					int startIndex = i;
					// por do fim para o inicio porque o dataset está ordenado
					// ao contrario
					for (i = j; i >= startIndex; i--) {
						// se os clusters forem diferentes, adiciona checkIn
						// se os clusters forem iguais, tem de ter passado pelo
						// menos um threshold de tempo entre os checkIns
						if ((!current.cluster.equals(next.cluster)) || ((current.cluster.equals(next.cluster)
								&& current.date.getTime() - next.date.getTime() > MEANINGLESS_THRESHOLD_MILI))) {
							sequence.addVertexInfo(vertexes.get(i));
						}
					}
					mSequences.add(sequence);
					// avançar o current para o sitio do next (frente) e comecar
					// a verificar se sao do mesmo dia outra vez, a partir desse
					// ponto
					i = j;
				}
			}
		}
		// System.out.println("criei " + count + " sequences");
		// System.out.println(mSequences.size() + " different");
	}

	public Double getPercentageByClusterId(Integer cid) {
		return this.cluster_percentage.get(cid);
	}

	public Set<Sequence> getTopNSequences(int size) {
		return this.mSequences.stream().sorted(Comparator.reverseOrder()).limit(size).collect(Collectors.toSet());
	}

	public Set<Sequence> getTopNSequences(int size, Set<Sequence> toFilter) {
		return toFilter.stream().sorted(Comparator.reverseOrder()).limit(size).collect(Collectors.toSet());
	}

	public void printSequences() {
		System.out.println("TOTAL DE SEQUENCIAS -> " + mSequences.size());
		int i = 0;
		for (Sequence seq : mSequences) {
			System.out.println("sequence " + i + " " + getAggregatedSeqString(seq));
			// System.out.println("sequence" + i + " -> v:" +
			// seq.getClusters().size());
			i++;
		}
	}

	private static String getAggregatedSeqString(Sequence seq) {
		String result = "";
		int count = 0;
		int lastId = seq.getClusters().get(0).cluster.mId;
		for (int i = 0; i < seq.getClusters().size(); i++) {
			VertexInfo vi = seq.getClusters().get(i);
			if (vi.cluster.mId == lastId) {
				count++;
			} else {
				result += "-> [" + convertId(lastId) + "(" + count + ")]";
				lastId = vi.cluster.mId;
				count = 1;
			}
		}
		// adicionar o ultimo a mao porque nao cai no else
		result += "-> [" + convertId(lastId) + "(" + count + ")]";
		return result;
	}

	public Set<Sequence> getAggregatedSeqs() {
		Set<Sequence> toReturn = new HashSet<Sequence>();
		for (Sequence seq : mSequences) {
			int lastId = -1;
			Sequence seqaux = new Sequence();
			for (VertexInfo vi : seq.getClusters()) {
				if (vi.cluster.mId != lastId) {
					lastId = vi.cluster.mId;
					vi.arrTime = vi.date;
					vi.leavTime = vi.date;
					seqaux.addVertexInfo(vi);
				} else {
					seqaux.getClusters().get(seqaux.getClusters().size() - 1).leavTime = vi.date;
				}
			}
			toReturn.add(seqaux);

		}
		SimpleDateFormat df = new SimpleDateFormat("HH:mm");
		String builder = "";
		for (Sequence seq : toReturn) {
			builder = "";
			for (VertexInfo vi : seq.getClusters()) {
				builder += "-> [(" + convertId(vi.cluster.mId) + ") a:" + df.format(vi.arrTime) + " l:"
						+ df.format(vi.leavTime) + "]";
			}
			// System.out.println("new aggr seq "+builder);
		}
		return toReturn;
	}

	public Set<Sequence> getAggregatedSeqs(Set<Sequence> toAggregate) {
		Set<Sequence> toReturn = new HashSet<Sequence>();
		for (Sequence seq : toAggregate) {
			int lastId = -1;
			Sequence seqaux = new Sequence();
			for (VertexInfo vi : seq.getClusters()) {
				if (vi.cluster.mId != lastId) {
					lastId = vi.cluster.mId;
					vi.arrTime = vi.date;
					vi.leavTime = vi.date;
					seqaux.addVertexInfo(vi);
				} else {
					seqaux.getClusters().get(seqaux.getClusters().size() - 1).leavTime = vi.date;
				}
			}
			toReturn.add(seqaux);

		}
		SimpleDateFormat df = new SimpleDateFormat("HH:mm");
		String builder = "";
		for (Sequence seq : toReturn) {
			for (VertexInfo vi : seq.getClusters()) {
				builder += "-> [(" + convertId(vi.cluster.mId) + ") a:" + df.format(vi.arrTime) + " l:"
						+ df.format(vi.leavTime) + "]";
			}
		}
		System.out.println(builder);
		return toReturn;
	}

	private static String convertId(int lastId) {
		switch (lastId) {
		case 0:
			return "A";
		case 1:
			return "B";
		case 2:
			return "C";
		case 3:
			return "D";
		case 4:
			return "E";
		default:
			return "NoID";
		}

	}

}
