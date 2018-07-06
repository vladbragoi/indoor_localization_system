package it.univr.vlad.fingerprinting;

import java.util.List;

import it.univr.vlad.fingerprinting.mv.MvManager;
import it.univr.vlad.fingerprinting.wifi.WifiManager;

public class Fingerprinting implements Observer {

    private WifiManager wifiManager;
    private MvManager mvManager;

    @Override
    public void update(int type, List<Node> results) {

    }

    @Override
    public void update(float[] mv) {

    }
}
