package pt.utl.ist.meic.geofriendsfire.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jakewharton.rxbinding2.support.v7.widget.RxSearchView;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.adapters.FriendSearchAdapter;
import pt.utl.ist.meic.geofriendsfire.models.Friend;
import pt.utl.ist.meic.geofriendsfire.models.User;

public class FriendSearchFragment extends BaseFragment{

    @BindView(R.id.networkDetectorHolder)
    TextView networkDetectorHolder;

    @BindView(R.id.recyclerview)
    RecyclerView recyclerView;

    @BindView(R.id.search_view)
    SearchView searchView;

    private static final String USERS_REF = "/users/";
    FriendSearchAdapter adapter;
    DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_list_search, container, false);
        ButterKnife.bind(this,view);
        super.setNetworkDetectorHolder(networkDetectorHolder);
        mDatabase = FirebaseDatabase.getInstance().getReference(USERS_REF);
        adapter = new FriendSearchAdapter(getContext());
        setupRecyclerView();
        setupSearchViewListener();
        return view;
    }

    private void setupSearchViewListener() {
        RxSearchView.queryTextChanges(searchView)
                .debounce(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(x -> {
                    if(x.length() > 0){
                        getUsersFromFirebase(x);
                    }else if(x.length() == 0){
                        adapter.clearValues();
                    }
                });
    }

    private void getUsersFromFirebase(CharSequence x) {
        String prefix = x.toString().trim();
        adapter.clearValues();
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot snap : dataSnapshot.getChildren()) {
                    User user = snap.getValue(User.class);
                    if(user.username.toLowerCase().startsWith(prefix) ||
                            user.username.toUpperCase().startsWith(prefix) ){
                        //skip self
                        if(!user.email.equals(MyApplicationContext.getInstance().getFirebaseUser().getEmail())){
                            adapter.addValue(user.username);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mDatabase.addValueEventListener(listener);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

}

