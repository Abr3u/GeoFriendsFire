package pt.utl.ist.meic.geofriendsfire.adapters;

import android.app.AlertDialog;
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

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.activities.EventsNearbyActivity;
import pt.utl.ist.meic.geofriendsfire.models.Event;
import pt.utl.ist.meic.geofriendsfire.utils.IntentKeys;
import pt.utl.ist.meic.geofriendsfire.utils.Utils;


public class EventsNearbyAdapter extends RecyclerView.Adapter<EventsNearbyAdapter.ViewHolder> implements GeoQueryEventListener {

    private static final String EVENTS_LOCATIONS_REF = "/eventsLocations";
    private static final String EVENTS_REF = "/events";

    private final Context mContext;
    private final TypedValue mTypedValue = new TypedValue();
    private int mBackground;
    private Map<String, Event> mEventsMap;
    private List<Event> mValues;

    private GeoQuery geoQuery;
    private GeoLocation mCurrentLocation;
    private double mCurrentRadius;

    public EventsNearbyAdapter(Context context, GeoLocation currentLocation) {
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
        mBackground = mTypedValue.resourceId;
        mEventsMap = new HashMap<>();
        mValues = new ArrayList<>();
        this.mContext = context;
        this.mCurrentLocation = currentLocation;
        this.mCurrentRadius = 0.1;

        this.geoQuery = new GeoFire(FirebaseDatabase.getInstance().getReference(EVENTS_LOCATIONS_REF))
                .queryAtLocation(currentLocation, mCurrentRadius);

        this.geoQuery.addGeoQueryEventListener(this);

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
        Event e = mValues.get(position);
        holder.mTextView.setText(e.description);

        GeoLocation eventLocation = e.geoLocation;
        double distance = Utils.distance(mCurrentLocation.latitude, eventLocation.latitude, mCurrentLocation.longitude, eventLocation.longitude);
        holder.mTextView2.setText(String.format("%.3f", distance / 1000) + " kms away");//to km

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                Intent intent = new Intent(context, EventsNearbyActivity.class);
                //intent.putExtra(IntentKeys.initialCenterLati.name(), mValues.get(position).geoLocation.latitude);
                //intent.putExtra(IntentKeys.initialCenterLati.name(), mValues.get(position).geoLocation.longitude);
                context.startActivity(intent);
            }
        });

        //setup category icon
        switch (e.category) {
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

    /*
    *
    *
    * GeoFire Listeners
    *
    * */


    @Override
    public void onKeyEntered(final String key, final GeoLocation location) {
        MyApplicationContext appContext = (MyApplicationContext) mContext.getApplicationContext();
        int maxWorkload = appContext.getMaximumWorkLoad();
        Log.d("yyy", "keyEntered, size -> " + this.mValues.size() + " :: " + maxWorkload);

        if (this.mValues.size() < maxWorkload) {
            // New Event nearby
            FirebaseDatabase.getInstance().getReference(EVENTS_REF).child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Event v = dataSnapshot.getValue(Event.class);
                    v.geoLocation = location;
                    v.setRef(key);
                    mValues.add(v);
                    mEventsMap.put(key, v);
                    Log.d("yyy", "nearby - item inserted - " + v.toString());
                    notifyItemInserted(mValues.size() - 1);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("yyy", databaseError.toString());
                }
            });

        }
    }

    @Override
    public void onKeyExited(String key) {
        // Remove any old event
        Event event = this.mEventsMap.get(key);
        if (event != null) {
            // Remove data from the list
            int eventIndex = mValues.indexOf(event);
            mEventsMap.remove(event);
            mValues.remove(event);
            // Update the RecyclerView
            Log.d("yyy", "nearby - item removed - " + event.toString());
            notifyItemRemoved(eventIndex);
        }
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        Event event = this.mEventsMap.get(key);
        if (event != null) {
            event.geoLocation = location;
        }
    }

    @Override
    public void onGeoQueryReady() {
        MyApplicationContext appContext = (MyApplicationContext) mContext.getApplicationContext();
        int maxWorkload = appContext.getMaximumWorkLoad();
        if (this.mValues.size() < maxWorkload) {
            mCurrentRadius = mCurrentRadius * 2;
            geoQuery.setRadius(mCurrentRadius);
        }
    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        new AlertDialog.Builder(mContext)
                .setTitle("Error")
                .setMessage("There was an unexpected error querying GeoFire: " + error.getMessage())
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
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

