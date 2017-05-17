package pt.utl.ist.meic.firebase.models;

public class EvaluationMetrics {
	
	public long bytesSpent;
	public int updates;
	
	public EvaluationMetrics(long bytesSpent, int updates) {
		this.bytesSpent = bytesSpent;
		this.updates = updates;
	}

	@Override
	public String toString() {
		return "bytes -> "+bytesSpent+" // updates -> "+updates;
	}
	
}
