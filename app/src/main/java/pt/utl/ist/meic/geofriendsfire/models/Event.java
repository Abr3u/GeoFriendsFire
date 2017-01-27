package pt.utl.ist.meic.geofriendsfire.models;

import android.util.Log;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Event {

    public String author;
    public String description;
    public String category;

    public Event() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public Event(String author, String description, String category) {
        this.author = author;
        this.description = description;
        this.category = category;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("author", this.author);
        result.put("description", this.description);
        result.put("category", this.category);
        return result;
    }


    @Override
    public int hashCode() {
        return author.hashCode() + description.hashCode() + category.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof Event))
            return false;
        Event other = (Event) obj;
        if (!author.equals(other.author))
            return false;
        if (!description.equals(other.description))
            return false;
        if (!category.equals(other.category))
            return false;
        return true;
    }
}
