package pt.utl.ist.meic.geofriendsfire.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.nakama.arraypageradapter.ArrayFragmentStatePagerAdapter;

import java.util.ArrayList;

import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.fragments.EventDetailsMapFragment;
import pt.utl.ist.meic.geofriendsfire.fragments.EventsNearbyListFragment;
import pt.utl.ist.meic.geofriendsfire.fragments.MapFragment;
import pt.utl.ist.meic.geofriendsfire.fragments.MyEventsListFragment;
import pt.utl.ist.meic.geofriendsfire.models.Event;
import pt.utl.ist.meic.geofriendsfire.utils.FragmentKeys;

public class DynamicViewPagerAdapter extends ArrayFragmentStatePagerAdapter<FragmentKeys> {


    private final Context mContext;
    private Event mEvent;

    public DynamicViewPagerAdapter(Context context, FragmentManager fm, ArrayList<FragmentKeys> datas) {
        super(fm, datas);
        this.mContext = context;
    }

    @Override
    public Fragment getFragment(FragmentKeys item, int position) {
        switch (item) {
            case EventsNearbyMap:
                MapFragment frag0 = new MapFragment();
                frag0.setContext(mContext);
                return frag0;
            case EventsNearby:
                EventsNearbyListFragment frag1 = new EventsNearbyListFragment();
                frag1.setContext(mContext);
                return frag1;
            case MyEvents:
                MyEventsListFragment frag2 = new MyEventsListFragment();
                frag2.setContext(mContext);
                return frag2;
            case EventDetailsMap:
                EventDetailsMapFragment frag3 = new EventDetailsMapFragment();
                frag3.setContext(mContext);
                frag3.setEvent(mEvent);
                return frag3;
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
