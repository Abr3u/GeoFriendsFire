package pt.utl.ist.meic;

public class Edge {
	public int from;
	public int to;

	public Edge() {
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
            return false;
        if (!(obj instanceof Edge))
            return false;
        Edge other = (Edge) obj;
        return this.from == other.from && this.to == other.to;
	}
	
	@Override
	public int hashCode() {
		return from * 31 + to;
	}
}
