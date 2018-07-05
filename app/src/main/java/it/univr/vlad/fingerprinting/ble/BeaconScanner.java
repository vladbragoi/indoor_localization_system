package it.univr.vlad.fingerprinting.ble;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.RemoteException;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

import it.univr.vlad.fingerprinting.Node;

public class BeaconScanner implements BeaconConsumer {

    protected BeaconManager mBeaconManager;

    public List<Node> getUpdatingList() {
        return updatingList;
    }

    private List<Node> updatingList = new ArrayList<>();
    BleManager mBleManager;

    public BeaconScanner(BleManager manager, Context appContext){
        mBeaconManager= BeaconManager.getInstanceForApplication(appContext);
        mBleManager = manager;

        //protocollo iBeacon
        mBeaconManager.getBeaconParsers().add(new BeaconParser("iBeacon").
                setBeaconLayout("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24"));

        //protocollo Eddystone
        mBeaconManager.getBeaconParsers().add(new BeaconParser("Eddystone").
                setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
    }

    @Override
    public void onBeaconServiceConnect() {
        mBeaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, org.altbeacon.beacon.Region region) {

                if (beacons.size() > 0) {

                    for (Beacon b : beacons) {
                        if(b.getParserIdentifier().equals("iBeacon"))
                            updatingList.add(new BleNode(b.getBluetoothAddress(), b.getRssi(), b.getId1().toString(),
                                    b.getId2().toString(), b.getId3().toString()));

                        else if(b.getParserIdentifier().equals("Eddystone"))
                            updatingList.add(new BleNode(b.getBluetoothAddress(), b.getRssi(), b.getId1().toString(),
                                   null, null));

                        mBleManager.notifyObservers();
                    }
                }
            }
        });
    }


    @Override
    public Context getApplicationContext() {
        return null;
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {

    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return false;
    }


    protected void bind() {
        try {
            mBeaconManager.bind(this);
            mBeaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    protected void unBind() {
        try {
            mBeaconManager.unbind(this);
            mBeaconManager.stopRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
