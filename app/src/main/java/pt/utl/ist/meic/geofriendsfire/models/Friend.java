package pt.utl.ist.meic.geofriendsfire.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Friend {

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
}
