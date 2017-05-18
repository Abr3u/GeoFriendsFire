package pt.utl.ist.meic.geofriendsfire.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.SignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.parceler.Parcels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.adapters.DynamicViewPagerAdapter;
import pt.utl.ist.meic.geofriendsfire.models.Event;
import pt.utl.ist.meic.geofriendsfire.models.User;
import pt.utl.ist.meic.geofriendsfire.utils.FragmentKeys;

public class DrawerMainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final String PARCEL_FRAGMENT = "fragment";
    private static final String PARCEL_EVENT = "event";

    private static final int CREATE_EVENT_REQ_CODE = 1;

    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @BindView(R.id.nav_view)
    NavigationView mNavigationView;

    @BindView(R.id.viewpager)
    ViewPager mViewPager;

    @BindView(R.id.tabs)
    TabLayout mTabLayout;

    @BindView(R.id.fab)
    FloatingActionButton fab;

    // Firebase
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mDatabase;
    private GoogleApiClient mGoogleApiClient;
    private DynamicViewPagerAdapter mAdapter;

    private int fragment;
    private Event detailedEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer_main);
        ButterKnife.bind(this);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        setupGoogle();
        setupAuth();

        //design
        setupToolBar();
        setupDrawerContent(mNavigationView);

        mAdapter = new DynamicViewPagerAdapter(getSupportFragmentManager(), new ArrayList<FragmentKeys>());
        mViewPager.setAdapter(mAdapter);
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.setVisibility(View.GONE);

        if (savedInstanceState != null) {
            recoverSavedState(savedInstanceState);
        } else {
            setupViewPagerEvents();
        }
    }

    private void recoverSavedState(Bundle savedInstanceState) {
        int savedFrag = Parcels.unwrap(savedInstanceState.getParcelable(PARCEL_FRAGMENT));
        switch (savedFrag) {
            case 0:
                setupViewPagerMap();
                break;
            case 1:
                setupViewPagerEvents();
                break;
            case 2:
                setupViewPagerFriends();
                break;
            case 3:
                setupViewPagerEventDetails((Event) Parcels.unwrap(savedInstanceState.getParcelable(PARCEL_EVENT)));
                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(PARCEL_FRAGMENT, Parcels.wrap(fragment));
        if (detailedEvent != null) {
            outState.putParcelable(PARCEL_EVENT, Parcels.wrap(detailedEvent));
        }
    }

    private void setupToolBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_menu);
        ab.setDisplayHomeAsUpEnabled(true);
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
        Log.d("yyy", "user " + mFirebaseUser);
        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            Log.d("yyy", "nome : " + mFirebaseUser.getDisplayName());
            MyApplicationContext.getInstance().setFirebaseUser(mFirebaseUser);
            MyApplicationContext.getInstance().startServices();
            MyApplicationContext.getInstance().populateFriendsList();
            writeUserIfNeeded(mFirebaseUser);
        }
    }

    private void writeUserIfNeeded(final FirebaseUser firebaseUser) {
        final String userId = MyApplicationContext.getInstance().getFirebaseUser().getUid();
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
        String uid = MyApplicationContext.getInstance().getFirebaseUser().getUid();
        childUpdates.put("/users/" + uid, userValues);

        mDatabase.updateChildren(childUpdates);

        Log.d("yyy", "adicionei user");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    //design

    private void setupDrawerContent(NavigationView navigationView) {

        View header = navigationView.getHeaderView(0);
        FirebaseUser user = MyApplicationContext.getInstance().getFirebaseUser();
        if (user != null) {
            ((TextView) header.findViewById(R.id.navTextView)).setText(user.getDisplayName());
        }

        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.nav_home:
                                setupViewPagerMap();
                                mDrawerLayout.closeDrawers();
                                return true;
                            case R.id.nav_events:
                                setupViewPagerEvents();
                                mDrawerLayout.closeDrawers();
                                return true;
                            case R.id.nav_friends:
                                setupViewPagerFriends();
                                mDrawerLayout.closeDrawers();
                                return true;
                            case R.id.nav_settings:
                                mDrawerLayout.closeDrawers();
                                startActivity(new Intent(DrawerMainActivity.this, SettingsActivity.class));
                                return false;
                            case R.id.nav_messages:
                                setupViewPagerMessages();
                                mDrawerLayout.closeDrawers();
                                return true;
                            default:
                                mDrawerLayout.closeDrawers();
                                return true;
                        }
                    }
                }

        );
    }

    public void setupViewPagerMessages() {
        mAdapter.clear();
        mAdapter.add(FragmentKeys.MessagesReceived);
        mAdapter.add(FragmentKeys.MessagesSent);
        mTabLayout.setVisibility(View.VISIBLE);
        fab.setOnClickListener(x -> startActivity(new Intent(DrawerMainActivity.this, CreateMessageActivity.class)));
        fab.setVisibility(View.VISIBLE);
    }

    public void setupViewPagerFriends() {
        fragment = 2;
        mAdapter.clear();
        mAdapter.add(FragmentKeys.Friends);
        mAdapter.add(FragmentKeys.FriendsSuggestions);
        mAdapter.add(FragmentKeys.FriendSearch);
        mTabLayout.setVisibility(View.VISIBLE);
        fab.setVisibility(View.GONE);
    }

    public void setupViewPagerEvents() {
        fragment = 1;
        mAdapter.clear();
        mAdapter.add(FragmentKeys.EventsNearby);
        mAdapter.add(FragmentKeys.MyEvents);
        mTabLayout.setVisibility(View.VISIBLE);
        fab.setOnClickListener(x -> showAlertDialogCreateEvent());
        fab.setVisibility(View.VISIBLE);
    }

    public void setupViewPagerMap() {
        fragment = 0;
        mAdapter.clear();
        mAdapter.add(FragmentKeys.EventsNearbyMap);
        mTabLayout.setVisibility(View.GONE);
        fab.setOnClickListener(x -> showAlertDialogCreateEvent());
        fab.setVisibility(View.VISIBLE);
    }

    public void setupViewPagerEventDetails(Event event) {
        fragment = 3;
        detailedEvent = event;
        mAdapter.clear();
        mAdapter.setEventForDetails(event);
        mAdapter.add(FragmentKeys.EventDetailsMap);
        mTabLayout.setVisibility(View.GONE);
        fab.setOnClickListener(x -> showAlertDialogCreateEvent());
        fab.setVisibility(View.VISIBLE);
    }

    /*
    *
    * FAB listeners
    *
     */

    private void showAlertDialogCreateEvent() {
        Location lastKnown = MyApplicationContext.getLocationsServiceInstance().getLastKnownLocation();
        if (lastKnown == null) {
            Toast.makeText(DrawerMainActivity.this, "Can't get current location", Toast.LENGTH_SHORT).show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.dialog_title_create_event));
            builder.setMessage(getString(R.string.dialog_message_create_event));

            String positiveText = getString(android.R.string.ok);
            builder.setPositiveButton(positiveText,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(DrawerMainActivity.this, CreateEventActivity.class);
                            startActivityForResult(intent, CREATE_EVENT_REQ_CODE);
                        }
                    });

            String negativeText = getString(android.R.string.cancel);
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
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CREATE_EVENT_REQ_CODE) {
            if (resultCode == RESULT_OK) {
                Log.d("zzz","result OK");
                MyApplicationContext.getEventsNearbyServiceInstance().restartListener();
                //DrawerMainActivity.this.setupViewPagerEvents();
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sample_actions, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.action_sign_out:
                signOut();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void signOut(){
        FirebaseAuth.getInstance().signOut();
        // Google sign out
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        startActivity(new Intent(DrawerMainActivity.this, SignInActivity.class));
                    }
                });
    }
}
