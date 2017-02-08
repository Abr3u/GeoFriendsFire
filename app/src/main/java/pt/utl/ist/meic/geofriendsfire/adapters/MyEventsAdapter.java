package pt.utl.ist.meic.geofriendsfire.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.activities.EventsNearbyActivity;
import pt.utl.ist.meic.geofriendsfire.models.Event;


public class MyEventsAdapter extends RecyclerView.Adapter<MyEventsAdapter.ViewHolder> {

    private static final String TAG = "yyy";

    private static final String EVENTS_LOCATIONS_REF = "/eventsLocations";
    private static final String EVENTS_REF = "/events";

    private final Context mContext;
    private DatabaseReference mDatabaseReference;
    private ChildEventListener mChildEventListener;

    private final TypedValue mTypedValue = new TypedValue();
    private int mBackground;
    private Map<String, Event> mEventsMap;
    private List<Event> mValues;

    public MyEventsAdapter(Context context, DatabaseReference ref) {
        mContext = context;
        mDatabaseReference = ref;
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
        mBackground = mTypedValue.resourceId;
        mEventsMap = new HashMap<>();
        mValues = new ArrayList<>();

        // Create child event listener
        // [START child_event_listener_recycler]
        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildAdded:" + dataSnapshot.getKey());

                // A new Event has been added, add it to the displayed list
                Event event = dataSnapshot.getValue(Event.class);

                // Update RecyclerView
                mEventsMap.put(dataSnapshot.getKey(),event);
                mValues.add(event);
                notifyItemInserted(mValues.size() - 1);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildChanged:" + dataSnapshot.getKey());

                // A comment has changed, use the key to determine if we are displaying this
                // comment and if so displayed the changed comment.
                Event newEvent = dataSnapshot.getValue(Event.class);
                String eventKey = dataSnapshot.getKey();

                Event e = mEventsMap.get(eventKey);
                if (e != null) {
                    int eventIndex = mValues.indexOf(e);
                    // Replace with the new data
                    mValues.set(eventIndex, newEvent);
                    // Update the RecyclerView
                    notifyItemChanged(eventIndex);
                } else {
                    Log.w(TAG, "onChildChanged:unknown_child:" + eventKey);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());

                // A comment has changed, use the key to determine if we are displaying this
                // comment and if so remove it.
                String eventKey = dataSnapshot.getKey();

                Event e = mEventsMap.get(eventKey);
                if (e != null) {
                    // Remove data from the list
                    int eventIndex = mValues.indexOf(e);
                    mEventsMap.remove(e);
                    mValues.remove(e);
                    // Update the RecyclerView
                    notifyItemRemoved(eventIndex);
                } else {
                    Log.w(TAG, "onChildRemoved:unknown_child:" + eventKey);
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildMoved:" + dataSnapshot.getKey());

                // A comment has changed position, use the key to determine if we are
                // displaying this comment and if so move it.
                Event movedEvent = dataSnapshot.getValue(Event.class);
                String eventKey = dataSnapshot.getKey();

                Toast.makeText(mContext, "Event Moved -> "+movedEvent.toString(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "postComments:onCancelled", databaseError.toException());
                Toast.makeText(mContext, "Failed to load comments.",
                        Toast.LENGTH_SHORT).show();
            }
        };

        String myId = ((MyApplicationContext)mContext.getApplicationContext()).getFirebaseUser().getUid();

        ref.orderByChild("authorId").equalTo(myId).addChildEventListener(childEventListener);
        // [END child_event_listener_recycler]

        // Store reference to listener so it can be removed on app stop
        mChildEventListener = childEventListener;

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_item, parent, false);
        view.setBackgroundResource(mBackground);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.mTextView.setText(mValues.get(position).description);
        holder.mTextView2.setText(mValues.get(position).creationDate);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                Intent intent = new Intent(context, EventsNearbyActivity.class);
                //intent.putExtra("initialCenterLati", mValues.get(position).geoLocation.latitude);
                //intent.putExtra("initialCenterLongi", mValues.get(position).geoLocation.longitude);
                context.startActivity(intent);
            }
        });


        //setup category icon
        switch (mValues.get(position).category) {
            case "Food":
                Glide.with(holder.mImageView.getContext())
                        .load(R.drawable.ic_food)
                        .fitCenter()
                        .into(holder.mImageView);
                break;
            case "Sports":
                Glide.with(holder.mImageView.getContext())
                        .load(R.drawable.ic_sports)
                        .fitCenter()
                        .into(holder.mImageView);
                break;
            case "Shop":
                Glide.with(holder.mImageView.getContext())
                        .load(R.drawable.ic_shopping)
                        .fitCenter()
                        .into(holder.mImageView);
                break;

        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public void cleanupListener() {
        if (mChildEventListener != null) {
            mDatabaseReference.removeEventListener(mChildEventListener);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final ImageView mImageView;
        public final TextView mTextView;
        public final TextView mTextView2;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mImageView = (ImageView) view.findViewById(R.id.iv_image);
            mTextView = (TextView) view.findViewById(R.id.iv_text);
            mTextView2 = (TextView) view.findViewById(R.id.iv_extra);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTextView.getText() + " & " + mTextView2.getText();
        }
    }

}

