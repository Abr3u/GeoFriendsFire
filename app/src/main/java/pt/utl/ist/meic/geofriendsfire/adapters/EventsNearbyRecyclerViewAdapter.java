package pt.utl.ist.meic.geofriendsfire.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import durdinapps.rxfirebase2.RxFirebaseDatabase;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.activities.EventsNearbyActivity;
import pt.utl.ist.meic.geofriendsfire.models.Event;


public class EventsNearbyRecyclerViewAdapter extends RecyclerView.Adapter<EventsNearbyRecyclerViewAdapter.ViewHolder> implements GeoQueryEventListener{

    private static final String EVENTS_LOCATIONS_REF = "/eventsLocations";

    private final Context mContext;
    private final TypedValue mTypedValue = new TypedValue();
    private int mBackground;
    private Map<String, Event> mEventsMap;
    private List<Event> mValues;
    private int maxWorkload;

    private GeoQuery geoQuery;

    public EventsNearbyRecyclerViewAdapter(Context context, GeoLocation currentLocation, int maxWorkload) {
        mContext = context;
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
        mBackground = mTypedValue.resourceId;
        mEventsMap = new HashMap<>();
        mValues = new ArrayList<>();
        this.maxWorkload = maxWorkload;

        this.geoQuery = new GeoFire(FirebaseDatabase.getInstance().getReference(EVENTS_LOCATIONS_REF))
                .queryAtLocation(currentLocation,1);

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
        holder.mBoundString = mValues.get(position).description;
        holder.mTextView.setText(mValues.get(position).description);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                Intent intent = new Intent(context, EventsNearbyActivity.class);
                intent.putExtra("initialCenterLati",mValues.get(position).geoLocation.latitude);
                intent.putExtra("initialCenterLongi",mValues.get(position).geoLocation.longitude);
                context.startActivity(intent);
            }
        });

        Glide.with(holder.mImageView.getContext())
                .load(R.drawable.ic_food)
                .fitCenter()
                .into(holder.mImageView);
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
        if (this.mValues.size() < maxWorkload) {
            // New Event nearby
            Toast.makeText(mContext, "New Event -> "+key, Toast.LENGTH_LONG).show();
            RxFirebaseDatabase.observeSingleValueEvent(FirebaseDatabase.getInstance().getReference("/events/"+key),Event.class)
                    .subscribe(new Subscriber<Event>() {
                        @Override
                        public void onSubscribe(Subscription s) {
                        }

                        @Override
                        public void onNext(Event v) {
                            v.geoLocation = location;
                            mValues.add(v);
                            mEventsMap.put(key, v);
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onComplete() {
                        }});

        }
    }

    @Override
    public void onKeyExited(String key) {
        // Remove any old event
        Event event = this.mEventsMap.get(key);
        if (event != null) {
            this.mValues.remove(event);
            this.mEventsMap.remove(key);
        }
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        Event event = this.mEventsMap.get(key);
        if(event != null){
            event.geoLocation = location;
        }
    }

    @Override
    public void onGeoQueryReady() {
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
        public String mBoundString;
        public final View mView;
        public final ImageView mImageView;
        public final TextView mTextView;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mImageView = (ImageView) view.findViewById(R.id.iv_image);
            mTextView = (TextView) view.findViewById(R.id.iv_text);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTextView.getText();
        }
    }

}

