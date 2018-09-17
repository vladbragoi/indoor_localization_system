package it.univr.vlad.fingerprinting.view;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import es.dmoral.toasty.Toasty;
import it.univr.vlad.fingerprinting.Application;
import it.univr.vlad.fingerprinting.Fingerprint;
import it.univr.vlad.fingerprinting.activity.MainActivity;
import it.univr.vlad.fingerprinting.templates.Node;
import it.univr.vlad.fingerprinting.R;
import it.univr.vlad.fingerprinting.Timer;
import it.univr.vlad.fingerprinting.model.CBLDatabase;
import it.univr.vlad.fingerprinting.devices.mv.MagneticVector;
import it.univr.vlad.fingerprinting.util.Dialog;
import it.univr.vlad.fingerprinting.viewmodel.NodeViewModel;

public class FingerprintingFragment extends Fragment implements Timer.TimerListener,
        SpeedDialView.OnActionSelectedListener, SpeedDialView.OnChangeListener {

    private SpeedDialView mSpeedDialView;
    private TextView mDirection;
    private TextView mTimerSeconds;
    private TextView mTimerMinutes;
    private NodeListAdapter mAdapter;
    public AppCompatCheckBox mWifiCheckbox;
    public AppCompatCheckBox mBeaconCheckbox;
    public AppCompatCheckBox mMagneticVectorCheckbox;

    private Timer mTimer;
    private int seconds = 0;

    private CBLDatabase mDatabase;
    private NodeViewModel mViewModel;

    private Fingerprint mCurrentFingerprint;

    private final Observer<List<Node>> mWifiNodesObserver = nodes -> {
        if (mCurrentFingerprint != null) mCurrentFingerprint.addWifiNodes(nodes);
        mAdapter.addWifiNodes(nodes);
    };

    private final Observer<List<Node>> mBeaconNodesObserver = nodes -> {
        if (mCurrentFingerprint != null) mCurrentFingerprint.addBeaconNodes(nodes);
        mAdapter.addBeaconNodes(nodes);
    };

    private final Observer<MagneticVector> mMagneticVectorObserver = mv -> {
        if (mCurrentFingerprint != null) mCurrentFingerprint.addMagneticVector(mv);
        mAdapter.setMv(mv);
    };

    private Observer<MagneticVector> mDirectionObserver = magneticVector ->
            mDirection.setText(magneticVector != null ? magneticVector.toString() : "NORTH");

    ///////////////////////////////////////////////
    //            PUBLIC METHODS
    ///////////////////////////////////////////////

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new NodeListAdapter();
        mTimer = new Timer();

        mTimer.setTimerListener(this);

        Activity activity = getActivity();
        if (activity != null) {
            Application application = (Application) activity.getApplication();
            mDatabase = application.getFingerprintingDatabase();
            if (mDatabase != null) mDatabase.startPushReplication(true);

            if (savedInstanceState == null && activity instanceof MainActivity) {
                ((MainActivity) activity).turnLocationOn();
            }
        }

        mViewModel = ViewModelProviders.of(this).get(NodeViewModel.class);
        mViewModel.getMv().observe(this, mDirectionObserver);
        mViewModel.getWifiList().observe(this, mWifiNodesObserver);
        mViewModel.getBeaconList().observe(this, mBeaconNodesObserver);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fingerprinting, container, false);
        mSpeedDialView = rootView.findViewById(R.id.start_stop_button);
        mDirection = rootView.findViewById(R.id.directionValue);
        mTimerSeconds = rootView.findViewById(R.id.secondsValue);
        mTimerMinutes = rootView.findViewById(R.id.minutesValue);

        RecyclerView mRecyclerView = rootView.findViewById(R.id.nodesRecyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        //mRecyclerView.setItemAnimator(null);
        mRecyclerView.setAdapter(mAdapter);

        setupSpeedDial(savedInstanceState == null);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mDatabase != null && !mDatabase.isRunning()) mDatabase.start();
        mViewModel.startMvScanning();
    }

    @Override
    public void onStop() {
        stopTimer();
        if (mDatabase != null && mDatabase.isRunning()) mDatabase.stop();
        mViewModel.stopMvScanning();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        mTimer.destroy();
        mViewModel.getWifiList().removeObserver(mWifiNodesObserver);
        mViewModel.getBeaconList().removeObserver(mBeaconNodesObserver);
        mViewModel.getMv().removeObserver(mDirectionObserver);
        super.onDestroy();
    }

    /**
     * Closes SpeedDial menu if it's opened.
     * @return true if SpeedDial menu is been closed.
     */
    public boolean closeSpeedDial() {
        if (mSpeedDialView.isOpen()) {
            mSpeedDialView.close();
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * @param hours
     * @param minutes
     * @param seconds
     */
    @Override
    public void onTimeChanged(String hours, String minutes, String seconds) {
        Context context = getContext();
        if (!hours.equals("0") && context != null) {
            Toasty.normal(context,
                    "hours: " + hours,
                    Toast.LENGTH_SHORT,
                    ContextCompat.getDrawable(context, R.drawable.ic_access_time_white_24dp)
            ).show();
        }
        mTimerMinutes.setText(minutes);
        mTimerSeconds.setText(seconds);
    }

    /**
     * {@inheritDoc}
     * @param status
     */
    @Override
    public void onTimerStopped(Timer.TimerStatus status) {
        if (status.equals(Timer.TimerStatus.STOPPED)) stopTimer();
    }

    @Override
    public boolean onActionSelected(SpeedDialActionItem actionItem) {
        Context context = getContext();
        switch (actionItem.getId()) {
            case R.id.fab_save:
                if (mDatabase == null && context != null) {
                    Toasty.info(context,
                            getString(R.string.sync_button_press),
                            Toast.LENGTH_SHORT
                    ).show();
                }
                else if (mCurrentFingerprint != null && mDatabase != null) {
                    mCurrentFingerprint.saveInto(mDatabase.unwrapDatabase());
                    mCurrentFingerprint = null;
                }
                break;
            case R.id.fab_stop:
                stopTimer();
                break;
        }

        if (mCurrentFingerprint != null) {
            mCurrentFingerprint.stopMeasuring();
        }

        Drawable drawable = AppCompatResources.getDrawable(
                Objects.requireNonNull(getContext()),
                R.drawable.ic_play_arrow_white_24dp);
        mSpeedDialView.setMainFabOpenedDrawable(drawable);
        return false; // True to keep the Speed Dial open
    }

    @Override
    public boolean onMainActionSelected() {
        showStartDialog();
        return false; // True to keep the Speed Dial open
    }

    @Override
    public void onToggleChanged(boolean isOpen) { }

    ///////////////////////////////////////////////
    //            PRIVATE METHODS
    ///////////////////////////////////////////////

    /**
     * Check whether location is enabled or not.
     * @param locationManager Location manager
     * @return true if location is enabled, false otherwise
     */
    @Deprecated
    private boolean isLocationEnabled(@NotNull LocationManager locationManager) {
        boolean gps_enabled;
        boolean network_enabled;

        gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return gps_enabled || network_enabled;
    }

    private void stopTimer() {
        if (mWifiCheckbox != null && mWifiCheckbox.isChecked())
            mViewModel.stopWifiScanning();
        if (mBeaconCheckbox != null && mBeaconCheckbox.isChecked())
            mViewModel.stopBeaconsScanning();
        if (mMagneticVectorCheckbox != null && mMagneticVectorCheckbox.isChecked())
            mViewModel.getMv().removeObserver(mMagneticVectorObserver);
        if (mTimer != null && mTimer.isRunning()) mTimer.stop();
    }

    private void showStartDialog() {
        Activity activity = getActivity();
        assert activity != null;

        Dialog.showStartDialog(activity, dialog -> { // On Show Listener
            Button start = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
            TextView errorTextView = ((AlertDialog) dialog).findViewById(R.id.errorMessage);
            TextInputEditText secondsEditText = ((AlertDialog) dialog).findViewById(R.id.seconds);
            TextInputLayout secondsInputLayout = ((AlertDialog) dialog).findViewById(R.id.secondsInputLayout);

            mWifiCheckbox = ((AlertDialog) dialog).findViewById(R.id.wifi);
            mBeaconCheckbox = ((AlertDialog) dialog).findViewById(R.id.beacons);
            mMagneticVectorCheckbox = ((AlertDialog) dialog).findViewById(R.id.magneticVector);

            mWifiCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && activity instanceof MainActivity) {
                    ((MainActivity) activity).turnWifiOn(this);
                }
            });
            mBeaconCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && activity instanceof MainActivity) {
                    ((MainActivity) activity).turnBluetoothOn(this);
                }
            });

            start.setOnClickListener(v -> {
                // Check user data input
                if (secondsEditText != null && !TextUtils.isEmpty(secondsEditText.getText())
                        && (mWifiCheckbox.isChecked() || mBeaconCheckbox.isChecked())) {

                    seconds = Integer.parseInt(secondsEditText.getText().toString());

                    if (mCurrentFingerprint == null) { // New fingerprint
                        mCurrentFingerprint = new Fingerprint();
                    }
                    if (mCurrentFingerprint.isRunning()) { // Next Measuration on same fingerprint
                        mCurrentFingerprint.newMeasuration(mDirection.getText().toString());
                        startCountdown(seconds);
                    }else { // Sets new fingerprint information data
                        showSetupFingerprintDialog();
                    }

                    dialog.dismiss();
                } else if (errorTextView != null && secondsInputLayout != null) {
                    // No such user input
                    errorTextView.setVisibility(View.VISIBLE);
                    secondsInputLayout.setError(getString(R.string.empty_error));
                }
            });
        }, null);
    }

    /**
     * Sets information on the new fingerprint created such as x, y, borders, etc.
     */
    private void showSetupFingerprintDialog() {
        Activity activity = getActivity();
        assert activity != null;

        Dialog.showSetFingerprintDialog(getActivity(), dialog -> { // On Show Listener
            Button start = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
            AtomicBoolean noErrors = new AtomicBoolean();
            TextInputEditText bordersEditText = ((AlertDialog) dialog).findViewById(R.id.borders);

            AlertDialog alertDialog = (AlertDialog) dialog;
            List<Pair<TextInputLayout, TextInputEditText>> inputs = new ArrayList<>();
            inputs.add(new Pair<>(alertDialog.findViewById(R.id.numberInputLayout),
                    alertDialog.findViewById(R.id.number)));
            inputs.add(new Pair<>(alertDialog.findViewById(R.id.xInputLayout),
                    alertDialog.findViewById(R.id.x)));
            inputs.add(new Pair<>(alertDialog.findViewById(R.id.yInputLayout),
                    alertDialog.findViewById(R.id.y)));

            start.setOnClickListener(v -> {
                noErrors.set(true);
                for (Pair<TextInputLayout, TextInputEditText> pair : inputs) {
                    if (TextUtils.isEmpty(pair.second.getText())) {
                        pair.first.setError(getString(R.string.empty_error));
                        noErrors.set(false);
                    }
                }

                if (noErrors.get()) {
                    String number = inputs.get(0).second.getText().toString();
                    mCurrentFingerprint.setNumber(number);

                    String x = inputs.get(1).second.getText().toString();
                    mCurrentFingerprint.setX(x);

                    String y = inputs.get(2).second.getText().toString();
                    mCurrentFingerprint.setY(y);

                    String borders = "";
                    if (bordersEditText != null) {
                        borders = bordersEditText.getText().toString();
                    }
                    mCurrentFingerprint.setBorders(borders);

                    mCurrentFingerprint.startMeasuring(mDirection.getText().toString());

                    startCountdown(seconds);

                    Drawable drawable = AppCompatResources.getDrawable(
                            Objects.requireNonNull(getContext()),
                            R.drawable.ic_fast_forward_white_24dp);
                    mSpeedDialView.setMainFabOpenedDrawable(drawable);

                    dialog.dismiss();
                }
            });
        }, null);
    }

    private void startCountdown(int duration) {
        if (!mTimer.isRunning()) { // Start scanning data
            if (mWifiCheckbox != null && mWifiCheckbox.isChecked())
                mViewModel.startWifiScanning();
            if (mBeaconCheckbox != null && mBeaconCheckbox.isChecked())
                mViewModel.startBeaconsScanning();
            if (mMagneticVectorCheckbox != null && mMagneticVectorCheckbox.isChecked())
                mViewModel.getMv().observe(this, mMagneticVectorObserver);

            mTimer.startCountFrom(duration);
        }
        else if (getContext() != null) { // Error message cause timer is just running
            Toast toast = Toasty.error(getContext(),
                    getString(R.string.start_timer),
                    Toast.LENGTH_SHORT,
                    true);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    /**
     * Sets SpeedDial buttons: Stop and Save because Start button is the
     * default, catched in @{@link FingerprintingFragment#onMainActionSelected()}
     * @param addActionItems Used for savedInstanceState (in order not to create duplicate buttons).
     */
    private void setupSpeedDial(boolean addActionItems) {
        Context context = getContext();
        assert context != null;

        // Setup mini speed-dial buttons
        if (addActionItems) {
            Drawable drawable = AppCompatResources
                    .getDrawable(getContext(), R.drawable.ic_stop_white_24dp);
            mSpeedDialView.addActionItem(
                    new SpeedDialActionItem.Builder(R.id.fab_stop, drawable)
                            .setLabel(getString(R.string.dial_stop))
                            .setTheme(R.style.AppTheme_Fab)
                            .create());

            drawable = AppCompatResources
                    .getDrawable(getContext(), R.drawable.ic_save_white_24dp);
            mSpeedDialView.addActionItem(
                    new SpeedDialActionItem.Builder(R.id.fab_save, drawable)
                            .setLabel(getString(R.string.dial_save))
                            .setTheme(R.style.AppTheme_Fab)
                            .create());
        }

        // Start button listener
        mSpeedDialView.setOnChangeListener(this);
        // Save, Stop and Restart buttons listener
        mSpeedDialView.setOnActionSelectedListener(this);
    }
}
