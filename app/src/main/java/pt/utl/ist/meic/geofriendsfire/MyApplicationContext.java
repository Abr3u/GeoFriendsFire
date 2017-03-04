package pt.utl.ist.meic.geofriendsfire;

import android.app.Application;

import com.google.firebase.auth.FirebaseUser;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;


public class MyApplicationContext extends Application{

    private RefWatcher refWatcher;
    private static MyApplicationContext instance;

    private FirebaseUser firebaseUser;
    private int maximumWorkLoad;
    private int furthestEvent;

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
        maximumWorkLoad = 4;
        furthestEvent = 20;
    }

    public static MyApplicationContext getInstance() {
        return instance;
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
    }

    public int getFurthestEvent() {
        return furthestEvent;
    }

    public void setFurthestEvent(int furthestEvent) {
        this.furthestEvent = furthestEvent;
    }
}
