package pt.utl.ist.meic.geofriendsfire.fragments;

/**
 * Created by ricar on 09/02/2017.
 */

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;

import pt.utl.ist.meic.geofriendsfire.R;

public class MapFragment extends Fragment implements GoogleMap.OnCameraChangeListener, OnMapReadyCallback{

    private static GeoLocation INITIAL_CENTER = new GeoLocation(38.7097424, -9.4224729);//Casa
    private static final int INITIAL_ZOOM_LEVEL = 14;

    private GoogleMap map;
    private Circle searchCircle;
    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) {
            Log.d("yyy","map onCreate nao era null");
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null) {
                parent.removeView(view);
            }
        }
        try{
        view = inflater.inflate(R.layout.map_fragment, container, false);
            Log.d("yyy","map onCreate try");
        }catch(InflateException e){
            Log.d("yyy","map onCreate catch");
            setUpMap();
            return view;
        }
        setUpMapIfNeeded();
        return view;
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

    public void setUpMapIfNeeded() {
        if (this.map == null) {
            SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            }
        }
        else{
            setUpMap();
        }
    }


    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
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
}
