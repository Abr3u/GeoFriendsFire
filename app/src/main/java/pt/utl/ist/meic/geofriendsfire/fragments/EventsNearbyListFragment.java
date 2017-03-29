package pt.utl.ist.meic.geofriendsfire.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.parceler.Parcels;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.activities.CreateEventActivity;
import pt.utl.ist.meic.geofriendsfire.activities.DrawerMainActivity;
import pt.utl.ist.meic.geofriendsfire.adapters.EventsNearbyAdapter;
import pt.utl.ist.meic.geofriendsfire.events.NewDeletedEvent;
import pt.utl.ist.meic.geofriendsfire.events.NewNearbyEvent;
import pt.utl.ist.meic.geofriendsfire.models.Event;
import pt.utl.ist.meic.geofriendsfire.services.EventsNearbyService;

import static android.app.Activity.RESULT_OK;

public class EventsNearbyListFragment extends BaseFragment {

    private static final String PARCEL_VALUES = "values";
    private static final int CREATE_EVENT_REQ_CODE = 1;

    @BindView(R.id.networkDetectorHolder)
    TextView networkDetectorHolder;

    @BindView(R.id.recyclerview)
    RecyclerView recyclerView;

    @BindView(R.id.fab)
    FloatingActionButton fab;

    private EventsNearbyAdapter mAdapter;

    @OnClick(R.id.fab)
    public void onFABClick(View view) {
        showAlertDialog();
    }

    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.dialog_title_create_event));
        builder.setMessage(getString(R.string.dialog_message_create_event));

        String positiveText = getString(android.R.string.ok);
        builder.setPositiveButton(positiveText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(getContext(), CreateEventActivity.class);
                        startActivityForResult(intent,CREATE_EVENT_REQ_CODE);
                    }
                });

        String negativeText = getString(android.R.string.cancel);
        builder.setNegativeButton(negativeText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_list, container, false);
        ButterKnife.bind(this, view);
        fab.setVisibility(View.VISIBLE);
        super.setNetworkDetectorHolder(networkDetectorHolder);
        Log.d("ttt", "oncreateview");
        mAdapter = new EventsNearbyAdapter(getContext());
        populateSavedEvents();
        setupRecyclerView();
        return view;
    }

    private void populateSavedEvents() {
        EventsNearbyService service = MyApplicationContext.getEventsNearbyServiceInstance();
        if(service != null){
            List<Event> savedEvents = service.getValues();
            if(savedEvents!= null && !savedEvents.isEmpty()){
                mAdapter.setValues(savedEvents);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CREATE_EVENT_REQ_CODE){
            if(resultCode == RESULT_OK){
                MyApplicationContext.getEventsNearbyServiceInstance().restartListener();
                ((DrawerMainActivity) getContext()).setupViewPagerEvents();
            }
        }
    }



    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("ttt","onsave");
        outState.putParcelable(PARCEL_VALUES, Parcels.wrap(mAdapter.getValues()));
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if(savedInstanceState != null){
            Log.d("ttt","onRestore");
            mAdapter.setValues(Parcels.unwrap(savedInstanceState.getParcelable(PARCEL_VALUES)));
        }else{
            Log.d("ttt","onRestore era null");
        }
    }


    //
    // EventBus
    //

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(NewDeletedEvent event) {
        mAdapter.removeItem(event.getDeleted());
        if(mAdapter.getItemCount() < MyApplicationContext.getInstance().getMaximumWorkLoad()){
            MyApplicationContext.getEventsNearbyServiceInstance().restartListener();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(NewNearbyEvent event) {
        Log.d("yyy","list recebi novo "+event);
        mAdapter.addItem(event.getNearby());
    }
}
