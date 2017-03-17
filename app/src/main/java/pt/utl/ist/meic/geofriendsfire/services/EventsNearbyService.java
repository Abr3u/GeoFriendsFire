package pt.utl.ist.meic.geofriendsfire.services;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.PublishSubject;
import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.events.NearbyEvent;
import pt.utl.ist.meic.geofriendsfire.events.NewSettingsEvent;
import pt.utl.ist.meic.geofriendsfire.models.Event;
import pt.utl.ist.meic.geofriendsfire.utils.Utils;

/**
 * Created by ricar on 12/03/2017.
 */

public class EventsNearbyService extends Service implements GeoQueryEventListener {

    private static final String EVENTS_LOCATIONS_REF = "/eventsLocations";
    private static final String EVENTS_REF = "/events";
    private static final double MIN_RADIUS = 0.1;

    private Map<String, Event> mEventsMap;
    private List<Event> mValues;
    private PublishSubject<Double> mCurrentRadiusObservable;
    CompositeDisposable mDisposable = new CompositeDisposable();

    private GeoQuery geoQuery;
    private GeoLocation mCurrentLocation;
    private double mCurrentRadius;

    private Map<String, Double> residentialDomainLimits;
    private int mFurthest;
    private int mWorkload;

    // Binder given to clients
    private final IBinder mBinder = new MyBinder();

  /*
    *
    *
    * GeoFire Listeners
    *
    * */


    @Override
    public void onKeyEntered(final String key, final GeoLocation location) {
        if (this.mValues.size() < mWorkload && mCurrentRadius < mFurthest) {
            // New Event nearby
            FirebaseDatabase.getInstance().getReference(EVENTS_REF).child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Event v = dataSnapshot.getValue(Event.class);
                    v.latitude = location.latitude;
                    v.longitude = location.longitude;
                    v.setRef(key);
                    Log.d("ttt","add event "+v);
                    mValues.add(v);
                    mEventsMap.put(key, v);
                    EventBus.getDefault().post(new NearbyEvent(v));
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("ena", databaseError.toString());
                }
            });

        }
    }

    @Override
    public void onKeyExited(String key) {
        // Remove any old event
        Event event = this.mEventsMap.get(key);
        if (event != null) {
            // Remove data from the list
            mEventsMap.remove(event);
            mValues.remove(event);
        }
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        Event event = this.mEventsMap.get(key);
        if (event != null) {
            event.latitude = location.latitude;
            event.longitude = location.longitude;
        }
    }

    @Override
    public void onGeoQueryReady() {
        if (this.mValues.size() < mWorkload && mCurrentRadius < mFurthest) {
            Log.d("ttt", "onReady :: " + this.mValues.size() + " era mais pequeno que o WL " + mWorkload);
            mCurrentRadius = mCurrentRadius + 0.2;
            geoQuery.setRadius(mCurrentRadius);
        } else {
            Log.d("ttt", "ja pssou algum maximo com Current radius -> " + mCurrentRadius
                    +" // "+mValues.size()+" tamanho" );
            //ja tem os events maximos ou ja passou do furthest limit
            mCurrentRadiusObservable.onNext(mCurrentRadius);
            cleanupListener();
            calculateResidentialDomainLimits();
            startMonitoringCurrentLocation();
        }
    }

    private void startMonitoringCurrentLocation() {
        Log.d("ttt", "startMonitoringCurrentLocation");
        MyApplicationContext.getLocationsServiceInstance()
                .getLastKnownLocationObservable()
                .forEach(x-> {
                    Log.d("ttt","recebido "+x + " // "+residentialDomainLimits.size());
                    if (isOutside(x)) {
                        Log.d("ttt","estava outside");
                        residentialDomainLimits.clear();
                        restartListener();
                    }
                });
    }

    private void debugResiDomain(){
        for(Map.Entry<String,Double> entry : residentialDomainLimits.entrySet()){
            Log.d("ttt","entry -> "+entry.getKey()+" // "+entry.getValue());
        }
    }

    public boolean isOutside(Location location) {
        if(residentialDomainLimits.isEmpty()){
            calculateResidentialDomainLimits();
        }

        return location.getLongitude() < residentialDomainLimits.get("left")
                || location.getLongitude() > residentialDomainLimits.get("right")
                || location.getLatitude() < residentialDomainLimits.get("bot")
                || location.getLatitude() > residentialDomainLimits.get("top");
    }

    private void calculateResidentialDomainLimits() {
        residentialDomainLimits = Utils.getBoundingBox(MyApplicationContext.getLocationsServiceInstance()
                .getLastKnownLocation(), mCurrentRadius);
        debugResiDomain();
    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("There was an unexpected error querying GeoFire: " + error.getMessage())
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class MyBinder extends Binder {
        public EventsNearbyService getService() {
            // Return this instance of LocalService so clients can call public methods
            return EventsNearbyService.this;
        }
    }

    public void restartListener() {
        restartVars();

        Location lastKnowLocation =
                MyApplicationContext.getLocationsServiceInstance().getLastKnownLocation();
        if (lastKnowLocation != null) {
            Log.d("ttt","criei listeners geofire");
            this.mCurrentLocation = new GeoLocation(lastKnowLocation.getLatitude(), lastKnowLocation.getLongitude());

            this.geoQuery = new GeoFire(FirebaseDatabase.getInstance().getReference(EVENTS_LOCATIONS_REF))
                    .queryAtLocation(mCurrentLocation, mCurrentRadius);

            this.geoQuery.addGeoQueryEventListener(this);
        }
    }

    public void cleanupListener() {
        Log.d("ttt","cleanupListener");
        if (geoQuery != null) {
            geoQuery.removeAllListeners();
        }
    }

    public void initVars(){
        mDisposable = new CompositeDisposable();
        mEventsMap = new HashMap<>();
        mValues = new ArrayList<>();
        mCurrentRadiusObservable = PublishSubject.create();
        residentialDomainLimits = new HashMap<String, Double>();
        this.mCurrentRadius = MIN_RADIUS;

        mFurthest = MyApplicationContext.getInstance().getFurthestEvent();
        mWorkload = MyApplicationContext.getInstance().getMaximumWorkLoad();
    }

    public void restartVars(){
        mDisposable.dispose();
        mEventsMap.clear();
        mValues.clear();

        this.mCurrentRadius = MIN_RADIUS;
        mFurthest = MyApplicationContext.getInstance().getFurthestEvent();
        mWorkload = MyApplicationContext.getInstance().getMaximumWorkLoad();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initVars();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(NewSettingsEvent event) {
        Log.d("yyy","service restarting due to new setts");
        this.restartListener();
    }

    public PublishSubject<Double> getCurrentRadiusObservable() {
        return mCurrentRadiusObservable;
    }
}
