package it.univr.vlad.fingerprinting.templates;

import java.util.List;

import it.univr.vlad.fingerprinting.devices.mv.MagneticVector;

public interface Observer {

    /**
     * @param type 0 for WifiManager, 1 for BleManager
     * @param results list of beacons or wifi nodes
     */
    void update(NodeType type, List<Node> results);

    /**
     * @param mv magnetic vector
     */
    void update(MagneticVector mv);

    void startScanning();
    void stopScanning();
}
