package it.univr.vlad.fingerprinting.view;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import it.univr.vlad.fingerprinting.Node;
import it.univr.vlad.fingerprinting.R;
import it.univr.vlad.fingerprinting.exceptions.DeviceUnknownException;
import it.univr.vlad.fingerprinting.mv.MagneticVector;
import it.univr.vlad.fingerprinting.viewmodel.NodeViewModel;
import it.univr.vlad.fingerprinting.wifi.WifiNode;

public class FingerprintingFragment extends Fragment {

    private SpeedDialView mSpeedDialView;
    private TextView mDirection;
    private NodeListAdapter mAdapter;
    private AppCompatCheckBox wifiCheckbox;
    private AppCompatCheckBox beaconCheckbox;

    private NodeViewModel mViewModel;
    private final Observer<MagneticVector> magneticVectorObserver =
            magneticVector -> mAdapter.setMv(magneticVector);

    private Handler mHandler;
    private Runnable mRunnable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(NodeViewModel.class);
        mAdapter = new NodeListAdapter(getContext());

        mViewModel.getMv().observe(this, magneticVector ->
                mDirection.setText(magneticVector != null ? magneticVector.toString() : "NORTH"));

        mViewModel.getWifiList()
                .observe(this, wifiNodes -> mAdapter.setWifiNodes(wifiNodes));

        mViewModel.getBeaconList()
                .observe(this, beaconNodes -> mAdapter.setBeaconNodes(beaconNodes));

        /*mViewModel.getMv().observe(this, magneticVectorObserver);*/

        Context context = getContext();
        if (context != null && savedInstanceState == null) {
            mHandler = new Handler();
            turnLocationOn(context);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fingerprinting, container, false);
        mSpeedDialView = rootView.findViewById(R.id.start_stop_button);
        mDirection = rootView.findViewById(R.id.directionValue);

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

        mSpeedDialView.setOnChangeListener(new SpeedDialView.OnChangeListener() {
            @Override
            public boolean onMainActionSelected() {
                showStartDialog(context, viewGroup);
                return false; // True to keep the Speed Dial open
            }

            @Override
            public void onToggleChanged(boolean isOpen) {}
        });

        mSpeedDialView.setOnActionSelectedListener(actionItem -> {
            switch (actionItem.getId()) {
                case R.id.fab_save:
                    // TODO: SAVE TO DB
                    break;
                case R.id.fab_stop:
                    mViewModel.stopWifiScanning();
                    mViewModel.stopBeaconsScanning();
                    mViewModel.getMv().removeObserver(magneticVectorObserver);
                    break;
            }
            return false; // True to keep the Speed Dial open
        });

    }

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
        mViewModel.getMv().observe(this, magneticVectorObserver);
        if (wifiCheckbox.isChecked()) mViewModel.startWifiScanning();
        if (beaconCheckbox.isChecked()) mViewModel.startBeaconsScanning();

        // TODO: TIMER

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
        LocationManager locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        if (!isLocationEnabled(locationManager)) {
            final AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setTitle(context.getString(R.string.location_title));
            dialog.setMessage(context.getString(R.string.location_message));
            dialog.setPositiveButton(android.R.string.ok, (dialog1, which) -> context
                    .startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
            dialog.setNegativeButton(android.R.string.no, (dialog2, which) -> dialog2.dismiss());
            dialog.show();
        }
    }

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
                dialog.setOnCancelListener(diag -> context
                        .startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
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
            dialog.setOnCancelListener(diag -> context
                    .startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS))
            );
            dialog.show();
        }
    }
}
