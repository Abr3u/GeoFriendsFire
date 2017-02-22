package pt.utl.ist.meic.domain;

import java.util.Date;

import ca.pfv.spmf.patterns.cluster.ClusterWithMean;

public class AuxiliarVertex {
	
	public ClusterWithMean vertex;
	public Date date;
	
	public int index1;
	public int index2;
	
	public AuxiliarVertex(ClusterWithMean vertex,int index1, int index2,Date date) {
		this.vertex = vertex;
		this.index1 = index1;
		this.index2 = index2;
		this.date = date;
	}
	
	@Override
	public String toString() {
		return "["+convertId(vertex.mId)+"("+index1+";"+index2+")]";
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
	
	@Override
	public int hashCode() {
		return vertex.mId + index1 + index2;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
            return false;
        if (!(obj instanceof AuxiliarVertex))
            return false;
        AuxiliarVertex other = (AuxiliarVertex) obj;
        return this.vertex.mId == other.vertex.mId
        		&& this.index1 == other.index1
        		&& this.index2 == other.index2;
	}
	
}
