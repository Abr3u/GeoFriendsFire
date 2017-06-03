package pt.utl.ist.meic.firebase.models;

public class ScalabilityMetrics {
	
	public long bytesSpent;
	public int updates;
	
	public ScalabilityMetrics(long bytesSpent, int updates) {
		this.bytesSpent = bytesSpent;
		this.updates = updates;
	}

	@Override
	public String toString() {
		return "bytes -> "+bytesSpent+" // updates -> "+updates;
	}
	
}
