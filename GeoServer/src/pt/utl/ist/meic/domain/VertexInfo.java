package pt.utl.ist.meic.domain;

import java.util.Date;

import ca.pfv.spmf.patterns.cluster.ClusterWithMean;

public class VertexInfo {

	public ClusterWithMean cluster;
	public Date date;
	
	public VertexInfo(ClusterWithMean cluster,Date date) {
		this.cluster = cluster;
		this.date = date;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
            return false;
        if (!(obj instanceof VertexInfo))
            return false;
        VertexInfo other = (VertexInfo) obj;
        return this.cluster.mId == other.cluster.mId;
	}
	
	@Override
	public int hashCode() {
		return cluster.mId;
	}
	
	@Override
	public String toString() {
		//return "["+vertex.mId+"("+index1+";"+index2+")]";
		String id = "";
		switch(cluster.mId){
		case 0:
			id = "A";
		break;
		case 1:
			id = "B";
			break;
		case 2:
			id = "C";
			break;
		case 3:
			id = "D";
			break;
		}
		return "["+id+"]";
	}
	
}
