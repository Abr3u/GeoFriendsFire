package pt.utl.ist.meic.geofriendsfire.utils;


public enum FragmentKeys {
    EventsNearbyMap,
    EventsNearby,
    MyEvents,
    EventDetailsMap,
    Test,
    Friends,
    FriendsSuggestions,
    FriendSearch,
    MessagesReceived,
    MessagesSent,
    MessageDetails;


    public String getPageTitle(){
        switch (this.ordinal()){
            case 1:
                return "Events Nearby";
            case 2:
                return "My Events";
            case 5:
                return "Friends";
            case 6:
                return "Suggestions";
            case 7:
                return "Search";
            case 8:
                return "Inbox";
            case 9:
                return "Sent";
        }
        return "";
    }
}
