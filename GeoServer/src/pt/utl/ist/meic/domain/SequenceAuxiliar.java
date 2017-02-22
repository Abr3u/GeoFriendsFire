package pt.utl.ist.meic.domain;

import java.util.ArrayList;
import java.util.List;

public class SequenceAuxiliar {

	public List<AuxiliarVertex> mVertexes;

	public SequenceAuxiliar() {
		this.mVertexes = new ArrayList<AuxiliarVertex>();
	}

	public Sequence toNormalSequence() {
		Sequence seq = new Sequence();

		for (AuxiliarVertex vertex : mVertexes) {
			seq.addVertexInfo(new VertexInfo(vertex.vertex, vertex.date));
		}

		return seq;
	}
	
	
	@Override
	public int hashCode() {
		int res = 0;
		for (int i = 0; i < mVertexes.size(); i++) {
			res = res + mVertexes.get(i).hashCode();
		}
		return res;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof SequenceAuxiliar))
			return false;
		SequenceAuxiliar other = (SequenceAuxiliar) obj;
		if (other.mVertexes.size() != this.mVertexes.size()) {
			return false;
		}
		for (int i = 0; i < this.mVertexes.size(); i++) {
			if (this.mVertexes.get(i).vertex.hashCode() != other.mVertexes.get(i).vertex.hashCode()) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public String toString() {
		String toReturn = "";
		for(AuxiliarVertex v : mVertexes){
			toReturn += v.toString()+"->";
		}
		return toReturn;
	}
	
	public AuxiliarVertex getLastVertex(){
		return this.mVertexes.get(this.mVertexes.size()-1);
	}
	
	public AuxiliarVertex getFirstVertex(){
		return this.mVertexes.get(0);
	}

	public AuxiliarVertex getSecondVertex(){
		return this.mVertexes.get(1);
	}
	
}
