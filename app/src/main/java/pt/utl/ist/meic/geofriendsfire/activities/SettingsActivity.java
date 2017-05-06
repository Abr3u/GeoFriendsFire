package pt.utl.ist.meic.geofriendsfire.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
}
