package pt.utl.ist.meic.geofriendsfire.utils;


public enum FragmentKeys {
    EventsNearbyMap,
    EventsNearby,
    MyEvents,
    EventDetailsMap,
    Friends,
    FriendsSuggestions,
    FriendSearch,
    MessagesReceived,
    MessagesSent;


    public String getPageTitle(){
        switch (this.ordinal()){
            case 1:
                return "Events Nearby";
            case 2:
                return "My Events";
            case 4:
                return "Friends";
            case 5:
                return "Suggestions";
            case 6:
                return "Search";
            case 7:
                return "Inbox";
            case 8:
                return "Sent";
        }
        return "";
    }
}
