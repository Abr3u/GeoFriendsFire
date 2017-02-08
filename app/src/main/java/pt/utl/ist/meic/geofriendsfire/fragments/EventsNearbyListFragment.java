package pt.utl.ist.meic.geofriendsfire.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firebase.geofire.GeoLocation;

import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.adapters.EventsNearbyAdapter;
import pt.utl.ist.meic.geofriendsfire.location.GPSTracker;

public class EventsNearbyListFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RecyclerView rv = (RecyclerView) inflater.inflate(
                R.layout.fragment_event_list, container, false);
        setupRecyclerView(rv);
        return rv;
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        GPSTracker gpsTracker = new GPSTracker(getActivity());
        if (!gpsTracker.canGetLocation()) {
            Toast.makeText(getActivity(), "cant get current location", Toast.LENGTH_LONG).show();
        } else {
            GeoLocation currentLocation = new GeoLocation(gpsTracker.getLatitude(), gpsTracker.getLongitude());

            recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
            recyclerView.setAdapter(new EventsNearbyAdapter(getActivity(),currentLocation));
        }
    }
}
