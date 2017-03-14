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

import java.util.HashMap;
import java.util.Map;

import io.reactivex.disposables.CompositeDisposable;
import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
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
    private Utils.ObservableList<Event> mValuesObservable;

    private GeoQuery geoQuery;
    private GeoLocation mCurrentLocation;
    private double mCurrentRadius;

    private Map<String, Double> residentialDomainLimits;
    CompositeDisposable mDisposable = new CompositeDisposable();
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
        if (this.mValuesObservable.list.size() < mWorkload && mCurrentRadius < mFurthest) {
            // New Event nearby
            FirebaseDatabase.getInstance().getReference(EVENTS_REF).child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Event v = dataSnapshot.getValue(Event.class);
                    v.latitude = location.latitude;
                    v.longitude = location.longitude;
                    v.setRef(key);
                    Log.d("ttt","add event "+v);
                    mValuesObservable.add(v);
                    mEventsMap.put(key, v);
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
            int eventIndex = mValuesObservable.list.indexOf(event);
            mEventsMap.remove(event);
            mValuesObservable.list.remove(event);
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
        if (this.mValuesObservable.list.size() < mWorkload && mCurrentRadius < mFurthest) {
            Log.d("ttt", "onReady :: " + this.mValuesObservable.list.size() + " era mais pequeno que o WL " + mWorkload);
            mCurrentRadius = mCurrentRadius + 0.2;
            geoQuery.setRadius(mCurrentRadius);
        } else {
            Log.d("ttt", "ja pssou algum maximo com Current radius -> " + mCurrentRadius
                    +" // "+mValuesObservable.list.size()+" tamanho" );
            //ja tem os events maximos ou ja passou do furthest limit
            cleanupListener();
            calculateResidentialDomainLimits();
            startMonitoringCurrentLocation();
        }
    }

    private void startMonitoringSettings() {
        mDisposable.add(MyApplicationContext.getInstance()
                .getFurthestEventObservable()
                .subscribe(x -> {
                    Log.d("ttt","mudou furthest");
                    mFurthest = x;
                    initListener();
                })
        );

        mDisposable.add(MyApplicationContext.getInstance()
                .getMaxWorkloadObservable()
                .subscribe(x -> {
                    Log.d("ttt","mudou WorkLoad");
                    mWorkload = x;
                    initListener();
                })
        );

    }

    private void startMonitoringCurrentLocation() {
        Log.d("ttt", "startMonitoringCurrentLocation");

        mDisposable.add(MyApplicationContext.getLocationsServiceInstance()
                .getLastKnownLocationObservable()
                .subscribe(x -> {
                    Log.d("ttt","monitoring Location "+x);
                    if (isOutside(x)) {
                        initListener();
                    }
                })
        );
    }

    private boolean isOutside(Location location) {
        Log.d("ttt", "isOutside");
        return location.getLatitude() < residentialDomainLimits.get("left")
                || location.getLatitude() > residentialDomainLimits.get("right")
                || location.getLongitude() < residentialDomainLimits.get("bot")
                || location.getLongitude() > residentialDomainLimits.get("top");
    }

    private void calculateResidentialDomainLimits() {
        residentialDomainLimits = Utils.getBoundingBox(MyApplicationContext.getLocationsServiceInstance()
                .getLastKnownLocation(), mCurrentRadius);
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

    public void initListener() {
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
        mValuesObservable = new Utils.ObservableList<>();
        residentialDomainLimits = new HashMap<String, Double>();
        this.mCurrentRadius = MIN_RADIUS;

        mFurthest = MyApplicationContext.getInstance().getFurthestEvent();
        mWorkload = MyApplicationContext.getInstance().getMaximumWorkLoad();
        startMonitoringSettings();
    }

    public void restartVars(){
        mDisposable.dispose();
        mEventsMap.clear();
        residentialDomainLimits.clear();
        this.mCurrentRadius = MIN_RADIUS;
        mValuesObservable.list.clear();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initVars();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    public Utils.ObservableList<Event> getEventsNearbyObservable() {
        return mValuesObservable;
    }
}
