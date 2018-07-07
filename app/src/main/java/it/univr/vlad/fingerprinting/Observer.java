package it.univr.vlad.fingerprinting;

import java.util.List;

public interface Observer {
    /**
     *
     * @param type 0 for WifiManager, 1 for BleManager
     * @param results list of beacons or wifi nodes
     */
    void update(int type, List<Node> results);

    /**
     *
     * @param mv magnetic vector
     */
    void update(float[] mv);
}
