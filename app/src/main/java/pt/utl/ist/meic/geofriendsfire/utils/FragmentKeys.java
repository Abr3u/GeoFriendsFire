package pt.utl.ist.meic.geofriendsfire.utils;

/**
 * Created by ricar on 11/02/2017.
 */

public enum FragmentKeys {
    EventsNearbyMap,
    EventsNearby,
    MyEvents,
    EventDetailsMap,
    CreateEvent,
    Friends,
    Test;


    public String getPageTitle(){
        switch (this.ordinal()){
            case 1:
                return "Events Nearby";
            case 2:
                return "My Events";
        }
        return "";
    }
}
