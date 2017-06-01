package pt.utl.ist.meic.geofriendsfire.services;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.WindowManager;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.events.NewLocationEvent;

public class LocationTrackingService extends Service {
    private static final String TAG = "ooo";
    private static final String LOCATIONS_REF = "/locations/";
    private static final int LOCATION_AGGREGATION_THRESHOLD = 3;
    private static final int LOCATION_INTERVAL = 5 * 1000;//5 secs
    private static final float LOCATION_DISTANCE = 250f;//meters

    private boolean isGpsEnabled;
    private boolean isNetworkEnabled;

    private LocationManager mLocationManager;
    private DatabaseReference mDatabase;
    private List<TimeLocation> mLocations;
    private Location mLastKnowLocation;

    // Binder given to clients
    private final IBinder mBinder = new MyBinder();

    private class TimeLocation {
        private DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z");
        public Date time;
        public Location location;

        public Map<String, Object> toMap() {
            HashMap<String, Object> result = new HashMap<>();
            result.put("time", df.format(this.time));
            result.put("latitude", this.location.getLatitude());
            result.put("longitude", this.location.getLongitude());
            return result;
        }
    }

    private class LocationListener implements android.location.LocationListener {
        public LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.e("ooo", "onLocationChanged: " + location + " from provider " + location.getProvider());
            TimeLocation tl = new TimeLocation();
            tl.time = new Date();
            tl.location = location;
            mLocations.add(tl);
            if (mLastKnowLocation == null) {
                mLastKnowLocation = location;
                MyApplicationContext.getEventsNearbyServiceInstance().restartListener();
            } else {
                mLastKnowLocation = location;
            }
            EventBus.getDefault().post(new NewLocationEvent(location));

            if (mLocations.size() > LOCATION_AGGREGATION_THRESHOLD) {
                sendPastLocationsFirebase();
                mLocations.clear();
                Log.d(TAG, "cleared locations");
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "onStatusChanged: " + provider + "changed to " + status);
        }
    }


    private void sendPastLocationsFirebase() {
        for (TimeLocation tl : mLocations) {
            Map<String, Object> locationValues = tl.toMap();

            String uid = MyApplicationContext.getInstance().getFirebaseUser().getUid();
            mDatabase.child(LOCATIONS_REF + uid).push().setValue(locationValues);
            Log.d(TAG, "sent location");
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    public class MyBinder extends Binder {
        public LocationTrackingService getService() {
            return LocationTrackingService.this;
        }
    }


    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");
        initLocationManager();
        initLastKnowLocation();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mLocations = new ArrayList<TimeLocation>();

        //observe location changes
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);

            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);

        } catch (java.lang.SecurityException | IllegalArgumentException ex) {
            Log.i("ooo", "fail to request location updates", ex);
        }

    }

    private void initLocationManager() {
        Log.e(TAG, "initLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private void initLastKnowLocation() {
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        isGpsEnabled = mLocationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        // getting network status
        isNetworkEnabled = mLocationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (isGpsEnabled) {
            mLastKnowLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        } else if (isNetworkEnabled) {
            mLastKnowLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    public Location getLastKnownLocation() {
        return mLastKnowLocation;
    }
}