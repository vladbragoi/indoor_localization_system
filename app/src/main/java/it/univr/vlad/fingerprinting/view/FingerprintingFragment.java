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
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

import it.univr.vlad.fingerprinting.MainActivity;
import it.univr.vlad.fingerprinting.R;
import it.univr.vlad.fingerprinting.Timer;
import it.univr.vlad.fingerprinting.mv.MagneticVector;
import it.univr.vlad.fingerprinting.viewmodel.NodeViewModel;

public class FingerprintingFragment extends Fragment implements Timer.TimerListener{

    private final static int LOCATION_RESULT_CODE = 4295;

    private SpeedDialView mSpeedDialView;
    private TextView mDirection;
    private TextView mSeconds;
    private TextView mMinutes;
    private NodeListAdapter mAdapter;
    private AppCompatCheckBox wifiCheckbox;
    private AppCompatCheckBox beaconCheckbox;

    private NodeViewModel mViewModel;
    private Timer mTimer;

    private final Observer<MagneticVector> magneticVectorObserver =
            magneticVector -> mAdapter.setMv(magneticVector);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(NodeViewModel.class);
        mAdapter = new NodeListAdapter(getContext());
        mTimer = new Timer();
        mTimer.setTimerListener(this);

        // Prepare observing data
        mViewModel.getMv().observe(this, magneticVector ->
                mDirection.setText(magneticVector != null ? magneticVector.toString() : "NORTH"));
        mViewModel.getWifiList()
                .observe(this, wifiNodes -> mAdapter.setWifiNodes(wifiNodes));
        mViewModel.getBeaconList()
                .observe(this, beaconNodes -> mAdapter.setBeaconNodes(beaconNodes));

        Context context = getContext();
        if (context != null && savedInstanceState == null) {
            turnLocationOn(context);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fingerprinting, container, false);
        mSpeedDialView = rootView.findViewById(R.id.start_stop_button);
        mDirection = rootView.findViewById(R.id.directionValue);
        mSeconds = rootView.findViewById(R.id.secondsValue);
        mMinutes = rootView.findViewById(R.id.minutesValue);

        RecyclerView mRecyclerView = rootView.findViewById(R.id.nodesRecyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setItemAnimator(null);
        mRecyclerView.setAdapter(mAdapter);

        setupSpeedDial(savedInstanceState == null, container);
        return rootView;
    }

    private void setupSpeedDial(boolean addActionItems, ViewGroup viewGroup) {
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
        mSpeedDialView.setOnChangeListener(new SpeedDialView.OnChangeListener() {
            @Override
            public boolean onMainActionSelected() {
                showStartDialog(context, viewGroup);
                return false; // True to keep the Speed Dial open
            }

            @Override
            public void onToggleChanged(boolean isOpen) {}
        });

        // Save, Stop and Restart buttons listener
        mSpeedDialView.setOnActionSelectedListener(actionItem -> {
            switch (actionItem.getId()) {
                case R.id.fab_save:

                    // TODO: SAVE TO DB

                    break;
                case R.id.fab_stop:
                    stop();
                    break;
            }
            return false; // True to keep the Speed Dial open
        });

    }

    /**
     * A dialog to ask user which device(s) should be used for "fingerprinting"
     * and how many seconds to scan values
     * @param context context
     * @param viewGroup root viewGroup to proper inflate dialog layout
     */
    private void showStartDialog(Context context, ViewGroup viewGroup) {
        View view = getLayoutInflater().inflate(R.layout.dialog_start_view, viewGroup);
        TextView errorTextView = view.findViewById(R.id.errorMessage);
        TextInputEditText secondsEditText = view.findViewById(R.id.seconds);
        TextInputLayout secondsInputLayout = view.findViewById(R.id.secondsInputLayout);
        wifiCheckbox = view.findViewById(R.id.wifi);
        beaconCheckbox = view.findViewById(R.id.beacons);

        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(getString(R.string.devices))
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        wifiCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) turnWifiOn(context);
        });
        beaconCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) turnBluetoothOn(context);
        });

        // Check for correct input
        dialog.setOnShowListener(dialogInterface -> {
            Button start = ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE);

            start.setOnClickListener(v -> {
                if(secondsEditText.getText() != null
                        && !secondsEditText.getText().toString().equals("")
                        && (wifiCheckbox.isChecked() || beaconCheckbox.isChecked())) {

                    startCountdown(Integer.valueOf(secondsEditText.getText().toString()));
                    dialog.dismiss();
                } else { // No such user input
                    errorTextView.setVisibility(View.VISIBLE);
                    secondsInputLayout.setError(getString(R.string.seconds_error));
                }
            });
        });
        dialog.show();
    }

    private void startCountdown(int duration) {
        if (mTimer.isRunning()) {
            Toast toast = Toast.makeText(getContext(), getString(R.string.start_timer), Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
        else { // Start scanning data
            mViewModel.getMv().observe(this, magneticVectorObserver);
            if (wifiCheckbox.isChecked()) mViewModel.startWifiScanning();
            if (beaconCheckbox.isChecked()) mViewModel.startBeaconsScanning();

            mTimer.startCountFrom(duration);
        }
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
        Toast.makeText(getContext(),
                getString(R.string.location_not_enabled),
                Toast.LENGTH_SHORT).show();
        mSpeedDialView.hide();
    }

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
                Toast.makeText(context,
                        R.string.ble_not_supported,
                        Toast.LENGTH_SHORT).show();
            }
            else {
                /*
                 * Should use this method, but just to uniform the requests
                 * mContext.startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                 */
                final android.app.AlertDialog.Builder dialog =
                        new android.app.AlertDialog.Builder(context);
                dialog.setTitle(context.getString(R.string.bluetooth_title));
                dialog.setMessage(context.getString(R.string.bluetooth_message));
                dialog.setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                    bluetoothAdapter.enable();
                    Toast.makeText(context,
                            context.getString(R.string.bluetooth_enabled),
                            Toast.LENGTH_SHORT).show();
                });
                dialog.setNegativeButton(android.R.string.no, (dialog2, which) -> {
                    if (beaconCheckbox != null) beaconCheckbox.setChecked(false);
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
                Toast.makeText(context,
                        context.getString(R.string.wifi_enabled),
                        Toast.LENGTH_SHORT).show();
            });
            dialog.setNegativeButton(android.R.string.no, (dialog2, which) -> {
                if (wifiCheckbox != null) wifiCheckbox.setChecked(false);
                dialog2.dismiss();
            });
            dialog.setOnCancelListener(diag ->
                    context.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS))
            );
            dialog.show();
        }
    }

    private void stop() {
        if (wifiCheckbox.isChecked()) mViewModel.stopWifiScanning();
        if (beaconCheckbox.isChecked()) mViewModel.stopBeaconsScanning();
        mViewModel.getMv().removeObserver(magneticVectorObserver);
        if (mTimer.isRunning()) mTimer.stop();
    }

    @Override public void onStop() {
        stop();
        super.onStop();
    }

    @Override public void onDestroy() {
        mTimer.destroy();
        super.onDestroy();
    }

    @Override
    public void onSecondsChanged(String seconds) {
        mSeconds.setText(seconds);
    }

    @Override
    public void onMinutesChanged(String minutes) {
        mMinutes.setText(minutes);
    }

    @Override
    public void onTimerStopped(Timer.TimerStatus status) {
        if (status.equals(Timer.TimerStatus.STOPPED)) stop();
    }
}
