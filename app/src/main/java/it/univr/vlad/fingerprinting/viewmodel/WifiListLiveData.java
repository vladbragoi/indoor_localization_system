package it.univr.vlad.fingerprinting.viewmodel;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import java.util.List;

import it.univr.vlad.fingerprinting.Node;
import it.univr.vlad.fingerprinting.Observer;
import it.univr.vlad.fingerprinting.mv.MagneticVector;
import it.univr.vlad.fingerprinting.wifi.WifiManager;

public class WifiListLiveData extends LiveData<List<Node>> implements Observer{

    private WifiManager mWifiManager;

    WifiListLiveData(Context context) {
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
    public void update(int type, List<Node> results) {
        if (type == 0) setValue(results);
    }

    @Override
    public void update(MagneticVector mv) {}
}
