package pt.utl.ist.meic.geofriendsfire.activities;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;


public class SettingsActivity extends AppCompatActivity {

    @BindView(R.id.workloadSeekBar)
    SeekBar workloadSeekbar;

    @BindView(R.id.workloadTextView)
    TextView workloadText;

    @BindView(R.id.furthestEventSeekBar)
    SeekBar furthestSeekbar;

    @BindView(R.id.furthestEventTextView)
    TextView furthestText;

    private Integer newWorkload;
    private Integer newFurthest;
    private AlertDialog mDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);
        setupUI();
    }

    private void setupUI() {

        int currentWorkload = ((MyApplicationContext) getApplicationContext()).getMaximumWorkLoad();
        int furthestEvent = ((MyApplicationContext) getApplicationContext()).getFurthestEvent();

        workloadSeekbar.setProgress(currentWorkload);
        furthestSeekbar.setProgress(furthestEvent);
        workloadText.setText(getResources().getString(R.string.preferredWorkloadHolder) + currentWorkload);
        furthestText.setText(getResources().getString(R.string.furthestRangeHolder)+furthestEvent+" kms away");

        workloadSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                workloadText.setText(getResources().getString(R.string.preferredWorkloadHolder) + i);
                newWorkload = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        furthestSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                furthestText.setText(getResources().getString(R.string.furthestRangeHolder) + i + " kms away");
                newFurthest = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void applySettings() {
        if (newWorkload != null) {
            ((MyApplicationContext) getApplicationContext()).setMaximumWorkLoad(newWorkload);
        }
        if(newFurthest != null){
            ((MyApplicationContext) getApplicationContext()).setFurthestEvent(newFurthest);
        }
        Toast.makeText(this, "Settings Saved Successfully", Toast.LENGTH_LONG).show();
        finish();
    }

    @OnClick(R.id.applySettingsButton)
    public void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");
        builder.setMessage("Save changes ?");

        String positiveText = getString(android.R.string.yes);
        builder.setPositiveButton(positiveText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        applySettings();
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


    /*
    toolbar stuff
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sample_actions, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        switch (AppCompatDelegate.getDefaultNightMode()) {
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
                menu.findItem(R.id.menu_night_mode_system).setChecked(true);
                break;
            case AppCompatDelegate.MODE_NIGHT_AUTO:
                menu.findItem(R.id.menu_night_mode_auto).setChecked(true);
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                menu.findItem(R.id.menu_night_mode_night).setChecked(true);
                break;
            case AppCompatDelegate.MODE_NIGHT_NO:
                menu.findItem(R.id.menu_night_mode_day).setChecked(true);
                break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_night_mode_system:
                setNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case R.id.menu_night_mode_day:
                setNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case R.id.menu_night_mode_night:
                setNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case R.id.menu_night_mode_auto:
                setNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setNightMode(@AppCompatDelegate.NightMode int nightMode) {
        AppCompatDelegate.setDefaultNightMode(nightMode);

        if (Build.VERSION.SDK_INT >= 11) {
            recreate();
        }
    }
}
