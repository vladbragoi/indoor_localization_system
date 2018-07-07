package it.univr.vlad.fingerprinting.ble;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
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

    @Override
    public void bind() {
        if (!isDeviceEnabled()) enableDevice();
        mBeaconScanner.bind();
    }

    @Override
    public void unbind() {
        mBeaconScanner.unbind();
    }

    @Override
    public void notifyObservers(List<Node> results) {
        for (Observer observer : super.mObservers) {
            observer.update(1, results);
        }
    }

    @Override
    public void notifyObservers() {
    }

    @Override
    public boolean isDeviceEnabled() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

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