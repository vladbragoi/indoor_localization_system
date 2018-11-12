package it.univr.vlad.fingerprinting.devices.ble;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.widget.Toast;

import java.util.List;

import it.univr.vlad.fingerprinting.templates.Manager;
import it.univr.vlad.fingerprinting.templates.Node;
import it.univr.vlad.fingerprinting.templates.NodeType;
import it.univr.vlad.fingerprinting.templates.Observer;
import it.univr.vlad.fingerprinting.R;

public class BleManager extends Manager {

    private BeaconScanner mBeaconScanner;
    private BluetoothAdapter mBluetoothAdapter;
    private Context mContext;

    public BleManager(Context context){
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBeaconScanner = new BeaconScanner(context);
        mBeaconScanner.setBeaconListerner(this::notifyObservers);
    }

    @Override
    public void start() {
        mBeaconScanner.start();
    }

    @Override
    public void stop() {
        mBeaconScanner.stop();
    }

    @Override
    public void bind() {
        mBeaconScanner.bind();
    }

    @Override
    public void unbind() {
        mBeaconScanner.stop();
        mBeaconScanner.unbind();
    }

    /**
     * Notifies observers with the updated values.
     * @param results the list of beacons received by BeaconScanner
     */
    @Override
    public void notifyObservers(List<Node> results) {
        for (Observer observer : super.mObservers) {
            observer.update(NodeType.BLE, results);
        }
    }

    @Override
    public void notifyObservers(float[] geomagneticField, float azimut) {
    }

    /**
     * Check whether if bluetooth is enabled.
     * @return true if enabled, false otheriwise
     */
    public boolean isDeviceEnabled() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    /**
     * Show a dialog asking for bluetooth to be enabled.
     */
    public void enableDevice() {
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(mContext,
                    R.string.ble_not_supported,
                    Toast.LENGTH_SHORT).show();
        }
        else {
            /*
             * Should use this method, but just to uniform the requests
             * mContext.startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
             */
            final AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
            dialog.setTitle(mContext.getString(R.string.bluetooth_title));
            dialog.setMessage(mContext.getString(R.string.bluetooth_message));
            dialog.setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                mBluetoothAdapter.enable();
                Toast.makeText(mContext,
                        mContext.getString(R.string.bluetooth_enabled),
                        Toast.LENGTH_SHORT).show();
            });
            dialog.setNegativeButton(android.R.string.cancel, (dialog2, which) -> dialog2.dismiss());
            dialog.setOnCancelListener(diag -> mContext
                    .startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            );
            dialog.show();
        }
    }
}