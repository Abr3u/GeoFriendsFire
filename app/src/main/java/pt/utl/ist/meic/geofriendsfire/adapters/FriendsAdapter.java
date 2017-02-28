package pt.utl.ist.meic.geofriendsfire.adapters;

import android.content.Context;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.models.Friend;


public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    private static final String TAG = "yyy";

    private final Context mContext;
    private DatabaseReference mDatabaseReference;
    private ValueEventListener mValueEventListener;

    private final TypedValue mTypedValue = new TypedValue();
    private int mBackground;
    private List<Friend> mValues;

    public FriendsAdapter(Context context, DatabaseReference ref) {
        mContext = context;
        mDatabaseReference = ref;
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
        mBackground = mTypedValue.resourceId;
        mValues = new ArrayList<Friend>();

        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot snap : dataSnapshot.getChildren()) {
                    Friend friend = new Friend();
                    friend.username = snap.getKey();
                    friend.score = snap.getValue(Double.class);
                    mValues.add(friend);
                }
                orderFriendsByValue();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "postComments:onCancelled", databaseError.toException());
                Toast.makeText(mContext, "Failed to load friends.",
                        Toast.LENGTH_SHORT).show();
            }
        };
        ref.addListenerForSingleValueEvent(valueEventListener);
        // [END child_event_listener_recycler]

        // Store reference to listener so it can be removed on app stop
        mValueEventListener = valueEventListener;

    }

    private void orderFriendsByValue() {
        Collections.sort(mValues,Friend.getComparatorScore());
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
        holder.mTextView.setText(mValues.get(position).username);
        holder.mTextView2.setText("" + mValues.get(position).score);

        Glide.with(holder.mImageView.getContext())
                .load(R.drawable.ic_person)
                .fitCenter()
                .into(holder.mImageView);

    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public void cleanupListener() {
        if (mValueEventListener != null) {
            mDatabaseReference.removeEventListener(mValueEventListener);
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

