package pt.utl.ist.meic.geofriendsfire.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pt.utl.ist.meic.geofriendsfire.MyApplicationContext;
import pt.utl.ist.meic.geofriendsfire.R;
import pt.utl.ist.meic.geofriendsfire.models.User;


public class SettingsActivity extends AppCompatActivity {

    @BindView(R.id.workloadSeekBar)
    SeekBar workloadSeekbar;

    @BindView(R.id.workloadTextView)
    TextView workloadText;

    @BindView(R.id.furthestEventSeekBar)
    SeekBar furthestSeekbar;

    @BindView(R.id.furthestEventTextView)
    TextView furthestText;

    @BindView(R.id.crossingsCheckbBox)
    CheckBox crossingsCheckBox;

    private static final String USERS_REF = "/users";

    private Integer newWorkload;
    private Integer newFurthest;
    private String suggestions;

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
        String uid = ((MyApplicationContext) getApplicationContext()).getFirebaseUser().getUid();

        workloadSeekbar.setProgress(currentWorkload);
        furthestSeekbar.setProgress(furthestEvent);
        workloadText.setText(getResources().getString(R.string.preferredWorkloadHolder) + currentWorkload);
        furthestText.setText(getResources().getString(R.string.furthestRangeHolder) + furthestEvent + " kms away");

        workloadSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (i == 0) {
                    workloadText.setText(getResources().getString(R.string.preferredWorkloadHolder) + 1);
                    newWorkload = 1;
                } else {
                    workloadText.setText(getResources().getString(R.string.preferredWorkloadHolder) + i);
                    newWorkload = i;
                }
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
                if (i == 0) {
                    furthestText.setText(getResources().getString(R.string.furthestRangeHolder) + 1 + " kms away");
                    newFurthest = 1;
                } else {
                    furthestText.setText(getResources().getString(R.string.furthestRangeHolder) + i + " kms away");
                    newFurthest = i;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        FirebaseDatabase.getInstance().getReference(USERS_REF).child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Log.d("yyy", "onDataChanged " + dataSnapshot);
                        User u = dataSnapshot.getValue(User.class);
                        updateCheckBoxState(u.suggestions);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }


    private void updateCheckBoxState(String suggestions) {
        crossingsCheckBox.setChecked(suggestions.equals("SAMEPLACETIME"));
    }

    private void applySettings() {
        if (newWorkload != null) {
            ((MyApplicationContext) getApplicationContext()).setMaximumWorkLoad(newWorkload);
        }
        if (newFurthest != null) {
            ((MyApplicationContext) getApplicationContext()).setFurthestEvent(newFurthest);
        }

        String uid = ((MyApplicationContext) getApplicationContext()).getFirebaseUser().getUid();

        Map<String, Object> updates = new HashMap<>();
        if (crossingsCheckBox.isChecked()) {
            updates.put("suggestions", "SAMEPLACETIME");
        } else {
            updates.put("suggestions", "NORMAL");
        }

        FirebaseDatabase.getInstance().getReference(USERS_REF).child(uid).updateChildren(updates);

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
