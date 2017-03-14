package pt.utl.ist.meic.geofriendsfire;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.google.firebase.auth.FirebaseUser;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import io.reactivex.subjects.PublishSubject;
import pt.utl.ist.meic.geofriendsfire.services.EventsNearbyService;
import pt.utl.ist.meic.geofriendsfire.services.LocationTrackingService;


public class MyApplicationContext extends Application{

    private RefWatcher refWatcher;
    private static MyApplicationContext instance;
    private static LocationTrackingService mLocationTrackingService;

    private FirebaseUser firebaseUser;

    private int maximumWorkLoad;
    private int furthestEvent;
    private PublishSubject<Integer> maxWorkloadObservable;
    private PublishSubject<Integer> furthestEventObservable;

    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        instance = (MyApplicationContext) getApplicationContext();
        refWatcher = LeakCanary.install(this);
        maximumWorkLoad = 2;
        furthestEvent = 5;

        maxWorkloadObservable = PublishSubject.create();
        furthestEventObservable = PublishSubject.create();
        maxWorkloadObservable.onNext(maximumWorkLoad);
        furthestEventObservable.onNext(furthestEvent);

        Intent locations = new Intent(this, LocationTrackingService.class);
        bindService(locations, locationConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection locationConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            LocationTrackingService.MyBinder binder = (LocationTrackingService.MyBinder) service;
            mLocationTrackingService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    public static MyApplicationContext getInstance() {
        return instance;
    }

    public static LocationTrackingService getLocationsServiceInstance() {
        return mLocationTrackingService;
    }

    public RefWatcher getRefWatcher() {
        return refWatcher;
    }

    public void setFirebaseUser(FirebaseUser firebaseUser) {
        this.firebaseUser = firebaseUser;
    }

    public FirebaseUser getFirebaseUser() {
        return this.firebaseUser;
    }

    public int getMaximumWorkLoad() {
        return maximumWorkLoad;
    }

    public void setMaximumWorkLoad(int maximumWorkLoad) {
        this.maximumWorkLoad = maximumWorkLoad;
        this.maxWorkloadObservable.onNext(maximumWorkLoad);
    }

    public int getFurthestEvent() {
        return furthestEvent;
    }

    public void setFurthestEvent(int furthestEvent) {
        this.furthestEvent = furthestEvent;
        this.furthestEventObservable.onNext(furthestEvent);
    }

    public PublishSubject<Integer> getMaxWorkloadObservable() {
        return maxWorkloadObservable;
    }

    public PublishSubject<Integer> getFurthestEventObservable() {
        return furthestEventObservable;
    }
}
