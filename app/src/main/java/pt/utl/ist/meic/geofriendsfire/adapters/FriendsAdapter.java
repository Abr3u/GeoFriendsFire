package pt.utl.ist.meic.geofriendsfire.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
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
import pt.utl.ist.meic.geofriendsfire.activities.CreateMessageActivity;
import pt.utl.ist.meic.geofriendsfire.models.Friend;
import pt.utl.ist.meic.geofriendsfire.utils.IntentKeys;


public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    private static final String TAG = "yyy";
    private static final String FRIENDS_REF = "/friends/";

    private final Context mContext;
    private DatabaseReference mDatabaseReference;
    private ChildEventListener mChildEventListener;

    private final TypedValue mTypedValue = new TypedValue();
    private int mBackground;

    private List<Friend> mValues;

    public FriendsAdapter(Context context, DatabaseReference ref) {
        mContext = context;
        mDatabaseReference = ref;
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
        mBackground = mTypedValue.resourceId;
        mValues = new ArrayList<Friend>();

        List<Friend> friendList = MyApplicationContext.getInstance().getMyFriends();
        if(!friendList.isEmpty()){
            mValues.addAll(friendList);
            notifyDataSetChanged();
        }

        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Friend f = new Friend();
                f.ref = dataSnapshot.getKey();
                f.username = dataSnapshot.getValue(String.class);
                if (!mValues.contains(f)) {
                    mValues.add(f);
                    MyApplicationContext.getInstance().addFriend(f);
                    notifyDataSetChanged();
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Friend f = new Friend();
                f.ref = dataSnapshot.getKey();
                f.username = dataSnapshot.getValue(String.class);
                if (mValues.contains(f)) {
                    mValues.remove(f);
                    MyApplicationContext.getInstance().removeFriend(f);
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
        mChildEventListener = childEventListener;

    }

    public List<Friend> getValues() {
        return mValues;
    }

    public void setValues(List<Friend> values) {
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
        String username = mValues.get(position).username;
        holder.mTextView.setText(username);

        Glide.with(holder.mFriendIcon.getContext())
                .load(R.drawable.ic_person)
                .fitCenter()
                .into(holder.mFriendIcon);

        Glide.with(holder.mSendMsgIcon.getContext())
                .load(R.drawable.ic_message)
                .fitCenter()
                .into(holder.mSendMsgIcon);

        Glide.with(holder.mRemoveFriendIcon.getContext())
                .load(R.drawable.ic_remove)
                .fitCenter()
                .into(holder.mRemoveFriendIcon);

        View.OnClickListener profileListerner = view ->
                Toast.makeText(mContext, "Go to User " + username + " profile", Toast.LENGTH_SHORT).show();

        holder.mFriendIcon.setOnClickListener(profileListerner);
        holder.mTextView.setOnClickListener(profileListerner);

        holder.mRemoveFriendIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAlertDialogDelete(position);
            }
        });

        holder.mSendMsgIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(mContext, CreateMessageActivity.class);
                i.putExtra(IntentKeys.messageReceiverUsername.toString(),username);
                i.putExtra(IntentKeys.messageReceiverRef.toString(),mValues.get(position).ref);
                mContext.startActivity(i);
            }
        });

    }

    private void showAlertDialogDelete(int position) {
        String username = mValues.get(position).username;
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getString(R.string.dialog_title_remove_friend)+username);
        builder.setMessage(mContext.getString(R.string.dialog_message_remove_friend));

        String positiveText = mContext.getString(android.R.string.ok);
        builder.setPositiveButton(positiveText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteFriendFirebase(position);
                    }
                });

        String negativeText = mContext.getString(android.R.string.cancel);
        builder.setNegativeButton(negativeText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteFriendFirebase(int position) {
        Toast.makeText(mContext, "Removing Friend", Toast.LENGTH_SHORT).show();
        Friend f = mValues.get(position);
        String myId = MyApplicationContext.getInstance().getFirebaseUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(FRIENDS_REF + myId);

        MyApplicationContext.getInstance().removeFriend(f);
        String key = f.ref;
        ref.child(key).removeValue();
        mValues.remove(position);
        notifyDataSetChanged();
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
        public final ImageView mFriendIcon;
        public final TextView mTextView;
        public final ImageView mSendMsgIcon;
        public final ImageView mRemoveFriendIcon;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mFriendIcon = (ImageView) view.findViewById(R.id.iv_image);
            mTextView = (TextView) view.findViewById(R.id.iv_friend);
            mSendMsgIcon = (ImageView) view.findViewById(R.id.iv_send_msg_icon);
            mRemoveFriendIcon = (ImageView) view.findViewById(R.id.iv_add_icon);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTextView.getText();
        }
    }
}

