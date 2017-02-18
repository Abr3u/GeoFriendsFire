package pt.utl.ist.meic;

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
	
}
