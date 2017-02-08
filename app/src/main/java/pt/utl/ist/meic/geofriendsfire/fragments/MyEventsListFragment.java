package pt.utl.ist.meic.geofriendsfire.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import durdinapps.rxfirebase2.RxFirebaseChildEvent;
import durdinapps.rxfirebase2.RxFirebaseDatabase;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.adapters.EventsNearbyAdapter;
import pt.utl.ist.meic.geofriendsfire.adapters.MyEventsAdapter;
import pt.utl.ist.meic.geofriendsfire.adapters.MyEventsAdapterRX;

public class MyEventsListFragment extends Fragment {

    private static final String EVENTS_LOCATIONS_REF = "/eventsLocations";
    private static final String EVENTS_REF = "/events";

    MyEventsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RecyclerView rv = (RecyclerView) inflater.inflate(
                R.layout.fragment_event_list, container, false);
        setupRecyclerView(rv);
        return rv;
    }

    private void setupRecyclerView(final RecyclerView recyclerView) {
        adapter = new MyEventsAdapter(getActivity(),FirebaseDatabase.getInstance().getReference(EVENTS_REF));
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.cleanupListener();
    }
}

