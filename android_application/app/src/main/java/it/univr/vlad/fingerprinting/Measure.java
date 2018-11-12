package it.univr.vlad.fingerprinting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.univr.vlad.fingerprinting.devices.mv.MagneticVector;
import it.univr.vlad.fingerprinting.templates.Node;

public class Measure {

    private List<List<Node>> wifi = new ArrayList<>();
    private List<List<Node>> ble = new ArrayList<>();
    private List<MagneticVector> mv = new ArrayList<>();

    public void addWifiNodes(List<Node> wifiNodes) {
        List<Node> newNodes = new ArrayList<>(wifiNodes);
        Collections.copy(newNodes, wifiNodes);
        this.wifi.add(newNodes);
    }

    public void addBleNodes(List<Node> beaconNodes) {
        List<Node> newNodes = new ArrayList<>(beaconNodes);
        Collections.copy(newNodes, beaconNodes);
        this.ble.add(newNodes);
    }

    public void addMagneticVector(MagneticVector mv) {
        MagneticVector newMv = new MagneticVector(mv);
        this.mv.add(newMv);
    }

    public List<List<Node>> getWifi() {
        return wifi;
    }

    public List<List<Node>> getBle() {
        return ble;
    }

    public List<MagneticVector> getMv() {
        return mv;
    }

    public void clear() {
        wifi.clear();
        ble.clear();
        mv.clear();
    }
}
