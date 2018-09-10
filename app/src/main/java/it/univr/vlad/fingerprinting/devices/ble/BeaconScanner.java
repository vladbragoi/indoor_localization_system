package it.univr.vlad.fingerprinting.devices.ble;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import es.dmoral.toasty.Toasty;
import it.univr.vlad.fingerprinting.templates.Node;
import it.univr.vlad.fingerprinting.R;

public class BeaconScanner implements BeaconConsumer {

    private BeaconListener mListener;
    private Context mContext;

    private BeaconManager mBeaconManager;
    private List<Node> updatingList;

    BeaconScanner(Context context){
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
     * Sets a range notifier: when a list of beacons is received, it parses each
     * beacon and create a new BleNode with the needed parameters and finally adds
     * it to the updatingList, then it notify the listener.
     * Then onBeaconServiceConnect starts ranging beacons in region.
     */
    @Override
    public void onBeaconServiceConnect() {
        mBeaconManager.addRangeNotifier((beacons, region) -> {
            if (beacons.size() <= 0) return;

            updatingList.clear();
            for (Beacon b: beacons) {
                if (b.getParserIdentifier().equals("iBeacon"))
                    updatingList.add(new IBeaconNode(b.getBluetoothAddress(),
                            b.getRssi(),
                            b.getId1().toString(),
                            b.getId2().toString(),
                            b.getId3().toString()));

                else if (b.getParserIdentifier().equals("Eddystone"))
                    updatingList.add(new EddystoneNode(b.getBluetoothAddress(),
                            b.getRssi(),
                            b.getId1().toString()));

                Collections.sort(updatingList, (o1, o2) -> o2.getId().compareTo(o1.getId()));

                if (updatingList != null && !updatingList.isEmpty())
                    mListener.onResultsChanged(updatingList);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Context getApplicationContext() {
        return mContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unbindService(ServiceConnection serviceConnection) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return false;
    }

    /**
     * Binds the BleManager with the scanner.
     */
    protected void bind() {
        if (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            mBeaconManager.bind(this);
        } else {
            Toasty.warning(mContext,
                    mContext.getString(R.string.ble_not_supported),
                    Toast.LENGTH_SHORT,
                    true).show();
        }
    }

    /**
     * Unbinds the BleManager with the scanner.
     */
    protected void unbind() {
        if (mBeaconManager.isBound(this)) mBeaconManager.unbind(this);
    }

    /**
     * Starts ranging beacons in region.
     */
    public void start() {
        if (mBeaconManager.isBound(this)) {
            try {
                mBeaconManager.startRangingBeaconsInRegion(
                        new Region("myRangingUniqueId",
                                null,
                                null,
                                null));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stops ranging beacons in region.
     */
    public void stop() {
        if (mBeaconManager.isBound(this)) {
            try {
                mBeaconManager.stopRangingBeaconsInRegion(
                        new Region("myRangingUniqueId",
                                null,
                                null,
                                null));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void setBeaconListerner(BeaconListener listerner) {
        mListener = listerner;
    }

    public interface BeaconListener {
        void onResultsChanged(List<Node> mResults);
    }
}