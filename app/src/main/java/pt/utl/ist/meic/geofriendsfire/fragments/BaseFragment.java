package pt.utl.ist.meic.geofriendsfire.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.TextView;

import com.squareup.leakcanary.RefWatcher;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.PublishProcessor;
import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;

public class BaseFragment
        extends Fragment {

    public void setNetworkDetectorHolder(TextView networkDetectorHolder) {
        this.networkDetectorHolder = networkDetectorHolder;
    }

    private TextView networkDetectorHolder;

    private BroadcastReceiver broadcastReceiver;
    private Disposable disposable;
    private PublishProcessor<Boolean> publishProcessor;

    @Override
    public void onStart() {
        super.onStart();
        publishProcessor = PublishProcessor.create();

        disposable = publishProcessor
                .startWith(getConnectivityStatus(getActivity()))
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(online -> {
                    if (online) {
                        networkDetectorHolder.setVisibility(View.GONE);
                    }
                    else {
                        networkDetectorHolder.setVisibility(View.VISIBLE);
                        networkDetectorHolder.setText("You are offline");
                    }
                });

        listenToNetworkConnectivity();
    }

    @Override
    public void onStop() {
        super.onStop();

        disposable.dispose();
        getActivity().unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = MyApplicationContext.getInstance().getRefWatcher();
        refWatcher.watch(this);
    }

    private void listenToNetworkConnectivity() {

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                publishProcessor.onNext(getConnectivityStatus(context));
            }
        };

        final IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        getActivity().registerReceiver(broadcastReceiver, intentFilter);
    }

    private boolean getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
