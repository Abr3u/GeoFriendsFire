package pt.utl.ist.meic.geofriendsfire.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.parceler.Parcel;
import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.ButterKnife;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.fragments.BaseFragment;
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
