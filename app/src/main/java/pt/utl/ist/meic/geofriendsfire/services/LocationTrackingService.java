package pt.utl.ist.meic.geofriendsfire.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.events.NewLocationEvent;
import pt.utl.ist.meic.geofriendsfire.location.GPSTracker;

public class LocationTrackingService extends Service {

    private List<Location> routeLocations = new ArrayList<Location>();
    private static int counter = 0;

    private static String ROUTE_FILE_NAME = "route.txt";

    private static final String TAG = "trackingService";
    private static final String LOCATIONS_REF = "/locations/";
    private static final int LOCATION_AGGREGATION_THRESHOLD = 5;
    private static final int LOCATION_INTERVAL = 30 * 60 * 1000;//30mins
    private static final float LOCATION_DISTANCE = 250f;//meters

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
            Log.e("ooo", "onLocationChanged: " + location);
            TimeLocation tl = new TimeLocation();
            tl.time = new Date();
            tl.location = location;
            mLocations.add(tl);
            if(mLastKnowLocation == null){
                mLastKnowLocation = location;
                MyApplicationContext.getEventsNearbyServiceInstance().restartListener();
            }else{
                mLastKnowLocation = location;
            }
            EventBus.getDefault().post(new NewLocationEvent(location));

            if (mLocations.size() > LOCATION_AGGREGATION_THRESHOLD) {
                sendLocationsFirebase();
                mLocations.clear();
                Log.d(TAG, "cleared locations");
            }
        }

        private void sendLocationsFirebase() {
            for (TimeLocation tl : mLocations) {
                Map<String, Object> locationValues = tl.toMap();

                String uid = MyApplicationContext.getInstance().getFirebaseUser().getUid();
                mDatabase.child(LOCATIONS_REF + uid).push().setValue(locationValues);
                Log.d(TAG, "sent location");
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + provider);
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


    /*
    *
    * get lat long from ip address
    *
     */
    protected String wifiIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            Log.e("ooo", "Unable to get host address.");
            ipAddressString = null;
        }
        Log.d("ooo", "IP " + ipAddressString);
        return ipAddressString;
    }

    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        return "";
    }

    public void nextRouteLocation(){
        if(routeLocations.isEmpty()){
            Toast.makeText(this, "No route loaded", Toast.LENGTH_SHORT).show();
        }
        else if(counter > routeLocations.size()){
            Toast.makeText(this, "No more points in route", Toast.LENGTH_SHORT).show();
        }else{
            setMockedLocation(routeLocations.get(counter));
            counter++;
        }
    }

    public void loadRoute() throws IOException {
        counter = 0;
        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(sdcard, ROUTE_FILE_NAME);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String line;

        while ((line = bufferedReader.readLine()) != null) {
            String[] latLong = line.split(",");
            Location test = new Location("test");
            test.setLatitude(Double.parseDouble(latLong[1]));
            test.setLongitude(Double.parseDouble(latLong[0]));
            routeLocations.add(test);
        }
        Toast.makeText(this, "Route loaded successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");
        initializeLocationManager();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mLocations = new ArrayList<TimeLocation>();

        GPSTracker tracker = new GPSTracker(this);
        if (tracker.canGetLocation()) {
            Location location = tracker.getLocation();
            mLastKnowLocation = location;
            Log.d("ooo", "tracker location");
            EventBus.getDefault().post(new NewLocationEvent(location));

            //send first location to Firebase
            //TODO: uncomment
            /*TimeLocation first = new TimeLocation();
            first.location = location;
            first.time = new Date();
            Map<String, Object> locationValues = first.toMap();

            String uid = MyApplicationContext.getInstance().getFirebaseUser().getUid();
            mDatabase.child(LOCATIONS_REF+uid).push().setValue(locationValues);*/
        } else {
            Log.d("ooo", "tracker cant get location");
            MyApplicationContext.getInstance().getRetrofitFindIP();
        }

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

    @Override
    public void onDestroy() {
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

    public Location getLastKnownLocation() {
        return mLastKnowLocation;
    }

    public void setMockedLocation(Location mocked) {
        if (mLastKnowLocation == null) {
            this.mLastKnowLocation = mocked;
            EventBus.getDefault().post(new NewLocationEvent(mocked));
        } else if (mocked.getLatitude() != mLastKnowLocation.getLatitude()
                || mocked.getLongitude() != mLastKnowLocation.getLongitude()) {
            this.mLastKnowLocation = mocked;
            EventBus.getDefault().post(new NewLocationEvent(mocked));
        }
    }

}