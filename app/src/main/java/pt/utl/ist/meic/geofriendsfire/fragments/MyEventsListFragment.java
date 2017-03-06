package pt.utl.ist.meic.geofriendsfire.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.database.FirebaseDatabase;

import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.ButterKnife;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.adapters.MyEventsAdapter;

public class MyEventsListFragment extends BaseFragment{

    private static final String PARCEL_VALUES = "values";
    private static final String PARCEL_VALUES_MAP = "valuesMap";

    @BindView(R.id.networkDetectorHolder)
    TextView networkDetectorHolder;

    @BindView(R.id.recyclerview)
    RecyclerView recyclerView;

    private static final String EVENTS_REF = "/events";
    MyEventsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_list, container, false);
        ButterKnife.bind(this,view);
        super.setNetworkDetectorHolder(networkDetectorHolder);
        adapter = new MyEventsAdapter(getContext(),FirebaseDatabase.getInstance().getReference(EVENTS_REF));
        setupRecyclerView();
        if(savedInstanceState != null){
            adapter.setValues(Parcels.unwrap(savedInstanceState.getParcelable(PARCEL_VALUES)));
            adapter.setValuesMap(Parcels.unwrap(savedInstanceState.getParcelable(PARCEL_VALUES_MAP)));
        }

        return view;
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.cleanupListener();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(PARCEL_VALUES, Parcels.wrap(adapter.getValues()));
        outState.putParcelable(PARCEL_VALUES_MAP, Parcels.wrap(adapter.getValuesMap()));
    }

}

