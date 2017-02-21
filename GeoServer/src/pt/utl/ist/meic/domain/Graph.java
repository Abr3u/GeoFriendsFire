package pt.utl.ist.meic.domain;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
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
	public Set<Sequence> mSequences;

	public Graph() {
		vertexes = new ArrayList<VertexInfo>();
		mSequences = new HashSet<Sequence>();
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
		for (Sequence seq : mSequences) {
			List<VertexInfo> toRemove = new ArrayList<VertexInfo>();
			for (i = 0; i < seq.getClusters().size(); i++) {
				for (j = i + 1; j < seq.getClusters().size(); j++) {
					VertexInfo current = seq.getClusters().get(i);
					VertexInfo next = seq.getClusters().get(j);

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
			seq.getClusters().removeAll(toRemove);
		}
		//System.out.println("depois de meaningless "+mSequences.size());
	}

	private void removeShortSequences() {
		mSequences = mSequences.stream().filter(s -> s.getClusters().size() >= MIN_SEQUENCE_CHECKINS)
				.collect(Collectors.toSet());
		System.out.println(mSequences.size()+" big enough");
	}

	private void removeSequencesUniCluster() {
		List<Sequence> toRemove = new ArrayList<Sequence>();
		for (Sequence seq : mSequences) {
			VertexInfo vi = seq.getClusters().get(0);
			if (seq.getClusters().stream().allMatch(vi::equals)) {
				toRemove.add(seq);
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
				
				Calendar cal1 = Calendar.getInstance();
				Calendar cal2 = Calendar.getInstance();
				cal1.setTime(current.date);
				cal2.setTime(next.date);
				boolean sameDay = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
				                  cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
				
				if (!sameDay) {
					// estes dois pontos nao sao do mesmo dia -> criar uma sequencia do conjunto de pontos entre eles
					Sequence sequence = new Sequence();
					count++;
					int startIndex = i;
					//por do fim para o inicio porque o dataset está ordenado ao contrario
					for (i = j; i >= startIndex; i--) {
						//se os clusters forem diferentes, adiciona checkIn
						//se os clusters forem iguais, tem de ter passado pelo menos um threshold de tempo entre os checkIns
						if((!current.cluster.equals(next.cluster)) || 
								((current.cluster.equals(next.cluster) && current.date.getTime() - next.date.getTime() > MEANINGLESS_THRESHOLD_MILI))){
							sequence.addVertexInfo(vertexes.get(i));
						}
					}
					mSequences.add(sequence);
					// avançar o current para o sitio do next (frente) e comecar
					// a verificar se sao do mesmo dia outra vez, a partir desse ponto
					i = j;
				}
			}
		}
		System.out.println("criei " + count + " sequences");
		System.out.println(mSequences.size() + " different");
	}

	
	public Set<Sequence> getTopNSequences(int size) {
		return this.mSequences.stream()
				.sorted(Comparator.reverseOrder())
				.limit(size)
				.collect(Collectors.toSet());
	}
	
	
	public void printSequences() {
		System.out.println("TOTAL DE SEQUENCIAS -> "+mSequences.size());
		int i = 0;
		for (Sequence seq : mSequences) {
			System.out.println("sequence" + i + " -> v:" + seq.getClusters().size());
			i++;
		}
	}



	

}
