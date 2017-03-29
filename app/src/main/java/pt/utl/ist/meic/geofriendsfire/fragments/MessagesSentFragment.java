package pt.utl.ist.meic.geofriendsfire.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
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
import butterknife.OnClick;
import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.activities.CreateMessageActivity;
import pt.utl.ist.meic.geofriendsfire.adapters.FriendsAdapter;
import pt.utl.ist.meic.geofriendsfire.adapters.MessagesAdapter;

public class MessagesSentFragment extends BaseFragment{

    private static final String PARCEL_VALUES = "values";

    @BindView(R.id.networkDetectorHolder)
    TextView networkDetectorHolder;

    @BindView(R.id.recyclerview)
    RecyclerView recyclerView;

    @BindView(R.id.fab)
    FloatingActionButton fab;

    @OnClick(R.id.fab)
    public void onClick(View v){
        startActivity(new Intent(getContext(), CreateMessageActivity.class));
    }

    private static final String MESSAGES_REF = "/messages/";
    MessagesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_list, container, false);
        ButterKnife.bind(this,view);
        fab.setVisibility(View.VISIBLE);
        super.setNetworkDetectorHolder(networkDetectorHolder);
        String myId = MyApplicationContext.getInstance().getFirebaseUser().getUid();
        adapter = new MessagesAdapter(getContext(),FirebaseDatabase.getInstance().getReference(MESSAGES_REF +myId+"/sent"),false);
        setupRecyclerView();

        if(savedInstanceState != null){
            adapter.setValues(Parcels.unwrap(savedInstanceState.getParcelable(PARCEL_VALUES)));
        }
        return view;
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(PARCEL_VALUES, Parcels.wrap(adapter.getValues()));
    }

}

