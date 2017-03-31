package pt.utl.ist.meic.geofriendsfire.adapters;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.activities.DrawerMainActivity;
import pt.utl.ist.meic.geofriendsfire.activities.EventDetailsMapActivity;
import pt.utl.ist.meic.geofriendsfire.models.Event;
import pt.utl.ist.meic.geofriendsfire.utils.IntentKeys;
import pt.utl.ist.meic.geofriendsfire.utils.Utils;


public class EventsNearbyAdapter extends RecyclerView.Adapter<EventsNearbyAdapter.ViewHolder> {

    private static final String EVENTS_LOCATIONS_REF = "/eventsLocations";
    private static final String EVENTS_REF = "/events";
    private static final double MIN_RADIUS = 0.1;

    private final Context mContext;
    private final TypedValue mTypedValue = new TypedValue();
    private int mBackground;
    private List<Event> mValues;

    public EventsNearbyAdapter(Context context) {
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
        mBackground = mTypedValue.resourceId;
        mContext = context;
        mValues = new ArrayList<>();
    }

    public void addItem(Event e) {
        if (mValues.contains(e)) {
            mValues.remove(e);
        }
        mValues.add(e);
        notifyDataSetChanged();
    }

    public void removeItem(Event toDelete) {
        Log.d("yyy", "evntsNearbyAdapter remove " + toDelete);
        mValues.remove(toDelete);
        notifyDataSetChanged();
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

        Location lastKnowLocation =
                MyApplicationContext.getLocationsServiceInstance().getLastKnownLocation();

        if (lastKnowLocation != null) {
            double distance = Utils.distance(lastKnowLocation.getLatitude(), e.latitude, lastKnowLocation.getLongitude(), e.longitude);
            holder.mTextView2.setText(String.format("%.3f", distance / 1000) + " kms away");//to km
        }

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Event event = mValues.get(position);
                Intent i = new Intent(mContext, EventDetailsMapActivity.class);
                i.putExtra(IntentKeys.eventDetails.toString(), Parcels.wrap(event));
                mContext.startActivity(i);
            }
        });

        holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Event event = mValues.get(position);
                Uri gmmIntentUri = Uri.parse("google.navigation:" +
                        "q=" + event.latitude + "," + event.longitude + "&mode=d");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                mContext.startActivity(mapIntent);
                return true;
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

    public List<Event> getValues() {
        return this.mValues;
    }

    public void setValues(List<Event> values) {
        this.mValues = values;
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

