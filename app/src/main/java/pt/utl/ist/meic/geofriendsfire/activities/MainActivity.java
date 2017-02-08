package pt.utl.ist.meic.geofriendsfire.activities;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.models.User;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    @BindView(R.id.maxWorkLoadEditText)
    EditText maxWorkLoad;

    MyApplicationContext mContext;
    // Firebase
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mDatabase;
    private GoogleApiClient mGoogleApiClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = (MyApplicationContext)getApplicationContext();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        setupGoogle();
        setupAuth();

        ButterKnife.bind(this);
    }

    /*
    Button listeners
     */

    @OnClick(R.id.maxWorkLoadButton)
    public void maxDistanceButtonClicked(){
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow((null == getCurrentFocus()) ? null
                : getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        mContext.setMaximumWorkLoad(Integer.parseInt(maxWorkLoad.getText().toString().trim()));

        Toast.makeText(this, "New Maximum Workload defined successfully", Toast.LENGTH_SHORT).show();
    }


    @OnClick(R.id.eventsNearbyButton)
    public void eventsNearbyButtonClicked(){
        startActivity(new Intent(this,EventsNearbyActivity.class));
    }


    @OnClick(R.id.createEventButton)
    public void createEventButtonClicked(){
        startActivity(new Intent(this,CreateEventActivity.class));
    }

    @OnClick(R.id.eventsButton)
    public void eventsButtonClicked(){
        startActivity(new Intent(this,EventsActivity.class));
    }


    private void setupGoogle() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();
    }

    private void setupAuth() {
        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        Log.d("yyy","user "+mFirebaseUser);
        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            Log.d("yyy","nome : "+mFirebaseUser.getDisplayName());
            mContext.setFirebaseUser(mFirebaseUser);
            writeUserIfNeeded(mFirebaseUser);
        }
    }

    private void writeUserIfNeeded(final FirebaseUser firebaseUser) {
        final String userId = mContext.getFirebaseUser().getUid();
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Get user value
                        User user = dataSnapshot.getValue(User.class);

                        if (user == null) {
                            Log.d("yyy", "era null");
                            User newUser = new User(firebaseUser.getDisplayName(), firebaseUser.getEmail());
                            writeNewUser(newUser);
                        } else {
                            Log.d("yyy", "nao era null");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w("yyy", "getUser:onCancelled", databaseError.toException());
                    }
                });
    }

    private void writeNewUser(User user) {
        Map<String, Object> userValues = user.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
        String uid = mContext.getFirebaseUser().getUid();
        childUpdates.put("/users/" + uid, userValues);

        mDatabase.updateChildren(childUpdates);

        Log.d("yyy", "adicionei user");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
