package pt.utl.ist.meic.geofriendsfire.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.activities.DrawerMainActivity;
import pt.utl.ist.meic.geofriendsfire.activities.MessageDetailsActivity;
import pt.utl.ist.meic.geofriendsfire.models.Message;
import pt.utl.ist.meic.geofriendsfire.utils.IntentKeys;


public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {

    private final Context mContext;
    private DatabaseReference mDatabaseReference;
    private ChildEventListener mChildEventListener;

    private final TypedValue mTypedValue = new TypedValue();
    private int mBackground;
    private boolean isInbox;

    private List<Message> mValues;

    public MessagesAdapter(Context context, DatabaseReference ref, boolean isInbox) {
        mContext = context;
        mDatabaseReference = ref;
        this.isInbox = isInbox;
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
        mBackground = mTypedValue.resourceId;
        mValues = new ArrayList<Message>();


        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Message message = dataSnapshot.getValue(Message.class);
                message.ref = dataSnapshot.getKey();
                if (!mValues.contains(message)) {
                    mValues.add(message);
                    notifyDataSetChanged();
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Message message = dataSnapshot.getValue(Message.class);
                if (mValues.contains(message)) {
                    mValues.remove(message);
                    notifyDataSetChanged();
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };


        ref.addChildEventListener(childEventListener);
        // [END child_event_listener_recycler]

        // Store reference to listener so it can be removed on app stop
        mChildEventListener = childEventListener;

    }

    public void removeValue(int position) {
        this.mValues.remove(position);
        notifyItemRemoved(position);
    }

    public List<Message> getValues() {
        return mValues;
    }

    public void setValues(List<Message> values) {
        this.mValues = values;
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
        Message msg = mValues.get(position);
        if (isInbox) {
            holder.mTextView.setText(msg.fromUsername);
        } else {
            holder.mTextView.setText(msg.toUsername);
        }
        holder.mTextView2.setText(msg.sentDate);

        Glide.with(holder.mImageView.getContext())
                .load(R.drawable.ic_message)
                .fitCenter()
                .into(holder.mImageView);

        View.OnClickListener messageDetailsListerner = view -> {
            Intent i = new Intent(mContext, MessageDetailsActivity.class);
            i.putExtra(IntentKeys.messageDetails.toString(),Parcels.wrap(msg));
            i.putExtra(IntentKeys.isMsgReceived.toString(),isInbox);
            mContext.startActivity(i);
        };

        holder.mView.setOnClickListener(messageDetailsListerner);

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
            return super.toString() + " '" + mTextView.getText();
        }
    }
}

