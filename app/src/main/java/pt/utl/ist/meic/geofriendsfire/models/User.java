package pt.utl.ist.meic.geofriendsfire.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class User {

    @Exclude
    public String ref;
    public String username;
    public String email;
    public String suggestions;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String username, String email,String suggestions) {
        this.username = username;
        this.email = email;
        this.suggestions = suggestions;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("email", email);
        result.put("suggestions",suggestions);
        return result;
    }
}
