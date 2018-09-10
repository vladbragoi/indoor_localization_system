package it.univr.vlad.fingerprinting.viewmodel;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import java.util.List;

import it.univr.vlad.fingerprinting.templates.Node;
import it.univr.vlad.fingerprinting.templates.NodeType;
import it.univr.vlad.fingerprinting.templates.Observer;
import it.univr.vlad.fingerprinting.devices.mv.MagneticVector;
import it.univr.vlad.fingerprinting.devices.wifi.WifiManager;

public class WifiLiveData extends LiveData<List<Node>> implements Observer{

    private WifiManager mWifiManager;

    WifiLiveData(Context context) {
         mWifiManager = new WifiManager(context);
         mWifiManager.registerObserver(this);
    }

    @Override
    protected void onActive() {
        super.onActive();
        mWifiManager.bind();
    }

    @Override
    protected void onInactive() {
        mWifiManager.unbind();
        super.onInactive();
    }

    @Override
    public void startScanning() {
        mWifiManager.start();
    }

    @Override
    public void stopScanning() {
        mWifiManager.stop();
    }

    @Override
    public void update(NodeType type, List<Node> results) {
        if (type == NodeType.WIFI) postValue(results);
    }

    @Override
    public void update(MagneticVector mv) {}
}
