package pt.utl.ist.meic.geofriendsfire.events;

import pt.utl.ist.meic.geofriendsfire.models.Event;

/**
 * Created by ricar on 17/03/2017.
 */

public class NewNearbyEvent {
    private Event nearby;

    public NewNearbyEvent(Event nearby) {
        this.nearby = nearby;
    }
    public Event getNearby() {
        return nearby;
    }
}
