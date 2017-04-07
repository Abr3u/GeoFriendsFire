package pt.utl.ist.meic.geofriendsfire.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.greenrobot.eventbus.EventBus;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.events.NewLocationEvent;
import pt.utl.ist.meic.geofriendsfire.location.GPSTracker;

public class LocationTrackingService extends Service
{
    private static final String TAG = "trackingService";
    private static final String LOCATIONS_REF = "/locations/";
    private static final int LOCATION_AGGREGATION_THRESHOLD = 5;
    private static final int LOCATION_INTERVAL = 30*60*1000;//30mins
    private static final float LOCATION_DISTANCE = 250f;//meters

    private LocationManager mLocationManager;
    private DatabaseReference mDatabase;
    private List<TimeLocation> mLocations;
    private Location mLastKnowLocation;

    // Binder given to clients
    private final IBinder mBinder = new MyBinder();

    private class TimeLocation{
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

    private class LocationListener implements android.location.LocationListener
    {
        public LocationListener(String provider)
        {
            Log.e(TAG, "LocationListener " + provider);
        }

        @Override
        public void onLocationChanged(Location location)
        {
            Log.e(TAG, "onLocationChanged: " + location);
            TimeLocation tl = new TimeLocation();
            tl.time = new Date();
            tl.location = location;
            mLocations.add(tl);
            mLastKnowLocation = location;
            EventBus.getDefault().post(new NewLocationEvent(location));

            if(mLocations.size() > LOCATION_AGGREGATION_THRESHOLD){
                sendLocationsFirebase();
                mLocations.clear();
                Log.d(TAG,"cleared locations");
            }
        }

        private void sendLocationsFirebase() {
            for(TimeLocation tl : mLocations) {
                Map<String, Object> locationValues = tl.toMap();

                String uid = MyApplicationContext.getInstance().getFirebaseUser().getUid();
                mDatabase.child(LOCATIONS_REF+uid).push().setValue(locationValues);
                Log.d(TAG, "sent location");
            }
        }

        @Override
        public void onProviderDisabled(String provider)
        {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0)
    {
        return mBinder;
    }

    public class MyBinder extends Binder {
        public LocationTrackingService getService() {
            return LocationTrackingService.this;
        }
    }


    @Override
    public void onCreate()
    {
        Log.e(TAG, "onCreate");
        initializeLocationManager();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mLocations = new ArrayList<TimeLocation>();

        GPSTracker tracker = new GPSTracker(this);
        if(tracker.canGetLocation()){
            Location location = tracker.getLocation();
            mLastKnowLocation = location;
            EventBus.getDefault().post(new NewLocationEvent(location));

            //send first location to Firebase
            //TODO: uncomment
            /*TimeLocation first = new TimeLocation();
            first.location = location;
            first.time = new Date();
            Map<String, Object> locationValues = first.toMap();

            String uid = MyApplicationContext.getInstance().getFirebaseUser().getUid();
            mDatabase.child(LOCATIONS_REF+uid).push().setValue(locationValues);*/
        }

        //observe location changes
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException | IllegalArgumentException ex) {
            Log.i(TAG, "fail to request location update GPS", ex);
            try {
                mLocationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                        mLocationListeners[0]);
            } catch (java.lang.SecurityException | IllegalArgumentException ex1) {
                Log.i(TAG, "fail to request location update network", ex1);
            }
        }

    }

    @Override
    public void onDestroy()
    {
        Log.e(TAG, "onDestroy");
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

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    public Location getLastKnownLocation(){return mLastKnowLocation;}

    public void setMockedLocation(Location mocked){
        if(mLastKnowLocation == null){
            this.mLastKnowLocation = mocked;
            EventBus.getDefault().post(new NewLocationEvent(mocked));
        }
        else if(mocked.getLatitude() != mLastKnowLocation.getLatitude()
                || mocked.getLongitude() != mLastKnowLocation.getLongitude()){
            this.mLastKnowLocation = mocked;
            EventBus.getDefault().post(new NewLocationEvent(mocked));
        }
    }

}