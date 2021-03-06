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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.models.Friend;
import pt.utl.ist.meic.geofriendsfire.models.User;


public class FriendsSuggestionsAdapter extends RecyclerView.Adapter<FriendsSuggestionsAdapter.ViewHolder> {

    private static final String TAG = "yyy";
    private static final String FRIENDS_REF = "/friends/";
    private static final String FRIENDS_SUG_REF = "/friendsSuggestions/";
    private static final String USERS_REF = "/users/";

    private final Context mContext;
    private DatabaseReference mDatabaseReference;
    private ValueEventListener mValueEventListener;

    private final TypedValue mTypedValue = new TypedValue();
    private int mBackground;

    private List<Friend> mValues;
    private List<String> myFriendsRefs;

    public FriendsSuggestionsAdapter(Context context, DatabaseReference ref) {
        mContext = context;
        mDatabaseReference = ref;
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
        mBackground = mTypedValue.resourceId;
        mValues = new ArrayList<Friend>();

        myFriendsRefs = new ArrayList<String>();
        for(Friend f : MyApplicationContext.getInstance().getMyFriends()){
            myFriendsRefs.add(f.ref);
        }

        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot snap : dataSnapshot.getChildren()) {
                    String friendKey = snap.getKey();
                    Double score = snap.getValue(Double.class);

                    FirebaseDatabase.getInstance().getReference(USERS_REF+friendKey).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Friend friend = new Friend();
                            friend.ref = friendKey;
                            friend.score = score;
                            User aux = dataSnapshot.getValue(User.class);
                            friend.username = aux.username;
                            //skip already friends
                            if(!myFriendsRefs.contains(friend.ref) && !mValues.contains(friend)){
                                mValues.add(friend);
                                orderFriendsByValue();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "friends:onCancelled", databaseError.toException());
                Toast.makeText(mContext, "Failed to load suggestions.",
                        Toast.LENGTH_SHORT).show();
            }
        };
        ref.addListenerForSingleValueEvent(valueEventListener);
        // [END child_event_listener_recycler]

        // Store reference to listener so it can be removed on app stop
        mValueEventListener = valueEventListener;

    }

    public List<Friend> getValues() {
        return mValues;
    }

    public void setValues(List<Friend> values) {
        this.mValues = values;
    }

    private void orderFriendsByValue() {
        Log.d("ggg","order");
        if(mValues.size()>1){
            Collections.sort(mValues,Friend.getComparatorScore());
        }
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_item_friend_suggestion, parent, false);
        view.setBackgroundResource(mBackground);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        String username = mValues.get(position).username;
        holder.mTextView.setText(username);

        Glide.with(holder.mImageView.getContext())
                .load(R.drawable.ic_person)
                .fitCenter()
                .into(holder.mImageView);

        Glide.with(holder.mImageView2.getContext())
                .load(R.drawable.ic_add)
                .fitCenter()
                .into(holder.mImageView2);

        View.OnClickListener profileListerner = view ->
                Toast.makeText(mContext, "Go to User "+username+" profile", Toast.LENGTH_SHORT).show();

        holder.mImageView.setOnClickListener(profileListerner);
        holder.mTextView.setOnClickListener(profileListerner);

        holder.mImageView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext, "Adding Friend", Toast.LENGTH_SHORT).show();
                Friend friend = mValues.get(position);
                MyApplicationContext.getInstance().addFriend(friend);
                String myId = MyApplicationContext.getInstance().getFirebaseUser().getUid();

                //add new friend
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference(FRIENDS_REF+myId);
                ref.child(friend.ref).setValue(friend.username);

                //remove sugestion for this friend
                ref = FirebaseDatabase.getInstance().getReference(FRIENDS_SUG_REF+myId);
                ref.child(friend.ref).removeValue();
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
        if (mValueEventListener != null) {
            mDatabaseReference.removeEventListener(mValueEventListener);
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

