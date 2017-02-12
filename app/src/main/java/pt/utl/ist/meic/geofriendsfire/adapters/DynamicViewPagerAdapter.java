package pt.utl.ist.meic.geofriendsfire.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.nakama.arraypageradapter.ArrayFragmentStatePagerAdapter;

import java.util.ArrayList;

import pt.utl.ist.meic.geofriendsfire.fragments.CreateEventFragment;
import pt.utl.ist.meic.geofriendsfire.fragments.EventDetailsMapFragment;
import pt.utl.ist.meic.geofriendsfire.fragments.EventsNearbyListFragment;
import pt.utl.ist.meic.geofriendsfire.fragments.MapFragment;
import pt.utl.ist.meic.geofriendsfire.fragments.MyEventsListFragment;
import pt.utl.ist.meic.geofriendsfire.models.Event;
import pt.utl.ist.meic.geofriendsfire.utils.FragmentKeys;

public class DynamicViewPagerAdapter extends ArrayFragmentStatePagerAdapter<FragmentKeys> {

    private Event mEvent;

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
            case EventDetailsMap:
                EventDetailsMapFragment frag3 = new EventDetailsMapFragment();
                frag3.setEvent(mEvent);
                return frag3;
            case CreateEvent:
                return new CreateEventFragment();
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
