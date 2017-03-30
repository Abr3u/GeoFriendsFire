package pt.utl.ist.meic.geofriendsfire;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import pt.utl.ist.meic.geofriendsfire.events.NewSettingsEvent;
import pt.utl.ist.meic.geofriendsfire.models.Friend;
import pt.utl.ist.meic.geofriendsfire.services.EventsNearbyService;
import pt.utl.ist.meic.geofriendsfire.services.LocationTrackingService;


public class MyApplicationContext extends Application{

    private static final String FRIENDS_REF = "/friends/";

    private RefWatcher refWatcher;
    private static MyApplicationContext instance;
    private static LocationTrackingService mLocationTrackingService;
    private static EventsNearbyService mEventsNearbyService;

    private FirebaseUser firebaseUser;
    private List<Friend> myFriends;

    private int maximumWorkLoad;
    private int furthestEvent;

    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        instance = (MyApplicationContext) getApplicationContext();
        refWatcher = LeakCanary.install(this);
        maximumWorkLoad = 2;
        furthestEvent = 5;
        myFriends = new ArrayList<>();
    }

    public void populateFriendsList() {
        String myId = this.firebaseUser.getUid();
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference(FRIENDS_REF + myId);

        ValueEventListener mListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snap : dataSnapshot.getChildren()) {
                    Friend f = new Friend();
                    f.ref = snap.getKey();
                    f.username = snap.getValue(String.class);
                    if (!myFriends.contains(f)) {
                        myFriends.add(f);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        mDatabase.addListenerForSingleValueEvent(mListener);
    }

    public void startServices(){
        Intent locations = new Intent(this, LocationTrackingService.class);
        bindService(locations, locationConnection, Context.BIND_AUTO_CREATE);

        Intent events = new Intent(this, EventsNearbyService.class);
        bindService(events, eventsConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection locationConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            LocationTrackingService.MyBinder binder = (LocationTrackingService.MyBinder) service;
            mLocationTrackingService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mLocationTrackingService = null;
        }
    };

    private ServiceConnection eventsConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            EventsNearbyService.MyBinder binder = (EventsNearbyService.MyBinder) service;
            mEventsNearbyService = binder.getService();
            mEventsNearbyService.restartListener();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mEventsNearbyService = null;
        }
    };

    public List<Friend> getMyFriends(){return this.myFriends;}

    public void addFriend(Friend f){
        if(!this.myFriends.contains(f)) {
            this.myFriends.add(f);
        }
    }

    public void removeFriend(Friend f){
        if(this.myFriends.contains(f)){
            this.myFriends.remove(f);
        }
    }

    public static MyApplicationContext getInstance() {
        return instance;
    }

    public static LocationTrackingService getLocationsServiceInstance() {
        return mLocationTrackingService;
    }

    public static EventsNearbyService getEventsNearbyServiceInstance(){
        return mEventsNearbyService;
    }

    public RefWatcher getRefWatcher() {
        return refWatcher;
    }

    public void setFirebaseUser(FirebaseUser firebaseUser) {
        this.firebaseUser = firebaseUser;
    }

    public FirebaseUser getFirebaseUser() {
        return this.firebaseUser;
    }

    public int getMaximumWorkLoad() {
        return maximumWorkLoad;
    }

    public void setMaximumWorkLoad(int maximumWorkLoad) {
        this.maximumWorkLoad = maximumWorkLoad;
        EventBus.getDefault().post(new NewSettingsEvent());
    }

    public int getFurthestEvent() {
        return furthestEvent;
    }

    public void setFurthestEvent(int furthestEvent) {
        this.furthestEvent = furthestEvent;
        EventBus.getDefault().post(new NewSettingsEvent());
    }
}
