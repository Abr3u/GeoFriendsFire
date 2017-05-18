package pt.utl.ist.meic.geofriendsfire.fragments;


import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.parceler.Parcels;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.OnClick;
import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.activities.EventDetailsMapActivity;
import pt.utl.ist.meic.geofriendsfire.events.NewLocationEvent;
import pt.utl.ist.meic.geofriendsfire.events.NewNearbyEvent;
import pt.utl.ist.meic.geofriendsfire.events.NewResidentDomainEvent;
import pt.utl.ist.meic.geofriendsfire.models.Event;
import pt.utl.ist.meic.geofriendsfire.services.EventsNearbyService;
import pt.utl.ist.meic.geofriendsfire.utils.IntentKeys;

public class MapFragment extends BaseFragment implements GoogleMap.OnCameraChangeListener,
        OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final String PARCEL_VALUES = "values";
    private static final String PARCEL_RADIUS = "radius";

    private static final int INITIAL_ZOOM_LEVEL = 15;
    private static final double INITIAL_RADIUS = 0.1;
    private static final int CREATE_EVENT_REQ_CODE = 1;

    private GoogleMap map;
    private Marker myMarker;
    private Polyline mPolyline;
    private View rootView;
    private Location mLastKnownLocation;

    private float currentZoom = INITIAL_ZOOM_LEVEL;

    private double mRadius;
    private Set<Event> mValues;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        try {
            rootView = inflater.inflate(R.layout.map_fragment, container, false);
        } catch (InflateException e) {
        }

        mLastKnownLocation =
                MyApplicationContext.getLocationsServiceInstance().getLastKnownLocation();
        if (mLastKnownLocation == null) {
            Toast.makeText(getContext(), "cant get current location", Toast.LENGTH_LONG).show();
        } else {
            mValues = new HashSet<>();
            mRadius = INITIAL_RADIUS;
            ButterKnife.bind(this, rootView);
            setUpMapIfNeeded();
        }
        return rootView;
    }

    private void populateSavedEvents() {
        EventsNearbyService service = MyApplicationContext.getEventsNearbyServiceInstance();
        if (service != null) {
            List<Event> savedEvents = service.getValues();
            if (savedEvents != null && !savedEvents.isEmpty()) {
                for (Event e : savedEvents) {
                    newMarkerFromService(e);
                }
            }
            Map<String, Double> resiDomain = service.getResidentialDomainLimits();
            if (resiDomain != null && !resiDomain.isEmpty()) {
                drawResiDomain(resiDomain);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }


    private void newMarkerFromService(Event event) {
        mValues.add(event);
        this.map.addMarker(new MarkerOptions()
                .position(new LatLng(event.latitude, event.longitude))
                .icon(BitmapDescriptorFactory.defaultMarker(88)));
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


    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            Log.d("ttt", "onRestore");
            mValues = Parcels.unwrap(savedInstanceState.getParcelable(PARCEL_VALUES));
            mRadius = Parcels.unwrap(savedInstanceState.getParcelable(PARCEL_RADIUS));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(PARCEL_VALUES, Parcels.wrap(mValues));
        outState.putParcelable(PARCEL_RADIUS, Parcels.wrap(mRadius));
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
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        MapStyleOptions style = MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.retro_map_style);
        map.setMapStyle(style);
        setUpMap();
    }

    protected void setUpMap() {
        Location lastKnownLocation = MyApplicationContext.getLocationsServiceInstance().getLastKnownLocation();
        if (lastKnownLocation != null) {
            double latitude = lastKnownLocation.getLatitude();
            double longitude = lastKnownLocation.getLongitude();
            drawMyLocation(latitude, longitude);
            this.map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), INITIAL_ZOOM_LEVEL));
        }
        populateSavedEvents();

        this.map.setOnMarkerClickListener(this);
        this.map.setOnCameraChangeListener(this);
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        for (Event event : mValues) {
            if (marker.getPosition().latitude == event.latitude &&
                    marker.getPosition().longitude == event.longitude) {
                Intent i = new Intent(getContext(), EventDetailsMapActivity.class);
                i.putExtra(IntentKeys.eventDetails.toString(),Parcels.wrap(event));
                startActivity(i);
                return true;
            }
        }
        return true;
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        LatLng center = cameraPosition.target;
        currentZoom = cameraPosition.zoom;
        //TODO: change

//        Location mocked = new Location("mocked");
//        mocked.setLatitude(center.latitude);
//        mocked.setLongitude(center.longitude);
//        MyApplicationContext.getLocationsServiceInstance().setMockedLocation(mocked);
//
//        this.map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, cameraPosition.zoom));

    }


    //
    // EventBus
    //

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(NewNearbyEvent event) {
        Log.d("yyy", "mapa recebi novo event " + event);
        newMarkerFromService(event.getNearby());
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(NewLocationEvent event) {
        Log.d("ooo", "mapa recebi nova location " + event.getLocation());
        drawMyLocation(event.getLocation().getLatitude(), event.getLocation().getLongitude());
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(NewResidentDomainEvent event) {
        drawResiDomain(event.getResiDomain());
    }

    private void drawMyLocation(double latitude, double longitude) {
        if (myMarker != null) {
            myMarker.remove();
        }
        myMarker = this.map.addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location)));

        CameraUpdate location = CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude), currentZoom);
        map.animateCamera(location);
    }

    private void drawResiDomain(Map<String, Double> resiDomain) {
        PolylineOptions rectOptions = new PolylineOptions()
                .color(Color.GREEN)
                .width(2)
                .add(new LatLng(resiDomain.get("bot"), resiDomain.get("left")))
                .add(new LatLng(resiDomain.get("bot"), resiDomain.get("right")))
                .add(new LatLng(resiDomain.get("top"), resiDomain.get("right")))
                .add(new LatLng(resiDomain.get("top"), resiDomain.get("left")))
                .add(new LatLng(resiDomain.get("bot"), resiDomain.get("left")));

        if (mPolyline != null) {
            mPolyline.remove();
        }
        mPolyline = this.map.addPolyline(rectOptions);
    }

}
