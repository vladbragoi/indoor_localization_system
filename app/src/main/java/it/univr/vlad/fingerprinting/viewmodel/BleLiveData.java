package it.univr.vlad.fingerprinting.viewmodel;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import java.util.List;

import it.univr.vlad.fingerprinting.templates.Node;
import it.univr.vlad.fingerprinting.templates.NodeType;
import it.univr.vlad.fingerprinting.templates.Observer;
import it.univr.vlad.fingerprinting.devices.ble.BleManager;
import it.univr.vlad.fingerprinting.devices.mv.MagneticVector;

public class BleLiveData extends LiveData<List<Node>> implements Observer{

    private BleManager mBleManager;

    BleLiveData(Context context) {
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

    @Override
    public void startScanning() {
        mBleManager.start();
    }

    @Override
    public void stopScanning() {
        mBleManager.stop();
    }

    @Override
    public void update(NodeType type, List<Node> results) {
        if (type == NodeType.BLE) postValue(results);
    }

    @Override
    public void update(MagneticVector mv) {}
}
