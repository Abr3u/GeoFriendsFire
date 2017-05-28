package pt.utl.ist.meic.firebase.models;

import java.util.HashMap;
import java.util.Map;

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
	
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("authorId", this.authorId);
        result.put("authorName", this.authorName);
        result.put("description", this.description);
        result.put("category", this.category);
        result.put("creationDate",this.creationDate);
        return result;
    }

}
