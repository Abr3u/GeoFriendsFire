package pt.utl.ist.meic.firebase.models;

import ca.pfv.spmf.patterns.cluster.ClusterWithMean;

public class FirebaseCluster {
	
	public ClusterWithMean mean;
	public int size;
	public double sizePrct;
	
	public FirebaseCluster(ClusterWithMean mean, int size, double sizePrct) {
		this.mean = mean;
		this.size = size;
		this.sizePrct = sizePrct;
	}
	
	public FirebaseCluster() {
	}

	@Override
	public String toString() {
		return "FirebaseCluster size "+size+" sizePrct "+sizePrct+"; mean "+mean.getmean().get(0)+"::"+mean.getmean().get(1);
	}
	
}
