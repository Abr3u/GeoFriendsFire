package pt.utl.ist.meic.domain;

import java.util.ArrayList;
import java.util.List;

public class Sequence implements Comparable<Sequence> {

	private List<VertexInfo> mClusters;

	public Sequence() {
		mClusters = new ArrayList<>();
	}

	public void addVertexInfo(VertexInfo vi) {
		this.mClusters.add(vi);
	}

	public List<VertexInfo> getClusters() {
		return this.mClusters;
	}

	@Override
	public int compareTo(Sequence o) {
		if (this.mClusters.size() > o.mClusters.size()) {
			return 1;
		}
		if (this.mClusters.size() < o.mClusters.size()) {
			return -1;
		} else {
			return 0;
		}
	}

	@Override
	public int hashCode() {
		int res = 0;
		for (int i = 0; i < mClusters.size(); i++) {
			res = res + mClusters.get(i).hashCode();
		}
		return res;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof Sequence))
			return false;
		Sequence other = (Sequence) obj;
		if (other.mClusters.size() != this.mClusters.size()) {
			return false;
		}
		for (int i = 0; i < this.mClusters.size(); i++) {
			if (!this.mClusters.get(i).equals(other.mClusters.get(i))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		String toReturn = "";
		for(VertexInfo v : mClusters){
			toReturn += v.toString()+"->";
		}
		return toReturn;
	}
	
}
