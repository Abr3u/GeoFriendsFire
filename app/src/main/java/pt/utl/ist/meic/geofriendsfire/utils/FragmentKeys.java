package pt.utl.ist.meic.geofriendsfire.utils;


public enum FragmentKeys {
    EventsNearbyMap,
    EventsNearby,
    MyEvents,
    EventDetailsMap,
    Friends;


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
