package it.univr.vlad.fingerprinting.viewmodel;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import java.util.List;

import it.univr.vlad.fingerprinting.Node;
import it.univr.vlad.fingerprinting.NodeType;
import it.univr.vlad.fingerprinting.Observer;
import it.univr.vlad.fingerprinting.ble.BleManager;
import it.univr.vlad.fingerprinting.mv.MagneticVector;

public class BeaconListLiveData extends LiveData<List<Node>> implements Observer{

    private BleManager mBleManager;

    BeaconListLiveData(Context context) {
         mBleManager = new BleManager(context);
         mBleManager.registerObserver(this);
    }

    @Override
    protected void onActive() {
        super.onActive();
        mBleManager.bind();
    }

    @Override
    protected void onInactive() {
        mBleManager.unbind();
        super.onInactive();
    }

    @Override public void startScanning() {
        mBleManager.start();
    }

    @Override public void stopScanning() {
        mBleManager.stop();
    }

    @Override
    public void update(NodeType type, List<Node> results) {
        if (type == NodeType.BEACON) postValue(results);
    }

    @Override
    public void update(MagneticVector mv) {}
}
