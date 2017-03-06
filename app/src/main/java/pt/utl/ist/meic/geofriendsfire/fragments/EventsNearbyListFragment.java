package pt.utl.ist.meic.geofriendsfire.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoLocation;

import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.ButterKnife;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.adapters.EventsNearbyAdapter;
import pt.utl.ist.meic.geofriendsfire.location.GPSTracker;

public class EventsNearbyListFragment extends BaseFragment {

    private static final String PARCEL_VALUES = "values";
    private static final String PARCEL_VALUES_MAP = "valuesMap";
    @BindView(R.id.networkDetectorHolder)
    TextView networkDetectorHolder;

    @BindView(R.id.recyclerview)
    RecyclerView recyclerView;

    private EventsNearbyAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_list, container, false);
        ButterKnife.bind(this, view);
        super.setNetworkDetectorHolder(networkDetectorHolder);

        GPSTracker gpsTracker = new GPSTracker(getContext());
        if (!gpsTracker.canGetLocation()) {
            Toast.makeText(getContext(), "cant get current location", Toast.LENGTH_LONG).show();
        } else {
            GeoLocation currentLocation = new GeoLocation(gpsTracker.getLatitude(), gpsTracker.getLongitude());
            mAdapter = new EventsNearbyAdapter(getContext(), currentLocation);
            setupRecyclerView();
        }

        if(savedInstanceState != null){
            mAdapter.setValues(Parcels.unwrap(savedInstanceState.getParcelable(PARCEL_VALUES)));
            mAdapter.setValuesMap(Parcels.unwrap(savedInstanceState.getParcelable(PARCEL_VALUES_MAP)));
        }
        return view;
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        mAdapter.cleanupListener();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(PARCEL_VALUES, Parcels.wrap(mAdapter.getValues()));
        outState.putParcelable(PARCEL_VALUES_MAP, Parcels.wrap(mAdapter.getValuesMap()));
    }
}
