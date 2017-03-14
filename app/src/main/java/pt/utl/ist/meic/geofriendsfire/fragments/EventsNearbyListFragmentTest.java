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

import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.CompositeDisposable;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.adapters.EventsNearbyAdapterTest;
import pt.utl.ist.meic.geofriendsfire.services.EventsNearbyService;

public class EventsNearbyListFragmentTest extends BaseFragment {

    private static final String PARCEL_VALUES = "values";

    @BindView(R.id.networkDetectorHolder)
    TextView networkDetectorHolder;

    @BindView(R.id.recyclerview)
    RecyclerView recyclerView;

    private EventsNearbyAdapterTest mAdapter;
    private EventsNearbyService mService;

    private CompositeDisposable mDisposable;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_list, container, false);
        ButterKnife.bind(this, view);
        super.setNetworkDetectorHolder(networkDetectorHolder);

        Log.d("ttt", "oncreateview");

        Intent events = new Intent(getContext(), EventsNearbyService.class);
        getContext().bindService(events, eventsConnection, Context.BIND_AUTO_CREATE);

        mDisposable = new CompositeDisposable();
        mAdapter = new EventsNearbyAdapterTest(getContext());
        setupRecyclerView();
        return view;
    }

    private ServiceConnection eventsConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            EventsNearbyService.MyBinder binder = (EventsNearbyService.MyBinder) service;
            mService = binder.getService();

            mDisposable.add(mService.getEventsNearbyObservable()
                    .getObservable()
                    .subscribe(x -> {
                        Log.d("ttt","received Event "+x);
                        mAdapter.addItem(x);
                    })
            );

            mService.initListener();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mDisposable.dispose();
            mService = null;
        }
    };

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("ttt","onsave");
        outState.putParcelable(PARCEL_VALUES, Parcels.wrap(mAdapter.getValues()));
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if(savedInstanceState != null){
            Log.d("ttt","onRestore");
            mAdapter.setValues(Parcels.unwrap(savedInstanceState.getParcelable(PARCEL_VALUES)));
        }else{
            Log.d("ttt","onRestore era null");
        }
    }
}