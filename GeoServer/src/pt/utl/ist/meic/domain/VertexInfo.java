package pt.utl.ist.meic.domain;

import java.text.SimpleDateFormat;
import java.util.Date;

import ca.pfv.spmf.patterns.cluster.ClusterWithMean;

public class VertexInfo {

	public ClusterWithMean cluster;
	public Date date;

	public Date arrTime;
	public Date leavTime;
	
	public VertexInfo(ClusterWithMean cluster, Date date) {
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
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		return "[" + convertId(cluster.mId) + " @ "+sdf.format(date)+"]";
	}

	private String convertId(int id) {
		switch (id) {
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
