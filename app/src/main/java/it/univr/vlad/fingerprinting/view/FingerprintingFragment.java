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
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;

import org.jetbrains.annotations.NotNull;

import it.univr.vlad.fingerprinting.R;
import it.univr.vlad.fingerprinting.exceptions.DeviceUnknownException;
import it.univr.vlad.fingerprinting.mv.MagneticVector;
import it.univr.vlad.fingerprinting.viewmodel.NodeViewModel;

public class FingerprintingFragment extends Fragment {

    private final String[] devices = new String[]{"wifi", "beacons"};
    private boolean[] devicesChecked = new boolean[]{false, false};

    private SpeedDialView mSpeedDialView;
    private TextView mDirection;

    private RecyclerView mRecyclerView;
    private NodeListAdapter mAdapter;
    private NodeViewModel mViewModel;
    private AppCompatCheckBox wifiCheckbox;
    private AppCompatCheckBox beaconCheckbox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(NodeViewModel.class);
        mAdapter = new NodeListAdapter(getContext());

        mViewModel.getWifiList()
                .observe(this, wifiNodes -> mAdapter.setWifiNodes(wifiNodes));

        mViewModel.getMv().observe(this, magneticVector ->
                mDirection.setText(magneticVector != null ? magneticVector.toString() : "NORTH"));

        /*mViewModel.getBeaconList()
                .observe(this, beaconNodes -> mAdapter.setBeaconNodes(beaconNodes));*/

        mViewModel.getMv().observe(this, mv -> mAdapter.setMv(mv));

        Context context = getContext();
        if (context != null && savedInstanceState == null) {
            turnWifiOn(context);
            turnBluetoothOn(context);
            turnLocationOn(context);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fingerprinting, container, false);
        mSpeedDialView = rootView.findViewById(R.id.start_stop_button);
        mDirection = rootView.findViewById(R.id.directionValue);
        mRecyclerView = rootView.findViewById(R.id.nodesRecyclerView);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setItemAnimator(null);
        mRecyclerView.setAdapter(mAdapter);
        setupSpeedDial(savedInstanceState == null, container);
        return rootView;
    }

    /*@Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getUserVisibleHint()) {
            mSpeedDialView = view.findViewById(R.id.start_stop_button);
            setupSpeedDial(savedInstanceState == null);
            mDirection = view.findViewById(R.id.directionValue);
        }
        else {
            Log.e(getClass().getName(),"User visibility = " + getUserVisibleHint());
        }
    }*/

    private void setupSpeedDial(boolean addActionItems, ViewGroup viewGroup) {
        if (addActionItems && getContext() != null) {
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
                final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Dispositivi");
                View view = getLayoutInflater().inflate(R.layout.dialog_start_view, viewGroup);
                builder.setView(view);
                wifiCheckbox = view.findViewById(R.id.wifi);
                wifiCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) turnWifiOn(getContext());
                });
                beaconCheckbox = view.findViewById(R.id.beacons);
                beaconCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) turnBluetoothOn(getContext());
                });

                builder.setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    if (id == DialogInterface.BUTTON_POSITIVE) {
                        // TODO: START COUNTDOWN
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.show();
                return false; // True to keep the Speed Dial open
            }

            @Override
            public void onToggleChanged(boolean isOpen) {}
        });

        mSpeedDialView.setOnActionSelectedListener(actionItem -> {
            Toast.makeText(getContext(), "DialAction", Toast.LENGTH_SHORT).show();
            return false; // True to keep the Speed Dial open
        });

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
