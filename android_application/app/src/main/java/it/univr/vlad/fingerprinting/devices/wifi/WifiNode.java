package it.univr.vlad.fingerprinting.devices.wifi;

import it.univr.vlad.fingerprinting.templates.Node;

public class WifiNode extends Node {

    private String ssid;    /// Access Point's name

    WifiNode(String id, String ssid, int value,  String timestamp) {
        super(id, value, timestamp, "WIFI");
        this.ssid = ssid;
    }

    @Override
    public String toString() {
        return super.toString() + " ssid: " + ssid;
    }
}