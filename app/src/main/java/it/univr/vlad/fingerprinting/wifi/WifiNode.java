package it.univr.vlad.fingerprinting.wifi;

import it.univr.vlad.fingerprinting.Node;

public class WifiNode extends Node {

    private String ssid;    /// Access Point name

    public WifiNode(String id, String ssid, int value) {
        super(id, value);
        this.ssid = ssid;
    }

    @Override
    public String toString() {
        return super.toString() + " ssid: " + ssid;
    }
}
