package pt.utl.ist.meic.geofriendsfire.models;


import com.google.firebase.database.Exclude;

import org.parceler.Parcel;

import java.util.HashMap;
import java.util.Map;

@Parcel
public class Message {
    public String from;
    public String to;
    public String toUsername;
    public String fromUsername;
    public String sentDate;
    public String content;

    public Message(){}

    public Message(String from,String fromUsername, String to,String toUsername, String sentDate,String content) {
        this.from = from;
        this.to = to;
        this.fromUsername = fromUsername;
        this.toUsername = toUsername;
        this.sentDate = sentDate;
        this.content = content;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("from", this.from);
        result.put("to", this.to);
        result.put("fromUsername", this.fromUsername);
        result.put("toUsername", this.toUsername);
        result.put("sentDate", this.sentDate);
        result.put("content", this.content);
        return result;
    }
}
