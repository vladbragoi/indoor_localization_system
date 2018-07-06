package it.univr.vlad.fingerprinting.ble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.List;

import it.univr.vlad.fingerprinting.Node;


public class BeaconScanner extends AppCompatActivity implements BeaconConsumer {

    private BeaconListener mListener;
    private Context mContext;

    private BeaconManager mBeaconManager;
    private List<Node> updatingList;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;


    public BeaconScanner(Context context){

        updatingList = new ArrayList<>();
        mContext = context;

        mBeaconManager= BeaconManager.getInstanceForApplication(context);

        //protocollo iBeacon
        mBeaconManager.getBeaconParsers().add(new BeaconParser("iBeacon").
                setBeaconLayout("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24"));
        //protocollo Eddystone
        mBeaconManager.getBeaconParsers().add(new BeaconParser("Eddystone").
                setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
    }


    /**
     * when a list of beacons is received, it adds all beacons to updatingList, depending if the beacon is
     * iBeacon or Eddystone, major and minor fields are filled or null. In the end, mListener will notify
     * changes.
     */
    @Override
    public void onBeaconServiceConnect() {
        mBeaconManager.addRangeNotifier((beacons, region) -> {
            if (beacons.size() <= 0) return;

            for (Beacon b: beacons) {
                if (b.getParserIdentifier().equals("iBeacon"))
                    updatingList.add(new BleNode(b.getBluetoothAddress(),
                            b.getRssi(),
                            b.getId1().toString(),
                            b.getId2().toString(),
                            b.getId3().toString()));

                else if (b.getParserIdentifier().equals("Eddystone"))
                    updatingList.add(new BleNode(b.getBluetoothAddress(),
                            b.getRssi(),
                            b.getId1().toString(),
                            null,
                            null));

                mListener.onResultsChanged(updatingList);
            }
        });
    }

    @Override
    public Context getApplicationContext() {
        return mContext;
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return false;
    }

    private void checkPermissions(){
        //coarse location
        if (ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity)mContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions((Activity)mContext,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }

        //bluetooth
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter==null) {
            Toast.makeText(getApplicationContext(), "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show();
            finish();
        }
        //handled by onActivityResult() of mContext (MainActivity.java)
        else if(!mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((Activity)mContext).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    protected void bind() {
        try {
            checkPermissions();
            mBeaconManager.bind(this);
            mBeaconManager.startRangingBeaconsInRegion(
                    new Region("myRangingUniqueId",
                            null,
                            null,
                            null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    protected void unbind() {
        try {
            mBeaconManager.unbind(this);
            mBeaconManager.stopRangingBeaconsInRegion(
                    new Region("myRangingUniqueId",
                            null,
                            null,
                            null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setBeaconListerner(BeaconListener listerner) {
        mListener = listerner;
    }

    public interface BeaconListener {
        void onResultsChanged(List<Node> mResults);
    }
}