package it.univr.vlad.fingerprinting.ble;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.widget.Toast;

import java.util.List;

import it.univr.vlad.fingerprinting.Manager;
import it.univr.vlad.fingerprinting.Node;
import it.univr.vlad.fingerprinting.Observer;
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

    /**
     * @brief binds the scanner if bluetooth is enabled
     * If bluetooth is enabled calls bind() method of BeaconScanner's instance, otherwise calls enableDevice()
     */
    @Override
    public void bind() {
        if (!isDeviceEnabled()) enableDevice();
        mBeaconScanner.bind();
    }

    @Override
    public void unbind() {
        mBeaconScanner.unbind();
    }

    /**
     * @brief for each registered observer notify the update
     * Calls the update() method of each observer, passing 1 that identify the type beacon,
     * and results
     * @param results the list of beacons received by BeaconScanner
     */
    @Override
    public void notifyObservers(List<Node> results) {
        for (Observer observer : super.mObservers) {
            observer.update(1, results);
        }
    }

    @Override
    public void notifyObservers() {
    }

    /**
     * @brief return whether bluetooth is enabled or not
     * @return true if enabled, false otheriwise
     */
    @Override
    public boolean isDeviceEnabled() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    /**
     * @brief Show a dialog asking for bluetooth to be enabled
     */
    @Override
    public void enableDevice() {
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
        dialog.setNegativeButton(android.R.string.no, (dialog2, which) -> dialog2.dismiss());
        dialog.setOnCancelListener(diag -> mContext
                .startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        );
        dialog.show();
    }
}