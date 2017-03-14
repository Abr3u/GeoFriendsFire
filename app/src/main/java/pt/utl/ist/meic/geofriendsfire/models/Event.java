package pt.utl.ist.meic.geofriendsfire.models;

import com.firebase.geofire.GeoLocation;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import org.parceler.Parcel;
import org.parceler.Transient;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
@Parcel
public class Event {
    @Exclude
    public double latitude;
    @Exclude
    public double longitude;
    @Exclude
    public String ref;
    public String authorId;
    public String authorName;
    public String description;
    public String category;
    public String creationDate;

    public Event() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public Event(String authorId,String author, String description, String category,String creationDate) {
        this.authorId = authorId;
        this.authorName = author;
        this.description = description;
        this.category = category;
        this.creationDate = creationDate;
    }

    public void setRef(String ref){
        this.ref = ref;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("authorId", this.authorId);
        result.put("authorName", this.authorName);
        result.put("description", this.description);
        result.put("category", this.category);
        result.put("creationDate",this.creationDate);
        return result;
    }

    @Override
    public String toString() {
        return "["+creationDate+"]"+"["+category+"]"+description+"::"+ authorName;
    }

    @Override
    public int hashCode() {
        return authorId.hashCode()+authorName.hashCode() + description.hashCode() + category.hashCode()+creationDate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof Event))
            return false;
        Event other = (Event) obj;
        return authorId.equals(other.authorId)
                && description.equals(other.description)
                && category.equals(other.category)
                && creationDate.equals(other.creationDate);
    }
}
