package it.univr.vlad.fingerprinting.wifi;

import android.content.Context;

import java.util.List;

import it.univr.vlad.fingerprinting.Manager;
import it.univr.vlad.fingerprinting.Node;
import it.univr.vlad.fingerprinting.Observer;

public class WifiManager extends Manager {

    private WifiScanner mWifiScanner;

    public WifiManager(Context context) {
        mWifiScanner = new WifiScanner(context);
        mWifiScanner.setWifiListerner(this::notifyObservers);
    }

    @Override public void bind() {
        mWifiScanner.register();
        mWifiScanner.start();
    }

    @Override
    public void unBind() {
        mWifiScanner.stop();
        mWifiScanner.unregister();
    }

    @Override
    public void notifyObservers(List<Node> results) {
        for (Observer observer : super.mObservers) {
            observer.update(results);
        }
    }

    @Override
    public void notifyObservers() {}
}
