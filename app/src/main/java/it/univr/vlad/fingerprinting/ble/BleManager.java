package it.univr.vlad.fingerprinting.ble;

import android.content.Context;

import it.univr.vlad.fingerprinting.Manager;
import it.univr.vlad.fingerprinting.Observer;

public class BleManager extends Manager {

    private BeaconScanner mBeaconScanner;

    public BleManager(Context appContext){
        mBeaconScanner = new BeaconScanner(this, appContext);

    }

    @Override
    public void notifyObservers() {
        for (Observer o: mObservers) {
            o.update(mBeaconScanner.getUpdatingList());
        }
    }

    @Override
    public void bind() {

        mBeaconScanner.bind();

    }

    @Override
    protected void unBind() {
        mBeaconScanner.unBind();
    }


}
