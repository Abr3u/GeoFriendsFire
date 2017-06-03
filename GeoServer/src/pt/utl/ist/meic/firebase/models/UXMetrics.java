package pt.utl.ist.meic.firebase.models;

public class UXMetrics {
	
	public long timeUntilFirst;
	
	public UXMetrics(long timeUntilFirst) {
		this.timeUntilFirst = timeUntilFirst;
	}

	@Override
	public String toString() {
		return "timeUntilFirst -> "+timeUntilFirst;
	}
	
}
