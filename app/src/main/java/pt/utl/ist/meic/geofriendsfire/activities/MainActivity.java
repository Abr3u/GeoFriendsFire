package pt.utl.ist.meic.geofriendsfire.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;

public class MainActivity extends AppCompatActivity {

    MyApplicationContext mContext;

    @BindView(R.id.maxWorkLoadEditText)
    EditText maxWorkLoad;


    @OnClick(R.id.maxWorkLoadButton)
    public void maxDistanceButtonClicked(){
        mContext.setMaximumWorkLoad(Integer.parseInt(maxWorkLoad.getText().toString().trim()));
    }


    @OnClick(R.id.eventsNearbyButton)
    public void eventsNearbyClicked(){
        startActivity(new Intent(this,EventsNearbyActivity.class));
    }


    @OnClick(R.id.createEventButton)
    public void createEventClicked(){
        startActivity(new Intent(this,CreateEventActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = (MyApplicationContext)getApplicationContext();

        ButterKnife.bind(this);
    }
}
