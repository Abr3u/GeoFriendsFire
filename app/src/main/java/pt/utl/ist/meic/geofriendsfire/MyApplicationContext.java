package pt.utl.ist.meic.geofriendsfire;

import android.app.Application;

import com.google.firebase.auth.FirebaseUser;


public class MyApplicationContext extends Application{

    private FirebaseUser firebaseUser;
    private int maximumWorkLoad;
    private int furthestEvent;

    @Override
    public void onCreate() {
        super.onCreate();
        maximumWorkLoad = 4;
        furthestEvent = 20;
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
