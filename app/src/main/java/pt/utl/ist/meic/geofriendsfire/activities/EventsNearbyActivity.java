package pt.utl.ist.meic.geofriendsfire.activities;

import android.content.Intent;
import android.os.Bundle;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;
import java.util.HashMap;

import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.location.GPSTracker;

public class EventsNearbyActivity extends FragmentActivity implements GeoQueryEventListener, GoogleMap.OnCameraChangeListener, OnMapReadyCallback {

    private static GeoLocation INITIAL_CENTER = new GeoLocation(38.7097424, -9.4224729);//Lisboa
    private static final int INITIAL_ZOOM_LEVEL = 14;

    private static final String EVENTS_LOCATIONS_REF = "/eventsLocations";

    private GoogleMap map;
    private Circle searchCircle;
    private GeoFire geoFire;
    private GeoQuery geoQuery;

    private Map<String, Marker> markers;
    private int preferedWorkLoad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events_nearby);

        Intent intent = getIntent();
        if(intent.hasExtra("initialCenterLati") && intent.hasExtra("initialCenterLongi")){
            double lati = intent.getDoubleExtra("initialCenterLati",38.7097424);
            double longi = intent.getDoubleExtra("initialCenterLongi",-9.4224729);
            this.INITIAL_CENTER = new GeoLocation(lati,longi);
        }


        GPSTracker gpsTracker = new GPSTracker(this);
        if (!gpsTracker.canGetLocation()) {
            Toast.makeText(this, "cant get current location", Toast.LENGTH_LONG).show();
        } else {
            this.markers = new HashMap<String, Marker>();

            MyApplicationContext context = (MyApplicationContext) getApplicationContext();
            preferedWorkLoad = context.getMaximumWorkLoad();

            GeoLocation currentLocation = new GeoLocation(gpsTracker.getLatitude(), gpsTracker.getLongitude());

            // setup map and camera position
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);

            // setup GeoFire
            this.geoFire = new GeoFire(FirebaseDatabase.getInstance().getReference(EVENTS_LOCATIONS_REF));

            // radius in km
            this.geoQuery = this.geoFire.queryAtLocation(currentLocation, 1);
        }


    }


    @Override
    public void onMapReady(GoogleMap map) {
        Log.d("yyy", "onMapReady");

        this.map = map;
        setUpMap();

    }

    private void setUpMap() {
        LatLng latLngCenter = new LatLng(INITIAL_CENTER.latitude, INITIAL_CENTER.longitude);
        this.searchCircle = this.map.addCircle(new CircleOptions().center(latLngCenter).radius(1000));
        this.searchCircle.setFillColor(Color.argb(66, 255, 0, 255));
        this.searchCircle.setStrokeColor(Color.argb(66, 0, 0, 0));
        this.map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngCenter, INITIAL_ZOOM_LEVEL));
        this.map.setOnCameraChangeListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // remove all event listeners to stop updating in the background
        this.geoQuery.removeAllListeners();
        for (Marker marker : this.markers.values()) {
            marker.remove();
        }
        this.markers.clear();
    }

    @Override
    protected void onStart() {
        super.onStart();
        MyApplicationContext context = (MyApplicationContext) getApplicationContext();
        preferedWorkLoad = context.getMaximumWorkLoad();
        // add an event listener to start updating locations again
        this.geoQuery.addGeoQueryEventListener(this);
    }


    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        if (this.markers.size() < preferedWorkLoad) {
            Log.d("yyy","nao passava do workload ---- "+this.markers.size()+ " < "+preferedWorkLoad);
            // Add a new marker to the map
            Marker marker = this.map.addMarker(new MarkerOptions().position(new LatLng(location.latitude, location.longitude)));
            this.markers.put(key, marker);
        }else{
            Log.d("yyy","ja passava do workload");
        }
    }

    @Override
    public void onKeyExited(String key) {
        // Remove any old marker
        Marker marker = this.markers.get(key);
        if (marker != null) {
            marker.remove();
            this.markers.remove(key);
        }
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        // Move the marker
        Marker marker = this.markers.get(key);
        if (marker != null) {
            this.animateMarkerTo(marker, location.latitude, location.longitude);
        }
    }

    @Override
    public void onGeoQueryReady() {
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

    // Animation handler for old APIs without animation support
    private void animateMarkerTo(final Marker marker, final double lat, final double lng) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final long DURATION_MS = 3000;
        final Interpolator interpolator = new AccelerateDecelerateInterpolator();
        final LatLng startPosition = marker.getPosition();
        handler.post(new Runnable() {
            @Override
            public void run() {
                float elapsed = SystemClock.uptimeMillis() - start;
                float t = elapsed / DURATION_MS;
                float v = interpolator.getInterpolation(t);

                double currentLat = (lat - startPosition.latitude) * v + startPosition.latitude;
                double currentLng = (lng - startPosition.longitude) * v + startPosition.longitude;
                marker.setPosition(new LatLng(currentLat, currentLng));

                // if animation is not finished yet, repeat
                if (t < 1) {
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

    private double zoomLevelToRadius(double zoomLevel) {
        // Approximation to fit circle into view
        return 16384000 / Math.pow(2, zoomLevel);
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        // Update the search criteria for this geoQuery and the circle on the map
        LatLng center = cameraPosition.target;
        double radius = zoomLevelToRadius(cameraPosition.zoom);
        this.searchCircle.setCenter(center);
        this.searchCircle.setRadius(radius);
        this.geoQuery.setCenter(new GeoLocation(center.latitude, center.longitude));
        // radius in km
        this.geoQuery.setRadius(radius / 1000);
    }
}
