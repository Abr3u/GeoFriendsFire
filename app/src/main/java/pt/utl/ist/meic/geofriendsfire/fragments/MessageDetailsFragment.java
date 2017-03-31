package pt.utl.ist.meic.geofriendsfire.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.models.Message;

public class MessageDetailsFragment extends BaseFragment {

    @BindView(R.id.networkDetectorHolder)
    TextView networkDetectorHolder;

    @BindView(R.id.userHolder)
    TextView user;

    @BindView(R.id.dateHolder)
    TextView date;

    @BindView(R.id.textHolder)
    TextView text;
    private Message mMessage;
    private boolean isInbox;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message_details, container, false);
        ButterKnife.bind(this,view);
        super.setNetworkDetectorHolder(networkDetectorHolder);

        date.setText(mMessage.sentDate);
        text.setText(mMessage.content);
        if(isInbox){
            user.setText("From: "+mMessage.fromUsername);
        }else{
            user.setText("To: "+mMessage.toUsername);
        }
        return view;
    }

    public void setMessage(Message m,boolean isInbox){
        this.mMessage = m;
        this.isInbox = isInbox;
    }
}
