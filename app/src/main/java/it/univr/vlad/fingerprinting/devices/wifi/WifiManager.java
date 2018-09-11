package it.univr.vlad.fingerprinting.devices.wifi;

import android.content.Context;

import java.util.List;

import it.univr.vlad.fingerprinting.templates.Manager;
import it.univr.vlad.fingerprinting.templates.Node;
import it.univr.vlad.fingerprinting.templates.NodeType;
import it.univr.vlad.fingerprinting.templates.Observer;

public class WifiManager extends Manager {

    private WifiScanner mWifiScanner;

    public WifiManager(Context context) {
        mWifiScanner = new WifiScanner(context);
        mWifiScanner.setWifiListerner(this::notifyObservers);
    }

    @Override
    public void bind() {
        mWifiScanner.register();
    }

    @Override
    public void unbind() {
        mWifiScanner.stop();
        mWifiScanner.unregister();
    }

    @Override
    public void start() {
        mWifiScanner.start();
    }

    @Override
    public void stop() {
        mWifiScanner.stop();
    }

    @Override
    public void notifyObservers(List<Node> results) {
        for (Observer observer : super.mObservers) {
            observer.update(NodeType.WIFI, results);
        }
    }

    @Override
    public void notifyObservers(float[] geomagneticField, float azimut) {}
}
