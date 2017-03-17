package pt.utl.ist.meic.geofriendsfire.events;

import pt.utl.ist.meic.geofriendsfire.models.Event;

/**
 * Created by ricar on 17/03/2017.
 */

public class DeletedEvent {
    private Event deleted;

    public DeletedEvent(Event deleted) {
        this.deleted = deleted;
    }
    public Event getDeleted() {
        return deleted;
    }
}
