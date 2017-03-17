package pt.utl.ist.meic.geofriendsfire.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.activities.DrawerMainActivity;
import pt.utl.ist.meic.geofriendsfire.models.Event;


public class MyEventsAdapter extends RecyclerView.Adapter<MyEventsAdapter.ViewHolder> {

    private static final String TAG = "yyy";

    private static final String EVENTS_LOCATIONS_REF = "/eventsLocations";

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

                // A new Event has been added, add it to the displayed list
                final Event eventAux = dataSnapshot.getValue(Event.class);
                final String eventKey = dataSnapshot.getKey();
                FirebaseDatabase.getInstance().getReference(EVENTS_LOCATIONS_REF).child(eventKey+"/l").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        double latitude = dataSnapshot.child("0").getValue(Double.class);
                        double longitude = dataSnapshot.child("1").getValue(Double.class);

                        Event v = new Event(eventAux.authorId,eventAux.authorName,eventAux.description,eventAux.category,eventAux.creationDate);
                        v.latitude = latitude;
                        v.longitude = longitude;
                        v.setRef(eventKey);

                        // Update RecyclerView
                        mEventsMap.put(eventKey,v);
                        mValues.add(v);
                        notifyItemInserted(mValues.size() - 1);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
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

                // An event has changed, use the key to determine if we are displaying this
                // comment and if so remove it.
                String eventKey = dataSnapshot.getKey();

                Event e = mEventsMap.get(eventKey);
                if (e != null) {
                    // Remove data from the list
                    int eventIndex = mValues.indexOf(e);
                    Log.d("yyy","MyEvtsAdapter removing on index "+eventIndex);
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

    public void removeValue(int position){
        mEventsMap.remove(mValues.get(position).ref);
        mValues.remove(position);
        notifyItemRemoved(position);
    }

    public List<Event> getValues(){
        return this.mValues;
    }

    public void setValues(List<Event> values){
        this.mValues = values;
    }

    public Map<String,Event> getValuesMap(){
        return this.mEventsMap;
    }

    public void setValuesMap(Map<String,Event> map){
        this.mEventsMap = map;
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


        //cardview listeners
        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Event event = mValues.get(position);
                if(mContext instanceof DrawerMainActivity){
                    ((DrawerMainActivity)mContext).setupViewPagerEventDetails(event);
                }
            }
        });

        holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Event event = mValues.get(position);
                Uri gmmIntentUri = Uri.parse("google.navigation:" +
                        "q="+event.latitude+","+event.longitude+"&mode=w");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                mContext.startActivity(mapIntent);
                return true;
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

