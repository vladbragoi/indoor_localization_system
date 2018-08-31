package it.univr.vlad.fingerprinting.view;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
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

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
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
import it.univr.vlad.fingerprinting.Node;
import it.univr.vlad.fingerprinting.R;
import it.univr.vlad.fingerprinting.Timer;
import it.univr.vlad.fingerprinting.model.CBLDatabase;
import it.univr.vlad.fingerprinting.mv.MagneticVector;
import it.univr.vlad.fingerprinting.viewmodel.NodeViewModel;

public class FingerprintingFragment extends Fragment implements Timer.TimerListener,
        SpeedDialView.OnActionSelectedListener, SpeedDialView.OnChangeListener {

    private final static int LOCATION_RESULT_CODE = 4295;

    private SpeedDialView mSpeedDialView;
    private TextView mDirection;
    private TextView mTimerSeconds;
    private TextView mTimerMinutes;
    private NodeListAdapter mAdapter;
    private AppCompatCheckBox mWifiCheckbox;
    private AppCompatCheckBox mBeaconCheckbox;
    private AppCompatCheckBox mMagneticVectorCheckbox;

    private Timer mTimer;
    private int seconds = 0;

    private CBLDatabase mDatabase;
    private NodeViewModel mViewModel;

    private Fingerprint mCurrentFingerprint;

    private final Observer<List<Node>> mWifiNodesObserver = nodes -> {
        mCurrentFingerprint.addWifiNodes(nodes);
        mAdapter.addWifiNodes(nodes);
    };

    private final Observer<List<Node>> mBeaconNodesObserver = nodes -> {
        mCurrentFingerprint.addBeaconNodes(nodes);
        mAdapter.addBeaconNodes(nodes);
    };

    private final Observer<MagneticVector> mMagneticVectorObserver = mv -> {
        mCurrentFingerprint.addMagneticVector(mv);
        mAdapter.setMv(mv);
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(NodeViewModel.class);
        mAdapter = new NodeListAdapter();
        mTimer = new Timer();

        mTimer.setTimerListener(this);
        mViewModel.getMv().observe(this, magneticVector ->
                mDirection.setText(magneticVector != null ? magneticVector.toString() : "NORTH"));

        Context context = getContext();
        if (context != null && savedInstanceState == null) {
            turnLocationOn(context);
        }

        if (getActivity() != null) {
            Application application = (Application) getActivity().getApplication();
            mDatabase = application.getDatabase();
        }
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

    private void setupSpeedDial(boolean addActionItems) {
        Context context = getContext();
        assert context != null;

        // Setup mini speed-dial buttons
        if (addActionItems) {
            Drawable drawable = AppCompatResources
                    .getDrawable(getContext(), R.drawable.ic_replay_white_24dp);
            mSpeedDialView.addActionItem(
                    new SpeedDialActionItem.Builder(R.id.fab_replay, drawable)
                            .setLabel(getString(R.string.dial_restart))
                            .setTheme(R.style.AppTheme_Fab)
                            .create());

            drawable = AppCompatResources
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

    @Override public void onResume() {
        super.onResume();
        mViewModel.getWifiList().observe(this, mWifiNodesObserver);
        mViewModel.getBeaconList().observe(this, mBeaconNodesObserver);
    }

    @Override public void onStop() {
        stopTimer();
        mViewModel.getWifiList().removeObserver(mWifiNodesObserver);
        mViewModel.getBeaconList().removeObserver(mBeaconNodesObserver);
        mViewModel.getMv().removeObserver(mMagneticVectorObserver);
        super.onStop();
    }

    @Override public void onDestroy() {
        mTimer.destroy();
        disableBluetooth();
        super.onDestroy();
    }

    public boolean closeSpeedDial() {
        //Closes menu if its opened.
        if (mSpeedDialView.isOpen()) {
            mSpeedDialView.close();
            return true;
        }
        return false;
    }

    private void turnLocationOn(@NotNull Context context) {
        // ENABLE LOCATION WITH GOOGLE-API
        LocationRequest locationRequest= new LocationRequest();

        // Low-Precision Location (low power)
        locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(context);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        // Request to enable location
        task.addOnFailureListener((Activity) context, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(getActivity(),
                            LOCATION_RESULT_CODE);
                } catch (IntentSender.SendIntentException e1) {
                    e1.printStackTrace();
                }
            }
        });

        /* ENABLE LOCATION THROUGH SYSTEM SETTINGS
        LocationManager locationManager = (LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;

        if (!isLocationEnabled(locationManager)) {
            final AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setTitle(context.getString(R.string.location_title));
            dialog.setMessage(context.getString(R.string.location_message));
            dialog.setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                *//*Activity activity = getActivity();
                if (activity != null)
                    activity.startActivityForResult(
                            new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                            LOCATION_RESULT_CODE);*//*
            });
            dialog.setNegativeButton(android.R.string.no, (dialog2, which) -> {
                    dialog2.dismiss();
                    locationNotEnabled();
            });
            dialog.show();
        }*/
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOCATION_RESULT_CODE) {
            if (resultCode != Activity.RESULT_OK) locationNotEnabled();
        } else
            super.onActivityResult(requestCode, resultCode, data);
    }

    private void locationNotEnabled() {
        assert getContext() != null;
        Toasty.warning(getContext(),
                getString(R.string.location_not_enabled),
                Toast.LENGTH_LONG,
                true).show();
        mSpeedDialView.hide();
    }

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

    private void turnBluetoothOn(Context context) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toasty.warning(context,
                        getString(R.string.ble_not_supported),
                        Toast.LENGTH_SHORT, true).show();
            }
            else {
                /*
                 * Should use this method, but just to uniform the requests
                 * mContext.startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                 */
                final AlertDialog.Builder dialog =
                        new AlertDialog.Builder(context);
                dialog.setTitle(context.getString(R.string.bluetooth_title));
                dialog.setMessage(context.getString(R.string.bluetooth_message));
                dialog.setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                    if (!bluetoothAdapter.isEnabled()) {
                        bluetoothAdapter.enable();
                        Toasty.success(context,
                                context.getString(R.string.bluetooth_enabled),
                                Toast.LENGTH_SHORT, true).show();
                    }
                });
                dialog.setNegativeButton(android.R.string.no, (dialog2, which) -> {
                    if (mBeaconCheckbox != null) mBeaconCheckbox.setChecked(false);
                    dialog2.dismiss();
                });
                dialog.setOnCancelListener(diag ->
                        context.startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                );
                dialog.show();
            }
        }
    }

    private void turnWifiOn(@NotNull Context context) {
        WifiManager wifiManager = (WifiManager) context
                .getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        if (wifiManager != null && !wifiManager.isWifiEnabled()) {
            final AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setTitle(context.getString(R.string.wifi_title));
            dialog.setMessage(context.getString(R.string.wifi_message));
            dialog.setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                wifiManager.setWifiEnabled(true);
                Toasty.success(context,
                        context.getString(R.string.wifi_enabled),
                        Toast.LENGTH_SHORT, true).show();
            });
            dialog.setNegativeButton(android.R.string.no, (dialog2, which) -> {
                if (mWifiCheckbox != null) mWifiCheckbox.setChecked(false);
                dialog2.dismiss();
            });
            dialog.setOnCancelListener(diag ->
                    context.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS))
            );
            dialog.show();
        }
    }

    private void disableBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled())
            bluetoothAdapter.disable();
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

    @Override
    public void onTimerStopped(Timer.TimerStatus status) {
        if (status.equals(Timer.TimerStatus.STOPPED)) stopTimer();
    }

    @Override
    public boolean onActionSelected(SpeedDialActionItem actionItem) {
        switch (actionItem.getId()) {
            case R.id.fab_save:
                // TODO: SAVE TO DB
                if (mCurrentFingerprint != null) {
                    mCurrentFingerprint.saveInto(mDatabase.unwrapDatabase());
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
    public void onToggleChanged(boolean isOpen) {

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
                if (isChecked) turnWifiOn(activity);
            });
            mBeaconCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) turnBluetoothOn(activity);
            });

            start.setOnClickListener(v -> {
                // Check user data input
                if (secondsEditText != null && !TextUtils.isEmpty(secondsEditText.getText())
                        && (mWifiCheckbox.isChecked() || mBeaconCheckbox.isChecked())) {

                    seconds = Integer.parseInt(secondsEditText.getText().toString());

                    if (mCurrentFingerprint == null) {
                        mCurrentFingerprint = new Fingerprint();
                    }

                    if (mCurrentFingerprint.isRunning()) {
                        mCurrentFingerprint.newMeasuration(mDirection.getText().toString());
                        startCountdown(seconds);
                    }else {
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
        else if (getContext() != null) {
            Toast toast = Toasty.error(getContext(),
                    getString(R.string.start_timer),
                    Toast.LENGTH_SHORT,
                    true);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }
}
