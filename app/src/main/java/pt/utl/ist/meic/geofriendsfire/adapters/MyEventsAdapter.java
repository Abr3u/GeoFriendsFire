package pt.utl.ist.meic.geofriendsfire.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import durdinapps.rxfirebase2.RxFirebaseRecyclerAdapter;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.activities.EventsNearbyActivity;
import pt.utl.ist.meic.geofriendsfire.models.Event;


public class MyEventsAdapter extends RxFirebaseRecyclerAdapter<MyEventsAdapter.EventViewHolder, Event>{
    private static final String TAG = "MyEventsAdapter";

    public MyEventsAdapter() {
        super(Event.class);
    }

    @Override
    public EventViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_item, parent, false);
        return new MyEventsAdapter.EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(EventViewHolder holder, int position) {
        Event event = getItems().get(position);
        holder.mBoundString = event.description;
        holder.mTextView.setText(event.description);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                Intent intent = new Intent(context, EventsNearbyActivity.class);
                context.startActivity(intent);
            }
        });

        Glide.with(holder.mImageView.getContext())
                .load(R.drawable.ic_food)
                .fitCenter()
                .into(holder.mImageView);
    }

    @Override
    protected void itemAdded(Event item, String key, int position) {
        //Add the refs if you need them later
        item.setRef(key);
        Log.d(TAG, "Added a new item to the adapter.");
    }

    @Override
    protected void itemChanged(Event oldItem, Event newItem, String key, int position) {
        //Add the refs if you need them later
        newItem.setRef(key);
        Log.d(TAG, "Changed an item.");
    }

    @Override
    protected void itemRemoved(Event item, String key, int position) {
        Log.d(TAG, "Removed an item.");
    }

    @Override
    protected void itemMoved(Event item, String key, int oldPosition, int newPosition) {
        Log.d(TAG, "Moved an item.");
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        public String mBoundString;
        public final View mView;
        public final ImageView mImageView;
        public final TextView mTextView;

        public EventViewHolder(View view) {
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
