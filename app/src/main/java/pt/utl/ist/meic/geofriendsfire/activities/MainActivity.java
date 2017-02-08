package pt.utl.ist.meic.geofriendsfire.activities;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

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
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow((null == getCurrentFocus()) ? null
                : getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        mContext.setMaximumWorkLoad(Integer.parseInt(maxWorkLoad.getText().toString().trim()));

        Toast.makeText(this, "New Maximum Workload defined successfully", Toast.LENGTH_SHORT).show();
    }


    @OnClick(R.id.eventsNearbyButton)
    public void eventsNearbyButtonClicked(){
        startActivity(new Intent(this,EventsNearbyActivity.class));
    }


    @OnClick(R.id.createEventButton)
    public void createEventButtonClicked(){
        startActivity(new Intent(this,CreateEventActivity.class));
    }

    @OnClick(R.id.eventsButton)
    public void eventsButtonClicked(){
        startActivity(new Intent(this,EventsActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = (MyApplicationContext)getApplicationContext();

        ButterKnife.bind(this);
    }
}
