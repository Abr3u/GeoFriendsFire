package pt.utl.ist.meic.geofriendsfire.models;


import com.google.firebase.database.Exclude;

import org.parceler.Parcel;

import java.util.HashMap;
import java.util.Map;

@Parcel
public class Message {
    @Exclude
    public String ref;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof Message))
            return false;
        Message other = (Message) obj;
        return this.to.equals(other.to) &&
                this.from.equals(other.from) &&
                this.sentDate.equals(other.sentDate) &&
                this.content.equals(other.content);
    }

    @Override
    public int hashCode() {
        return to.hashCode()+from.hashCode()+sentDate.hashCode()+content.hashCode();
    }
}
