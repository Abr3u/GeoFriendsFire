package pt.utl.ist.meic.geofriendsfire;

import android.app.Application;

import com.google.firebase.auth.FirebaseUser;

import java.io.Serializable;

public class MyApplicationContext extends Application{

    private FirebaseUser firebaseUser;
    private int maximumWorkLoad;

    @Override
    public void onCreate() {
        super.onCreate();
        maximumWorkLoad = 1;
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
}
