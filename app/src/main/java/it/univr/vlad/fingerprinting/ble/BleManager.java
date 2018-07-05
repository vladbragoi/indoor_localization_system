package it.univr.vlad.fingerprinting.ble;

import android.content.Context;

import java.util.List;

import it.univr.vlad.fingerprinting.Manager;
import it.univr.vlad.fingerprinting.Node;
import it.univr.vlad.fingerprinting.Observer;

public class BleManager extends Manager {

    private BeaconScanner mBeaconScanner;

    public BleManager(Context context){
        mBeaconScanner = new BeaconScanner(context);
        mBeaconScanner.setBeaconListerner(this::notifyObservers);
    }

    @Override
    public void bind() {
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
}