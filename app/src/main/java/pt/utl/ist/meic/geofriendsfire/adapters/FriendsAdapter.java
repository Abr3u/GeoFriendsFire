package pt.utl.ist.meic.geofriendsfire.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
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

import java.util.ArrayList;
import java.util.List;

import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;


public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    private static final String TAG = "yyy";
    private static final String FRIENDS_REF = "/friends/";

    private final Context mContext;
    private DatabaseReference mDatabaseReference;
    private ChildEventListener mChildEventListener;

    private final TypedValue mTypedValue = new TypedValue();
    private int mBackground;

    private List<String> mValues;

    public FriendsAdapter(Context context, DatabaseReference ref) {
        mContext = context;
        mDatabaseReference = ref;
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
        mBackground = mTypedValue.resourceId;
        mValues = new ArrayList<String>();


        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String friend = dataSnapshot.getKey();
                if(!mValues.contains(friend)){
                    mValues.add(friend);
                    notifyDataSetChanged();
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

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

    public List<String> getValues() {
        return mValues;
    }

    public void setValues(List<String> values) {
        this.mValues = values;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_item_friend, parent, false);
        view.setBackgroundResource(mBackground);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        String username = mValues.get(position);
        holder.mTextView.setText(username);

        Glide.with(holder.mImageView.getContext())
                .load(R.drawable.ic_person)
                .fitCenter()
                .into(holder.mImageView);

        Glide.with(holder.mImageView2.getContext())
                .load(R.drawable.ic_remove)
                .fitCenter()
                .into(holder.mImageView2);

        View.OnClickListener profileListerner = view ->
                Toast.makeText(mContext, "Go to User "+username+" profile", Toast.LENGTH_SHORT).show();

        holder.mImageView.setOnClickListener(profileListerner);
        holder.mTextView.setOnClickListener(profileListerner);

        holder.mImageView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext, "Removing Friend", Toast.LENGTH_SHORT).show();
                String username = mValues.get(position);
                String myId = MyApplicationContext.getInstance().getFirebaseUser().getUid();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference(FRIENDS_REF+myId);
                ref.child(username).removeValue();
                mValues.remove(position);
                notifyDataSetChanged();
            }
        });

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
        public final ImageView mImageView2;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mImageView = (ImageView) view.findViewById(R.id.iv_image);
            mTextView = (TextView) view.findViewById(R.id.iv_friend);
            mImageView2 = (ImageView) view.findViewById(R.id.iv_add_icon);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTextView.getText();
        }
    }
}

