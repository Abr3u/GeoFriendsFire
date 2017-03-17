package pt.utl.ist.meic.geofriendsfire.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.parceler.Parcels;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.adapters.EventsNearbyAdapter;
import pt.utl.ist.meic.geofriendsfire.events.DeletedEvent;
import pt.utl.ist.meic.geofriendsfire.events.NearbyEvent;
import pt.utl.ist.meic.geofriendsfire.models.Event;
import pt.utl.ist.meic.geofriendsfire.services.EventsNearbyService;

public class EventsNearbyListFragment extends BaseFragment {

    private static final String PARCEL_VALUES = "values";

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

        Log.d("ttt", "oncreateview");
        mAdapter = new EventsNearbyAdapter(getContext());
        populateSavedEvents();
        setupRecyclerView();
        return view;
    }

    private void populateSavedEvents() {
        EventsNearbyService service = MyApplicationContext.getEventsNearbyServiceInstance();
        if(service != null){
            List<Event> savedEvents = service.getValues();
            if(savedEvents!= null && !savedEvents.isEmpty()){
                mAdapter.setValues(savedEvents);
            }
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

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


    //
    // EventBus
    //

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DeletedEvent event) {
        mAdapter.removeItem(event.getDeleted());
        if(mAdapter.getItemCount() < MyApplicationContext.getInstance().getMaximumWorkLoad()){
            MyApplicationContext.getEventsNearbyServiceInstance().restartListener();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(NearbyEvent event) {
        mAdapter.addItem(event.getNearby());
    }
}
