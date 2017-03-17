package pt.utl.ist.meic.geofriendsfire.events;

import android.location.Location;

public class NewLocationEvent {
    private Location location;

    public NewLocationEvent(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}
