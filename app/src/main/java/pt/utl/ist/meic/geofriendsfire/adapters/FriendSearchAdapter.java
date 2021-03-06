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


public class FriendSearchAdapter extends RecyclerView.Adapter<FriendSearchAdapter.ViewHolder> {

    private static final String FRIENDS_REF = "/friends/";

    private final Context mContext;
    private final TypedValue mTypedValue = new TypedValue();
    private int mBackground;
    private List<User> mValues;

    public FriendSearchAdapter(Context context) {
        mContext = context;
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
        mBackground = mTypedValue.resourceId;
        mValues = new ArrayList<User>();
    }

    public void clearValues(){
        this.mValues.clear();
        notifyDataSetChanged();
    }
    public void addValue(User value){
        if(!this.mValues.contains(value)){
            this.mValues.add(value);
            notifyDataSetChanged();
        }
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
                User u = mValues.get(position);
                String username = u.username;

                Friend aux = new Friend();
                aux.ref = u.ref;
                aux.username = username;
                MyApplicationContext.getInstance().addFriend(aux);
                Toast.makeText(mContext, "Adding Friend "+username, Toast.LENGTH_SHORT).show();
                String myId = MyApplicationContext.getInstance().getFirebaseUser().getUid();

                //add new friend
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference(FRIENDS_REF+myId);
                ref.child(u.ref).setValue(username);
                mValues.remove(position);
                notifyDataSetChanged();
            }
        });

    }

    @Override
    public int getItemCount() {
        return mValues.size();
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

