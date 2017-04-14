package pt.utl.ist.meic.geofriendsfire;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

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
import pt.utl.ist.meic.geofriendsfire.models.GeoIpResponse;
import pt.utl.ist.meic.geofriendsfire.models.IpAddressResponse;
import pt.utl.ist.meic.geofriendsfire.network.GeoIpEndpoints;
import pt.utl.ist.meic.geofriendsfire.network.IdentMeEndpoints;
import pt.utl.ist.meic.geofriendsfire.services.EventsNearbyService;
import pt.utl.ist.meic.geofriendsfire.services.LocationTrackingService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MyApplicationContext extends Application{

    private static final String FRIENDS_REF = "/friends/";
    public static final String FIND_IP_BASE_URL = "http://ident.me";
    public static final String FIND_LOCATION_BASE_URL = "http://freegeoip.net";

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

    public void getRetrofitFindIP(){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(FIND_IP_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        IdentMeEndpoints apiService =
                retrofit.create(IdentMeEndpoints.class);

        Call<IpAddressResponse> call = apiService.getIpAddress();
        call.enqueue(new Callback<IpAddressResponse>() {
            @Override
            public void onResponse(Call<IpAddressResponse> call, Response<IpAddressResponse> response) {
                int statusCode = response.code();
                IpAddressResponse ipResponse = response.body();
                String ip = ipResponse.getAddress();
                Log.d("ooo","IP "+ip);
                Toast.makeText(MyApplicationContext.this, "IP "+ip, Toast.LENGTH_SHORT).show();
                getRetrofitFindGeoLocation(ip);
            }

            @Override
            public void onFailure(Call<IpAddressResponse> call, Throwable t) {
                // Log error here since request failed
            }
        });
    }

    public void getRetrofitFindGeoLocation(String ip){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(FIND_LOCATION_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GeoIpEndpoints apiService =
                retrofit.create(GeoIpEndpoints.class);

        Call<GeoIpResponse> call = apiService.getLocationInfoFromIp(ip);
        call.enqueue(new Callback<GeoIpResponse>() {
            @Override
            public void onResponse(Call<GeoIpResponse> call, Response<GeoIpResponse> response) {
                int statusCode = response.code();
                GeoIpResponse geoIpResponse = response.body();
                String city = geoIpResponse.getCity();
                Log.d("ooo","city "+city);
                Toast.makeText(MyApplicationContext.this, "City "+city, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<GeoIpResponse> call, Throwable t) {
                // Log error here since request failed
            }
        });
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
