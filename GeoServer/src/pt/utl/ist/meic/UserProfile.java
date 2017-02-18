package pt.utl.ist.meic;

import java.util.HashMap;
import java.util.Map;

public class UserProfile {
	
	private Map<String,Graph> mGraphs;
	private String userId;
	
	public UserProfile(String userId) {
		this.userId = userId;
		mGraphs = new HashMap<String,Graph>();
	}

	public void addNewGraph(String level, Graph graph){
		this.mGraphs.put(level, graph);
	}
	
}
