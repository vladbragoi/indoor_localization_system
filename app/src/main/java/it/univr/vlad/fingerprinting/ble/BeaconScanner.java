package it.univr.vlad.fingerprinting.ble;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.RemoteException;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.List;

import it.univr.vlad.fingerprinting.Node;

public class BeaconScanner implements BeaconConsumer {

    private BeaconListener mListener;
    private Context mContext;

    private BeaconManager mBeaconManager;
    private List<Node> updatingList;

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
     * set a range notifier: When a list of beacons is received, it parses each beacon and create a new BleNode
     * with the needed parameters and finally adds it to updatingList. Then it notify the BleManager
     * listener
     * Then onBeaconServiceConnect starts ranging beacons in region
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

                mListener.onResultsChanged(updatingList);
            }
        });
    }

    @Override
    public Context getApplicationContext() {
        return mContext;
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {}

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return false;
    }

    /**
     * bind the BleManager with the scanner
     */
    protected void bind() {
        mBeaconManager.bind(this);
    }

    /**
     * unbind the BleManager with the scanner and stop ranging beacons in region
     */
    protected void unbind() {
        mBeaconManager.unbind(this);
        /*try {
            mBeaconManager.unbind(this);
            mBeaconManager.stopRangingBeaconsInRegion(
                    new Region("myRangingUniqueId",
                            null,
                            null,
                            null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }*/
    }

    public void start() {
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

    public void stop() {
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

    public void setBeaconListerner(BeaconListener listerner) {
        mListener = listerner;
    }

    public interface BeaconListener {
        void onResultsChanged(List<Node> mResults);
    }
}