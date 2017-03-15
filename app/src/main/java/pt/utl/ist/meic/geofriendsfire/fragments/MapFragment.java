package pt.utl.ist.meic.geofriendsfire.fragments;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
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

import org.parceler.Parcels;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.CompositeDisposable;
import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.models.Event;
import pt.utl.ist.meic.geofriendsfire.services.EventsNearbyService;
import pt.utl.ist.meic.geofriendsfire.utils.Utils;

public class MapFragment extends BaseFragment implements GoogleMap.OnCameraChangeListener,
        OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final String PARCEL_VALUES = "values";
    private static final String PARCEL_RADIUS = "radius";

    private static final int INITIAL_ZOOM_LEVEL = 15;
    private static final double INITIAL_RADIUS = 0.1;

    @BindView(R.id.networkDetectorHolder)
    TextView networkDetectorHolder;

    private GoogleMap map;
    private Marker myMarker;
    private View rootView;
    private Location mLastKnownLocation;

    private double mRadius;
    private Set<Event> mValues;
    EventsNearbyService mService;

    private CompositeDisposable mDisposable;

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

        mLastKnownLocation =
                MyApplicationContext.getLocationsServiceInstance().getLastKnownLocation();
        if (mLastKnownLocation == null) {
            Toast.makeText(getContext(), "cant get current location", Toast.LENGTH_LONG).show();
        } else {
            mValues = new HashSet<>();
            mRadius = INITIAL_RADIUS;

            Intent events = new Intent(getContext(), EventsNearbyService.class);
            getContext().bindService(events, eventsConnection, Context.BIND_AUTO_CREATE);
            mDisposable = new CompositeDisposable();

            try {
                rootView = inflater.inflate(R.layout.map_fragment, container, false);
            } catch (InflateException e) {
            }
            ButterKnife.bind(this, rootView);
            super.setNetworkDetectorHolder(networkDetectorHolder);
            setUpMapIfNeeded();

        }
        return rootView;
    }

    private ServiceConnection eventsConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            EventsNearbyService.MyBinder binder = (EventsNearbyService.MyBinder) service;
            mService = binder.getService();

            startMonitoringSettings();
            mService.restartListener();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mDisposable.dispose();
            mService = null;
        }
    };

    private void startMonitoringSettings() {
        mDisposable.add(MyApplicationContext.getInstance()
                .getFurthestEventObservable()
                .subscribe(x -> mService.restartListener())
        );

        mDisposable.add(MyApplicationContext.getInstance()
                .getMaxWorkloadObservable()
                .subscribe(x -> mService.restartListener())
        );
    }

    private void newMarkerFromService(Event event) {
        mValues.add(event);
        this.map.addMarker(new MarkerOptions()
                .position(new LatLng(event.latitude, event.longitude))
                .icon(BitmapDescriptorFactory.defaultMarker(88)));
    }

    public void newRadiusFromService(double radius){
        Map<String,Double> resiDomain = Utils.getBoundingBox(
                MyApplicationContext.getLocationsServiceInstance().getLastKnownLocation(),radius);

        PolylineOptions rectOptions = new PolylineOptions()
                .color(Color.GREEN)
                .width(2)
                .add(new LatLng(resiDomain.get("bot"), resiDomain.get("left")))
                .add(new LatLng(resiDomain.get("bot"), resiDomain.get("right")))
                .add(new LatLng(resiDomain.get("top"), resiDomain.get("right")))
                .add(new LatLng(resiDomain.get("top"), resiDomain.get("left")))
                .add(new LatLng(resiDomain.get("bot"), resiDomain.get("left")));


        this.map.addPolyline(rectOptions);
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
        mDisposable.dispose();
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
        if (mLastKnownLocation != null) {
            LatLng latLngCenter = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
            myMarker = this.map.addMarker(new MarkerOptions()
                    .position(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location)));
            this.map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngCenter, INITIAL_ZOOM_LEVEL));
        }
        mDisposable.add(mService.getEventsNearbyObservable()
                .getObservable()
                .subscribe(x -> newMarkerFromService(x))
        );

        mDisposable.add(mService.getCurrentRadiusObservable()
                .subscribe(x -> newRadiusFromService(x))
        );
        this.map.setOnMarkerClickListener(this);
        this.map.setOnCameraChangeListener(this);
    }

    protected void setupCardView(final Event event) {
        View detailsHolder = rootView.findViewById(R.id.detailsHolder);
        View cardview = detailsHolder.findViewById(R.id.card_view);

        TextView description = (TextView) cardview.findViewById(R.id.iv_text);
        description.setText(event.description);

        TextView extra = (TextView) cardview.findViewById(R.id.iv_extra);
        if (mLastKnownLocation == null) {
            extra.setText(event.creationDate);
        } else {
            double distance = Utils.distance(mLastKnownLocation.getLatitude(), event.latitude,
                    mLastKnownLocation.getLongitude(), event.longitude);
            extra.setText(String.format("%.3f", distance / 1000) + " kms away");
        }

        cardview.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Uri gmmIntentUri = Uri.parse("google.navigation:" +
                        "q=" + event.latitude + "," + event.longitude + "&mode=w");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
                return true;
            }
        });

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
        for (Event event : mValues) {
            if (marker.getPosition().latitude == event.latitude &&
                    marker.getPosition().longitude == event.longitude) {
                setupCardView(event);
                return true;
            }
        }
        return true;
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        View detailsHolder = rootView.findViewById(R.id.detailsHolder);
        detailsHolder.setVisibility(View.GONE);
        LatLng center = cameraPosition.target;

        myMarker.remove();
        myMarker = this.map.addMarker(new MarkerOptions()
                .position(new LatLng(center.latitude, center.longitude))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location)));

        this.map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, cameraPosition.zoom));

        /*
        //TODO: change
        Location mocked = new Location("mocked");
        mocked.setLatitude(center.latitude);
        mocked.setLongitude(center.longitude);
        MyApplicationContext.getLocationsServiceInstance().setMockedLocation(mocked);*/
    }
}
