package pt.utl.ist.meic.geofriendsfire.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import butterknife.BindView;
import butterknife.ButterKnife;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.location.GPSTracker;
import pt.utl.ist.meic.geofriendsfire.models.Event;
import pt.utl.ist.meic.geofriendsfire.utils.Utils;

/**
 * Created by ricar on 11/02/2017.
 */

public class EventDetailsMapFragment extends BaseFragment implements GoogleMap.OnCameraChangeListener,
        OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    @BindView(R.id.networkDetectorHolder)
    TextView networkDetectorHolder;

    private static final int INITIAL_ZOOM_LEVEL = 15;

    private Event mEvent;
    private GoogleMap map;
    private View rootView;
    private GeoLocation mCurrentLocation;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (rootView != null) {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (parent != null) {
                parent.removeView(rootView);
            }
        }
        try {
            rootView = inflater.inflate(R.layout.event_details_map_fragment, container, false);
        } catch (InflateException e) {
            ButterKnife.bind(this,rootView);
            super.setNetworkDetectorHolder(networkDetectorHolder);
            setUpMap();
            return rootView;
        }
        ButterKnife.bind(this,rootView);
        super.setNetworkDetectorHolder(networkDetectorHolder);
        setUpMap();
        setupCardView(mEvent);
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

    public void setEvent(Event event) {
        this.mEvent = event;
    }

    protected void setUpMap() {
        if (this.map == null) {
            SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            }
        } else {
            GPSTracker gpsTracker = new GPSTracker(getContext());
            if (!gpsTracker.canGetLocation()) {
                Toast.makeText(getContext(), "cant get current location", Toast.LENGTH_LONG).show();
            } else {
                mCurrentLocation = new GeoLocation(gpsTracker.getLatitude(), gpsTracker.getLongitude());
                this.map.addMarker(new MarkerOptions()
                        .position(new LatLng(mCurrentLocation.latitude, mCurrentLocation.longitude))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location)));
            }
            this.map.addMarker(new MarkerOptions()
                    .position(new LatLng(mEvent.geoLocation.latitude, mEvent.geoLocation.longitude))
                    .icon(BitmapDescriptorFactory.defaultMarker(88)));
            LatLng latLngCenter = new LatLng(mEvent.geoLocation.latitude, mEvent.geoLocation.longitude);
            this.map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngCenter, INITIAL_ZOOM_LEVEL));
            setupCardView(mEvent);
        }
    }

    protected void setupCardView(final Event event) {
        View detailsHolder = rootView.findViewById(R.id.detailsHolder);
        View cardview = detailsHolder.findViewById(R.id.card_view);

        TextView description = (TextView) cardview.findViewById(R.id.iv_text);
        description.setText(event.description);

        TextView extra = (TextView) cardview.findViewById(R.id.iv_extra);
        if (mCurrentLocation == null || event.geoLocation == null) {
            extra.setText(event.creationDate);
        }else{
            double distance = Utils.distance(mCurrentLocation.latitude, event.geoLocation.latitude, mCurrentLocation.longitude, event.geoLocation.longitude);
            extra.setText(String.format("%.3f", distance / 1000) + " kms away");
        }

        cardview.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Uri gmmIntentUri = Uri.parse("google.navigation:" +
                        "q="+event.geoLocation.latitude+","+event.geoLocation.longitude+"&mode=w");
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
    public void onCameraChange(CameraPosition cameraPosition) {
        View detailsHolder = rootView.findViewById(R.id.detailsHolder);
        detailsHolder.setVisibility(View.GONE);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        setupCardView(mEvent);
        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        MapStyleOptions style = MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.retro_map_style);
        map.setMapStyle(style);
        setUpMap();
    }
}
