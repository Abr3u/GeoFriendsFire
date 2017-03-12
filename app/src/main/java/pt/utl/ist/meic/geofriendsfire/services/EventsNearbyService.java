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

import org.reactivestreams.Subscriber;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.disposables.CompositeDisposable;
import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.location.GPSTracker;
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
    private Utils.ObservableList<Event> mValues;

    private GeoQuery geoQuery;
    private GeoLocation mCurrentLocation;
    private double mCurrentRadius;

    private Map<String, Double> residentialDomainLimits;
    private Subscriber subscriber;

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
        int maxWorkload = MyApplicationContext.getInstance().getMaximumWorkLoad();
        int furthestEvent = MyApplicationContext.getInstance().getFurthestEvent();

        if (this.mValues.list.size() < maxWorkload && mCurrentRadius < furthestEvent) {
            // New Event nearby
            FirebaseDatabase.getInstance().getReference(EVENTS_REF).child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Event v = dataSnapshot.getValue(Event.class);
                    v.geoLocation = location;
                    v.setRef(key);
                    mValues.add(v);
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
            int eventIndex = mValues.list.indexOf(event);
            mEventsMap.remove(event);
            mValues.list.remove(event);
        }
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        Event event = this.mEventsMap.get(key);
        if (event != null) {
            event.geoLocation = location;
        }
    }

    @Override
    public void onGeoQueryReady() {
        int maxWorkload = MyApplicationContext.getInstance().getMaximumWorkLoad();
        int furthestEvent = MyApplicationContext.getInstance().getFurthestEvent();
        if (this.mValues.list.size() < maxWorkload && mCurrentRadius < furthestEvent) {
            Log.d("aaa", "NEARBY :: " + this.mValues.list.size() + " era mais pequeno que o WL " + maxWorkload);
            mCurrentRadius = mCurrentRadius + 0.2;
            geoQuery.setRadius(mCurrentRadius);
        } else {
            //ja tem os events maximos ou ja passou do furthest limit
            cleanupListener();
            calculateResidentialDomainLimits();
            startMonitoringCurrentLocation();
        }
        Log.d("aaa", "nearby Current radius -> " + mCurrentRadius);
    }

    private void startMonitoringCurrentLocation() {
        Log.d("ttt", "startMonitoring");

        CompositeDisposable disposable = new CompositeDisposable();

        disposable.add(MyApplicationContext.getLocationsServiceInstance()
                .getLastKnownLocation()
                .subscribe(x -> {
                    Log.d("ttt","monitoring "+x);
                    if (isOutside(x)) {
                        initListener();
                        disposable.dispose();
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
        MyApplicationContext.getLocationsServiceInstance()
                .getLastKnownLocation()
                .subscribe(x -> {
                    residentialDomainLimits = Utils.getBoundingBox(x, mCurrentRadius);
                });
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

    @Override
    public void onCreate() {
        super.onCreate();
        initListener();
    }

    public void initListener() {
        mEventsMap = new HashMap<>();
        mValues = new Utils.ObservableList<Event>();
        residentialDomainLimits = new HashMap<String, Double>();
        this.mCurrentRadius = MIN_RADIUS;

        GPSTracker tracker = new GPSTracker(this);
        if (tracker.canGetLocation()) {
            this.mCurrentLocation = new GeoLocation(tracker.getLatitude(), tracker.getLongitude());

            this.geoQuery = new GeoFire(FirebaseDatabase.getInstance().getReference(EVENTS_LOCATIONS_REF))
                    .queryAtLocation(mCurrentLocation, mCurrentRadius);

            this.geoQuery.addGeoQueryEventListener(this);
        }
    }

    public void cleanupListener() {
        if (geoQuery != null) {
            geoQuery.removeAllListeners();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    public Utils.ObservableList<Event> getEventsNearby() {
        return mValues;
    }
}
