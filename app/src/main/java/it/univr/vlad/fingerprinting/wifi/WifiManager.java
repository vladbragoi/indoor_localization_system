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
        if (!isDeviceEnabled()) enableDevice();
        mWifiScanner.register();
        mWifiScanner.start();
    }

    @Override public void unbind() {
        mWifiScanner.stop();
        mWifiScanner.unregister();
    }

    @Override public void notifyObservers(List<Node> results) {
        for (Observer observer : super.mObservers) {
            observer.update(0, results);
        }
    }

    @Override public void notifyObservers() {}

    @Override public boolean isDeviceEnabled() {
        return mWifiScanner.isWifiEnabled();
    }

    @Override public void enableDevice() {
        mWifiScanner.enableWifi();
    }
}
