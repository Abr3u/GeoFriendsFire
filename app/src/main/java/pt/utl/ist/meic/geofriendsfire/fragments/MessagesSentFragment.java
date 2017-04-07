package pt.utl.ist.meic.geofriendsfire.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.FirebaseDatabase;

import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.activities.CreateMessageActivity;
import pt.utl.ist.meic.geofriendsfire.adapters.MessagesAdapter;
import pt.utl.ist.meic.geofriendsfire.models.Message;

public class MessagesSentFragment extends BaseFragment{

    private static final String PARCEL_VALUES = "values";

    @BindView(R.id.networkDetectorHolder)
    TextView networkDetectorHolder;

    @BindView(R.id.recyclerview)
    RecyclerView recyclerView;

    private static final String MESSAGES_REF = "/messages/";
    MessagesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_list, container, false);
        ButterKnife.bind(this,view);
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
                    builder.setMessage("Are you sure you want to delete this message?");    //set message

                    builder.setPositiveButton("REMOVE", new DialogInterface.OnClickListener() { //when click on DELETE
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteMessageFirebase(position);
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

    private void deleteMessageFirebase(int position) {
        Message toDelete = adapter.getValues().get(position);
        adapter.removeValue(position);
        Toast.makeText(getContext(), "Deleting "+toDelete.content, Toast.LENGTH_LONG).show();
        String myId = MyApplicationContext.getInstance().getFirebaseUser().getUid();
        FirebaseDatabase.getInstance().getReference(MESSAGES_REF +myId+"/sent").child(toDelete.ref).removeValue();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(PARCEL_VALUES, Parcels.wrap(adapter.getValues()));
    }

}

