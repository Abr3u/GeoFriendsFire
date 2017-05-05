package pt.utl.ist.meic.firebase.models;

public class Event {
	
	public String id;
	public String authorId;
	public String authorName;
	public EventCategory category;
	public String creationDate;
	public String description;
	
	public Event(){}
	
	public Event(String id, String authorId, String authorName, EventCategory category, String creationDate, String description) {
		this.id = id;
		this.authorId = authorId;
		this.authorName = authorName;
		this.category = category;
		this.creationDate = creationDate;
		this.description = description;
	}

	@Override
	public String toString() {
		return "Event [id=" + id + ", authorId=" + authorId + ", authorName=" + authorName + ", category=" + category
				+ ", creationDate=" + creationDate + ", description=" + description + "]";
	}

}
