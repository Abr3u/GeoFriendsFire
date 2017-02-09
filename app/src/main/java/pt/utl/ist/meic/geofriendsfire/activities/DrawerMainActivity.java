package pt.utl.ist.meic.geofriendsfire.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
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
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.adapters.DynamicViewPagerAdapter;
import pt.utl.ist.meic.geofriendsfire.fragments.EventsNearbyListFragment;
import pt.utl.ist.meic.geofriendsfire.fragments.MapFragment;
import pt.utl.ist.meic.geofriendsfire.fragments.MyEventsListFragment;
import pt.utl.ist.meic.geofriendsfire.models.PagerItem;
import pt.utl.ist.meic.geofriendsfire.models.User;

public class DrawerMainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @BindView(R.id.viewpager)
    ViewPager mViewPager;

    @BindView(R.id.tabs)
    TabLayout mTabLayout;

    private MyApplicationContext mContext;

    // Firebase
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mDatabase;
    private GoogleApiClient mGoogleApiClient;
    private DynamicViewPagerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer_main);
        ButterKnife.bind(this);

        mContext = (MyApplicationContext) getApplicationContext();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        setupGoogle();
        setupAuth();

        //design
        setupToolBar();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            setupDrawerContent(navigationView);
        }

        mAdapter = new DynamicViewPagerAdapter(getSupportFragmentManager(),new ArrayList<PagerItem>());
        mViewPager.setAdapter(mAdapter);
        mTabLayout.setupWithViewPager(mViewPager);
        setupViewPagerEvents();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAlertDialog();
            }
        });
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

    //design

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.nav_home:
                                menuItem.setChecked(true);
                                setupViewPagerMap();
                                mDrawerLayout.closeDrawers();
                                return true;
                            case R.id.nav_events:
                                menuItem.setChecked(true);
                                setupViewPagerEvents();
                                mDrawerLayout.closeDrawers();
                                return true;
                            case R.id.nav_friends:
                                menuItem.setChecked(true);
                                mDrawerLayout.closeDrawers();
                                return true;
                            default:
                                menuItem.setChecked(true);
                                mDrawerLayout.closeDrawers();
                                return true;
                        }
                    }
                }

        );
    }

    private void setupViewPagerEvents() {
        EventsNearbyListFragment frag1 = new EventsNearbyListFragment();
        frag1.setContext(DrawerMainActivity.this);

        MyEventsListFragment frag2 = new MyEventsListFragment();
        frag2.setContext(DrawerMainActivity.this);

        mAdapter.clear();
        mAdapter.add(new PagerItem("Events Nearby", frag1));
        mAdapter.add(new PagerItem("My Events", frag2));

        mTabLayout.setVisibility(View.VISIBLE);
    }

    private void setupViewPagerMap() {
        mAdapter.clear();
        mAdapter.add(new PagerItem("", new MapFragment()));

        mTabLayout.setVisibility(View.GONE);
    }

    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(DrawerMainActivity.this);
        builder.setTitle(getString(R.string.dialog_title));
        builder.setMessage(getString(R.string.dialog_message));

        String positiveText = getString(android.R.string.ok);
        builder.setPositiveButton(positiveText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(DrawerMainActivity.this, CreateEventActivity.class));
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sample_actions, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        switch (AppCompatDelegate.getDefaultNightMode()) {
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
                menu.findItem(R.id.menu_night_mode_system).setChecked(true);
                break;
            case AppCompatDelegate.MODE_NIGHT_AUTO:
                menu.findItem(R.id.menu_night_mode_auto).setChecked(true);
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                menu.findItem(R.id.menu_night_mode_night).setChecked(true);
                break;
            case AppCompatDelegate.MODE_NIGHT_NO:
                menu.findItem(R.id.menu_night_mode_day).setChecked(true);
                break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.menu_night_mode_system:
                setNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case R.id.menu_night_mode_day:
                setNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case R.id.menu_night_mode_night:
                setNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case R.id.menu_night_mode_auto:
                setNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setNightMode(@AppCompatDelegate.NightMode int nightMode) {
        AppCompatDelegate.setDefaultNightMode(nightMode);

        if (Build.VERSION.SDK_INT >= 11) {
            recreate();
        }
    }


}
