package pt.utl.ist.meic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ca.pfv.spmf.patterns.cluster.ClusterWithMean;

public class Graph {

	private static final int MIN_SEQUENCE_CHECKINS = 3;

	private final long TWO_DAYS_IN_MILI = 48 * 60 * 60 * 1000;
	private final long MEANINGLESS_THRESHOLD_MILI = 20 * 1000;//20 secs

	public List<VertexInfo> vertexes;
	public Set<Graph> mSequences;

	public Graph() {
		vertexes = new ArrayList<VertexInfo>();
		mSequences = new HashSet<Graph>();
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
		//removeMeaninglessClustersFromSequences();
		removeShortSequences();
	}

	private void removeMeaninglessClustersFromSequences() {
		//System.out.println("antes de meaningless "+mSequences.size());
		int i, j;
		for (Graph seq : mSequences) {
			List<VertexInfo> toRemove = new ArrayList<VertexInfo>();
			for (i = 0; i < seq.vertexes.size(); i++) {
				for (j = i + 1; j < seq.vertexes.size(); j++) {
					VertexInfo current = seq.vertexes.get(i);
					VertexInfo next = seq.vertexes.get(j);

					long diffMilis = current.date.getTime() - next.date.getTime();
					if (current.equals(next) && diffMilis < MEANINGLESS_THRESHOLD_MILI) {
						// checkIn no mesmo cluster em menos de threshold
						toRemove.add(next);
						// avançar o current para o sitio do next (frente), remover next e
						// comecar a contar outra vez a partir desse ponto
						i = j;
					}
				}
			}
			seq.vertexes.removeAll(toRemove);
		}
		//System.out.println("depois de meaningless "+mSequences.size());
	}

	private void removeShortSequences() {
		mSequences = mSequences.stream().filter(g -> g.vertexes.size() >= MIN_SEQUENCE_CHECKINS)
				.collect(Collectors.toSet());
		System.out.println(mSequences.size()+" big enough");
	}

	private void removeSequencesUniCluster() {
		List<Graph> toRemove = new ArrayList<Graph>();
		for (Graph g : mSequences) {
			VertexInfo vi = g.vertexes.get(0);
			if (g.vertexes.stream().allMatch(vi::equals)) {
				toRemove.add(g);
			}
		}
		mSequences.removeAll(toRemove);
		System.out.println(mSequences.size()+" multi cluster");
	}

	private void getDailySequences() {
		int count = 0;
		int i, j;
		for (i = 0; i < vertexes.size(); i++) {
			for (j = i + 1; j < vertexes.size(); j++) {
				VertexInfo current = vertexes.get(i);
				VertexInfo next = vertexes.get(j);

				long diffMilis = current.date.getTime() - next.date.getTime();
				if (diffMilis > TWO_DAYS_IN_MILI) {
					// passou mais de dois dias entre estes dois pontos
					Graph sequence = new Graph();
					count++;
					int startIndex = i;
					//por do fim para o inicio porque o dataset está ordenado ao contrario
					for (i = j; i >= startIndex; i--) {
						sequence.addVertex(vertexes.get(i));
					}
					mSequences.add(sequence);
					// avançar o current para o sitio do next (frente) e comecar
					// a contar 24h outra vez a partir desse ponto
					i = j;
				}
			}
		}
		System.out.println("criei " + count + " sequences");
		System.out.println(mSequences.size() + " different");
	}

	@Override
	public int hashCode() {
		int res = 0;
		for (int i = 0; i < vertexes.size(); i++) {
			res = res + vertexes.get(i).hashCode();
		}
		return res;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof Graph))
			return false;
		Graph other = (Graph) obj;
		if (other.vertexes.size() != this.vertexes.size()) {
			return false;
		}
		for (int i = 0; i < this.vertexes.size(); i++) {
			if (!this.vertexes.get(i).equals(other.vertexes.get(i))) {
				return false;
			}
		}
		return true;
	}

	public void printSequences() {
		System.out.println("TOTAL DE SEQUENCIAS -> "+mSequences.size());
		int i = 0;
		for (Graph seq : mSequences) {
			System.out.println("sequence" + i + " -> v:" + seq.vertexes.size());
			i++;
		}
	}

}
