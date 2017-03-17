package pt.utl.ist.meic.geofriendsfire.events;

import java.util.Map;

public class NewResidentDomainEvent {
    private Map<String,Double> resiDomain;

    public NewResidentDomainEvent(Map<String, Double> resiDomain) {
        this.resiDomain = resiDomain;
    }

    public Map<String, Double> getResiDomain() {
        return resiDomain;
    }
}
