package pt.utl.ist.meic.firebase.models;

public class ScalabilityMetrics {
	
	public long bytesUpload;
	public long bytesDownload;
	public int updates;
	
	public ScalabilityMetrics(long bytesUpload,long bytesDownload, int updates) {
		this.bytesUpload = bytesUpload;
		this.bytesDownload = bytesDownload;
		this.updates = updates;
	}

	@Override
	public String toString() {
		return "bytesUpload -> "+bytesUpload+" ; bytesDownload -> "+bytesDownload+" // updates -> "+updates;
	}
	
}
