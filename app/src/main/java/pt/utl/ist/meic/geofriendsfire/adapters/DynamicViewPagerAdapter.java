package pt.utl.ist.meic.geofriendsfire.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;

import com.nakama.arraypageradapter.ArrayFragmentStatePagerAdapter;

import java.util.ArrayList;

import pt.utl.ist.meic.geofriendsfire.fragments.EventsNearbyListFragment;
import pt.utl.ist.meic.geofriendsfire.fragments.FriendSearchFragment;
import pt.utl.ist.meic.geofriendsfire.fragments.FriendsFragment;
import pt.utl.ist.meic.geofriendsfire.fragments.FriendsSuggestionsFragment;
import pt.utl.ist.meic.geofriendsfire.fragments.MapFragment;
import pt.utl.ist.meic.geofriendsfire.fragments.MessagesReceivedFragment;
import pt.utl.ist.meic.geofriendsfire.fragments.MessagesSentFragment;
import pt.utl.ist.meic.geofriendsfire.fragments.MyEventsListFragment;
import pt.utl.ist.meic.geofriendsfire.models.Event;
import pt.utl.ist.meic.geofriendsfire.models.Message;
import pt.utl.ist.meic.geofriendsfire.utils.FragmentKeys;

public class DynamicViewPagerAdapter extends ArrayFragmentStatePagerAdapter<FragmentKeys> {

    private Event mEvent;
    private Message mMessage;
    private boolean isInbox;

    public DynamicViewPagerAdapter(FragmentManager fm, ArrayList<FragmentKeys> datas) {
        super(fm, datas);
    }

    @Override
    public Fragment getFragment(FragmentKeys item, int position) {
        switch (item) {
            case EventsNearbyMap:
                return new MapFragment();
            case EventsNearby:
                return new EventsNearbyListFragment();
            case MyEvents:
                return new MyEventsListFragment();
            case Friends:
                return new FriendsFragment();
            case FriendsSuggestions:
                return new FriendsSuggestionsFragment();
            case FriendSearch:
                return new FriendSearchFragment();
            case MessagesReceived:
                return new MessagesReceivedFragment();
            case MessagesSent:
                return new MessagesSentFragment();
        }
        //shouldn't happen so it's not in switch. just fail-safe
        return new MapFragment();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return getItem(position).getPageTitle();
    }

    public void setEventForDetails(Event event){
        this.mEvent = event;
    }

}
