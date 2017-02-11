package pt.utl.ist.meic.geofriendsfire.fragments;

/**
 * Created by ricar on 09/02/2017.
 */


import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.location.GPSTracker;
import pt.utl.ist.meic.geofriendsfire.models.Event;

public class MapFragment extends Fragment implements GeoQueryEventListener, GoogleMap.OnCameraChangeListener,
        OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final String EVENTS_LOCATIONS_REF = "/eventsLocations";
    private static final String EVENTS_REF = "/events";

    private static GeoLocation INITIAL_CENTER = new GeoLocation(38.7097424, -9.4224729);//Casa
    private static final int INITIAL_ZOOM_LEVEL = 15;
    private static final double INITIAL_RADIUS = 0.5;

    private GoogleMap map;
    private View rootView;
    private GeoFire geoFire;
    private GeoQuery geoQuery;

    private Map<String, Marker> markers;
    private Context mContext;
    private double mRadius;

    private Map<String, Event> mEventsMap;
    private List<Event> mValues;
    private GeoLocation mCurrentLocation;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);
        if (rootView != null) {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (parent != null) {
                parent.removeView(rootView);
            }
        }

        GPSTracker gpsTracker = new GPSTracker(mContext);
        if (!gpsTracker.canGetLocation()) {
            Toast.makeText(mContext, "cant get current location", Toast.LENGTH_LONG).show();
        } else {
            this.markers = new HashMap<String, Marker>();
            this.mEventsMap = new HashMap<>();
            this.mValues = new ArrayList<>();
            this.mRadius = INITIAL_RADIUS;

            mCurrentLocation = new GeoLocation(gpsTracker.getLatitude(), gpsTracker.getLongitude());

            try {
                rootView = inflater.inflate(R.layout.map_fragment, container, false);
            } catch (InflateException e) {
                setUpMap();
                return rootView;
            }
            setUpMapIfNeeded();

            // setup GeoFire
            this.geoFire = new GeoFire(FirebaseDatabase.getInstance().getReference(EVENTS_LOCATIONS_REF));

            // radius in km
            this.geoQuery = this.geoFire.queryAtLocation(mCurrentLocation, mRadius);

        }
        return rootView;
    }

    @Override
    public void onDestroyView() {
        SupportMapFragment f = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        if (f != null) {
            try {
                getFragmentManager().beginTransaction().remove(f).commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onDestroyView();
    }

    protected void setUpMapIfNeeded() {
        if (this.map == null) {
            SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            }
        } else {
            setUpMap();
        }
    }


    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        View detailsHolder = rootView.findViewById(R.id.detailsHolder);
        detailsHolder.setVisibility(View.GONE);
        /*LatLng center = cameraPosition.target;
        double radius = zoomLevelToRadius(currentZoom);
        this.geoQuery.setCenter(new GeoLocation(center.latitude, center.longitude));
        // radius in km
        this.geoQuery.setRadius(radius / 1000);
        myLocationMarker.remove();
        myLocationMarker = this.map.addMarker(new MarkerOptions()
                .position(new LatLng(center.latitude, center.longitude))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location)));*/
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        setUpMap();
    }

    protected void setUpMap() {
        LatLng latLngCenter = new LatLng(INITIAL_CENTER.latitude, INITIAL_CENTER.longitude);
        this.map.addMarker(new MarkerOptions()
                .position(new LatLng(mCurrentLocation.latitude, mCurrentLocation.longitude))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location)));

        this.map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngCenter, INITIAL_ZOOM_LEVEL));
        this.map.setOnMarkerClickListener(this);
        this.map.setOnCameraChangeListener(this);
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    protected void setupCardView(Event event) {
        View detailsHolder = rootView.findViewById(R.id.detailsHolder);
        View cardview = detailsHolder.findViewById(R.id.card_view);

        TextView description = (TextView) cardview.findViewById(R.id.iv_text);
        description.setText(event.description);

        TextView creationDate = (TextView) cardview.findViewById(R.id.iv_extra);
        creationDate.setText(event.creationDate);

        ImageView category = (ImageView) cardview.findViewById(R.id.iv_image);

        switch (event.category) {
            case "Food":
                Glide.with(category.getContext())
                        .load(R.drawable.ic_food)
                        .fitCenter()
                        .into(category);
                break;
            case "Sports":
                Glide.with(category.getContext())
                        .load(R.drawable.ic_sports)
                        .fitCenter()
                        .into(category);
                break;
            case "Shop":
                Glide.with(category.getContext())
                        .load(R.drawable.ic_shopping)
                        .fitCenter()
                        .into(category);
                break;

        }
        detailsHolder.setVisibility(View.VISIBLE);
    }


    @Override
    public boolean onMarkerClick(final Marker marker) {
        for (String key : markers.keySet()) {
            if (markers.get(key).equals(marker)) {
                setupCardView(mEventsMap.get(key));
                return true;
            }
        }
        return true;
    }


    /*
    GeoFire-----------------------------------------------
     */

    @Override
    public void onStop() {
        super.onStop();
        // remove all event listeners to stop updating in the background
        this.geoQuery.removeAllListeners();
        for (Marker marker : this.markers.values()) {
            marker.remove();
        }
        this.mValues.clear();
        this.mEventsMap.clear();
        this.markers.clear();
    }

    @Override
    public void onStart() {
        super.onStart();
        // add an event listener to start updating locations again
        this.geoQuery.addGeoQueryEventListener(this);
    }


    @Override
    public void onKeyEntered(final String key, final GeoLocation location) {
        int maxWorkload = ((MyApplicationContext) mContext.getApplicationContext()).getMaximumWorkLoad();
        if (this.markers.size() < maxWorkload) {
            Marker marker = this.map.addMarker(new MarkerOptions()
                    .position(new LatLng(location.latitude, location.longitude))
                    .icon(BitmapDescriptorFactory.defaultMarker(88)));
            this.markers.put(key, marker);

            // New Event nearby
            FirebaseDatabase.getInstance().getReference(EVENTS_REF).child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Event v = dataSnapshot.getValue(Event.class);
                    v.geoLocation = location;
                    v.setRef(key);
                    mValues.add(v);
                    mEventsMap.put(key, v);
                    Log.d("yyy", "nearby - item inserted - " + v.toString());
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("yyy", databaseError.toString());
                }
            });
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

        // Remove any old event
        Event event = this.mEventsMap.get(key);
        if (event != null) {
            // Remove data from the list
            mEventsMap.remove(event);
            mValues.remove(event);
            // Update the RecyclerView
            Log.d("yyy", "nearby - item removed - " + event.toString());
        }
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        // Move the marker
        Marker marker = this.markers.get(key);
        if (marker != null) {
            this.animateMarkerTo(marker, location.latitude, location.longitude);
        }

        Event event = this.mEventsMap.get(key);
        if (event != null) {
            event.geoLocation = location;
        }
    }

    @Override
    public void onGeoQueryReady() {
        int maxWorkload = ((MyApplicationContext) mContext.getApplicationContext()).getMaximumWorkLoad();
        if (this.markers.size() < maxWorkload) {
            Log.d("yyy", this.mValues.size() + " era mais pequeno que o WL " + maxWorkload);
            mRadius = mRadius + 0.2;
            geoQuery.setRadius(mRadius);
        }
    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        new AlertDialog.Builder(mContext)
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
        // Approximation to fit circle into rootView
        return 16384000 / Math.pow(2, zoomLevel);
    }

}
