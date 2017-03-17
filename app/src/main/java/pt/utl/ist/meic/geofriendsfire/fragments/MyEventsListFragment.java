package pt.utl.ist.meic.geofriendsfire.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.greenrobot.eventbus.EventBus;
import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.ButterKnife;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.adapters.MyEventsAdapter;
import pt.utl.ist.meic.geofriendsfire.events.NewDeletedEvent;
import pt.utl.ist.meic.geofriendsfire.models.Event;

public class MyEventsListFragment extends BaseFragment{

    private static final String PARCEL_VALUES = "values";
    private static final String PARCEL_VALUES_MAP = "valuesMap";

    @BindView(R.id.networkDetectorHolder)
    TextView networkDetectorHolder;

    @BindView(R.id.recyclerview)
    RecyclerView recyclerView;

    private static final String EVENTS_REF = "/events";
    private static final String EVENTS_LOCATIONS_REF = "/eventsLocations";
    private MyEventsAdapter adapter;
    private DatabaseReference mFirebaseEventsRef;
    private DatabaseReference mFirebaseEventsLocationRef;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_list, container, false);
        ButterKnife.bind(this,view);
        super.setNetworkDetectorHolder(networkDetectorHolder);
        mFirebaseEventsRef = FirebaseDatabase.getInstance().getReference(EVENTS_REF);
        mFirebaseEventsLocationRef = FirebaseDatabase.getInstance().getReference(EVENTS_LOCATIONS_REF);
        adapter = new MyEventsAdapter(getContext(), mFirebaseEventsRef);
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
        setSwipeToDelete();
    }

    private void setSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition(); //get position which is swiped

                if (direction == ItemTouchHelper.LEFT) {    //if swipe left

                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext()); //alert for confirm to delete
                    builder.setMessage("Are you sure you want to delete this event?");    //set message

                    builder.setPositiveButton("REMOVE", new DialogInterface.OnClickListener() { //when click on DELETE
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteEventFirebase(position);
                            return;
                        }
                    }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {  //not removing items if cancel is done
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            adapter.notifyItemRemoved(position + 1);    //notifies the RecyclerView Adapter that data in adapter has been removed at a particular position.
                            adapter.notifyItemRangeChanged(position, adapter.getItemCount());
                            return;
                        }
                    }).show();  //show alert dialog
                }
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView); //set swipe to recylcerview
    }

    private void deleteEventFirebase(int position) {
        Event toDelete = adapter.getValues().get(position);
        adapter.removeValue(position);
        Toast.makeText(getContext(), "Deleting "+toDelete.description, Toast.LENGTH_LONG).show();

        EventBus.getDefault().post(new NewDeletedEvent(toDelete));
        mFirebaseEventsRef.child(toDelete.ref).removeValue();
        mFirebaseEventsLocationRef.child(toDelete.ref).removeValue();
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

