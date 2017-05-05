package pt.utl.ist.meic.geofriendsfire.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.ButterKnife;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.models.Message;
import pt.utl.ist.meic.geofriendsfire.utils.IntentKeys;

public class MessageDetailsActivity extends AppCompatActivity {

    @BindView(R.id.userHolder)
    TextView user;

    @BindView(R.id.dateHolder)
    TextView date;

    @BindView(R.id.textHolder)
    TextView text;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_msg_details);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent i = getIntent();
        if(!i.hasExtra(IntentKeys.messageDetails.toString())){
            Toast.makeText(this, "Can not display messsage", Toast.LENGTH_SHORT).show();
            finish();
        }
        Message msg = Parcels.unwrap(i.getParcelableExtra(IntentKeys.messageDetails.toString()));

        date.setText(msg.sentDate);
        text.setText(msg.content);
        if(i.getBooleanExtra(IntentKeys.isMsgReceived.toString(),true)){
            user.setText("From: "+msg.fromUsername);
        }else{
            user.setText("To: "+msg.toUsername);
        }
    }
}
