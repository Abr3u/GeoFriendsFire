package pt.utl.ist.meic.geofriendsfire.activities;

import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoLocation;
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
import com.google.firebase.auth.FirebaseAuth;

import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.ButterKnife;
import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.fragments.BaseFragment;
import pt.utl.ist.meic.geofriendsfire.fragments.MapFragment;
import pt.utl.ist.meic.geofriendsfire.models.Event;
import pt.utl.ist.meic.geofriendsfire.utils.IntentKeys;
import pt.utl.ist.meic.geofriendsfire.utils.Utils;

public class EventDetailsMapActivity extends AppCompatActivity implements GoogleMap.OnCameraChangeListener,
        OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    SupportMapFragment mapFragment;

    private static final int INITIAL_ZOOM_LEVEL = 15;

    private Event mEvent;
    private GoogleMap map;
    private GeoLocation mCurrentLocation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent i = getIntent();
        if(!i.hasExtra(IntentKeys.eventDetails.toString())){
            Toast.makeText(this, "Can not display event", Toast.LENGTH_SHORT).show();
            finish();
        }

        mEvent = Parcels.unwrap(i.getParcelableExtra(IntentKeys.eventDetails.toString()));
        setUpMap();
        setupCardView(mEvent);
    }

    protected void setUpMap() {
        if (this.map == null) {
            mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                mapFragment.getMapAsync(this);
        } else {
            Location lastKnownLocation =
                    MyApplicationContext.getLocationsServiceInstance().getLastKnownLocation();
            if (lastKnownLocation == null) {
                Toast.makeText(this, "cant get current location", Toast.LENGTH_LONG).show();
            } else {
                mCurrentLocation = new GeoLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                this.map.addMarker(new MarkerOptions()
                        .position(new LatLng(mCurrentLocation.latitude, mCurrentLocation.longitude))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location)));
            }
            this.map.addMarker(new MarkerOptions()
                    .position(new LatLng(mEvent.latitude, mEvent.longitude))
                    .icon(BitmapDescriptorFactory.defaultMarker(88)));
            LatLng latLngCenter = new LatLng(mEvent.latitude, mEvent.longitude);
            this.map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngCenter, INITIAL_ZOOM_LEVEL));
            setupCardView(mEvent);
        }
    }

    protected void setupCardView(final Event event) {
        View detailsHolder = findViewById(R.id.detailsHolder);
        View cardview = detailsHolder.findViewById(R.id.card_view);

        TextView description = (TextView) cardview.findViewById(R.id.iv_text);
        description.setText(event.description);

        TextView extra = (TextView) cardview.findViewById(R.id.iv_extra);
        if (mCurrentLocation == null) {
            extra.setText(event.creationDate);
        }else{
            double distance = Utils.distance(mCurrentLocation.latitude, event.latitude, mCurrentLocation.longitude, event.longitude);
            extra.setText(String.format("%.3f", distance / 1000) + " kms away");
        }

        cardview.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Uri gmmIntentUri = Uri.parse("google.navigation:" +
                        "q="+event.latitude+","+event.longitude+"&mode=w");
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
        View detailsHolder = findViewById(R.id.detailsHolder);
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
        MapStyleOptions style = MapStyleOptions.loadRawResourceStyle(this, R.raw.retro_map_style);
        map.setMapStyle(style);
        setUpMap();
    }
}
