package pt.utl.ist.meic.geofriendsfire.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.models.Friend;
import pt.utl.ist.meic.geofriendsfire.models.Message;

public class CreateMessageActivity extends AppCompatActivity {

    private static final String FRIENDS_REF = "friends/";
    private static final String MSG_REF = "messages/";
    private DatabaseReference mDatabase;
    private ArrayAdapter<String> adapter;
    private ValueEventListener mListener;
    private List<Friend> mValues;

    @BindView(R.id.friendsSpinner)
    Spinner spinner;

    @BindView(R.id.message_content)
    EditText messageContent;

    @OnClick(R.id.createMessageButton)
    public void onClick(View v){
        String content = messageContent.getText().toString().trim();
        if(content.isEmpty()){
            Toast.makeText(this, "Can't send empty messages", Toast.LENGTH_SHORT).show();
        }else{
            String receiverUsername = spinner.getSelectedItem().toString();
            Friend receiver = new Friend();
            for(Friend f : mValues){
                if(f.username.equals(receiverUsername)){
                    receiver = f;
                }
            }

            String myId = MyApplicationContext.getInstance().getFirebaseUser().getUid();
            String myUsername = MyApplicationContext.getInstance().getFirebaseUser().getDisplayName();
            DatabaseReference sentRef = FirebaseDatabase.getInstance().getReference(MSG_REF+myId+"/sent");
            DatabaseReference receiverRef = FirebaseDatabase.getInstance().getReference(MSG_REF+receiver.ref+"/received");

            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String creationDate = df.format(new Date());

            Message newMsg = new Message(myId,myUsername,receiver.ref,receiver.username,creationDate,content);
            sentRef.push().setValue(newMsg.toMap());
            receiverRef.push().setValue(newMsg.toMap());
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ButterKnife.bind(this);

        mValues = new ArrayList<>();
        String myId = MyApplicationContext.getInstance().getFirebaseUser().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference(FRIENDS_REF+myId);

        mListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot snap : dataSnapshot.getChildren()) {
                    Friend f = new Friend();
                    f.ref = snap.getKey();
                    f.username = snap.getValue(String.class);
                    if(!mValues.contains(f)){
                        mValues.add(f);
                    }
                }
                populateSpinner(mValues);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        mDatabase.addValueEventListener(mListener);

    }

    private void populateSpinner(List<Friend> friends) {
        List<String> friendsUsername = new ArrayList<>();

        for(Friend f : friends){
            friendsUsername.add(f.username);
        }

        adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, friendsUsername);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDatabase.removeEventListener(mListener);
    }
}
