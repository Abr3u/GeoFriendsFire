package pt.utl.ist.meic.firebase.models;

public class User {

	public String id;
	public String username;
	public String suggestions;
	
	public User(){
	}
	
	public User(String id, String username,String suggestions) {
		this.id = id;
		this.username = username;
		this.suggestions = suggestions;
	}
	
	@Override
	public String toString() {
		return "User "+username+" // suggestions: "+suggestions;
	}
	
}
