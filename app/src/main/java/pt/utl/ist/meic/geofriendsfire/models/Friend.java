package pt.utl.ist.meic.geofriendsfire.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import org.parceler.Parcel;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
@Parcel
public class Friend {

    @Exclude
    public String ref;
    public String username;
    public Double score;

    public Friend() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public Friend(String username, Double score) {
        this.username = username;
        this.score = score;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("score", score);
        return result;
    }

    //reversed a la pata
    public static Comparator<Friend> getComparatorScore()
    {
        Comparator comp = new Comparator<Friend>(){
            @Override
            public int compare(Friend f1, Friend f2)
            {
                return f1.score.compareTo(f2.score) * -1;
            }
        };
        return comp;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof Friend))
            return false;
        Friend other = (Friend) obj;
        return username.equals(other.username);
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }
}
