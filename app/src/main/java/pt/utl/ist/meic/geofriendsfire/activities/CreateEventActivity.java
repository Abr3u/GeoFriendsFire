package pt.utl.ist.meic.geofriendsfire.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import durdinapps.rxfirebase2.RxFirebaseDatabase;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.location.GPSTracker;
import pt.utl.ist.meic.geofriendsfire.models.Event;

public class CreateEventActivity extends AppCompatActivity {


    private static final String EVENTS_REF = "/events";
    private static final String EVENTS_LOCATIONS_REF = "/eventsLocations";

    @BindView(R.id.categorySpinner)
    Spinner eventCategory;

    @BindView(R.id.eventDescription)
    TextView eventDescription;

    private DatabaseReference mDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        ButterKnife.bind(this);
    }

    @OnClick(R.id.createEventButton)
    public void createNewEventButtonClicked() {

        GPSTracker mGpsTracker = new GPSTracker(this);

        if (!mGpsTracker.canGetLocation()) {
            Toast.makeText(this, "can not get location", Toast.LENGTH_LONG).show();
        }else if(eventDescription.getText().toString().trim().isEmpty()){
            Toast.makeText(this, "plz provide an event description", Toast.LENGTH_SHORT).show();
        }
        else {
            double latitude = mGpsTracker.getLatitude();
            double longitude = mGpsTracker.getLongitude();
            String category = String.valueOf(eventCategory.getSelectedItem());

            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String creationDate = df.format(new Date());

            Event newEvent = new Event("abreu", eventDescription.getText().toString().trim(),category,creationDate);

            // Generate a reference to a new location and add some data using push()
            DatabaseReference eventsRef = mDatabase.child(EVENTS_REF);
            DatabaseReference newEventRef = eventsRef.push();

            // Add some data to the new location
            newEventRef.setValue(newEvent.toMap());

            // Get the unique ID generated by push()
            String eventID = newEventRef.getKey();

            //create geoLocation for the event - key is the event ID
            DatabaseReference ref = mDatabase.child(EVENTS_LOCATIONS_REF);
            GeoFire geoFire = new GeoFire(ref);
            geoFire.setLocation(eventID, new GeoLocation(latitude, longitude));

            finish();
        }
    }
}
