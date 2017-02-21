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
		//return "["+vertex.mId+"("+index1+";"+index2+")]";
		String id = "";
		switch(vertex.mId){
		case 0:
			id = "A";
		break;
		case 1:
			id = "B";
			break;
		case 2:
			id = "C";
			break;
		}
		return "["+id+"("+index1+";"+index2+")]";
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
