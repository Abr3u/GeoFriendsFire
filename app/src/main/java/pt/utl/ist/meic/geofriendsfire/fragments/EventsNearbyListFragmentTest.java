package pt.utl.ist.meic.geofriendsfire.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoLocation;

import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.adapters.EventsNearbyAdapter;
import pt.utl.ist.meic.geofriendsfire.adapters.EventsNearbyAdapterTest;
import pt.utl.ist.meic.geofriendsfire.location.GPSTracker;
import pt.utl.ist.meic.geofriendsfire.services.EventsNearbyService;

public class EventsNearbyListFragmentTest extends BaseFragment {

    private static final String PARCEL_VALUES = "values";

    @BindView(R.id.networkDetectorHolder)
    TextView networkDetectorHolder;

    @BindView(R.id.recyclerview)
    RecyclerView recyclerView;

    private EventsNearbyAdapterTest mAdapter;
    private EventsNearbyService mService;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_list, container, false);
        ButterKnife.bind(this, view);
        super.setNetworkDetectorHolder(networkDetectorHolder);

        Intent events = new Intent(getContext(), EventsNearbyService.class);
        getContext().bindService(events, eventsConnection, Context.BIND_AUTO_CREATE);

        if(savedInstanceState != null){
            mAdapter.setValues(Parcels.unwrap(savedInstanceState.getParcelable(PARCEL_VALUES)));
        }
        return view;
    }

    private ServiceConnection eventsConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            EventsNearbyService.MyBinder binder = (EventsNearbyService.MyBinder) service;
            mService = binder.getService();
            mService.getEventsNearby()
                    .getObservable()
                    .subscribe(x -> {
                        mAdapter.addItem(x);
                    });
            mAdapter = new EventsNearbyAdapterTest(getContext());
            setupRecyclerView();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(PARCEL_VALUES, Parcels.wrap(mAdapter.getValues()));
    }
}
