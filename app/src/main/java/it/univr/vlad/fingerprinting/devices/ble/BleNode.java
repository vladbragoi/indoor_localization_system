package it.univr.vlad.fingerprinting.devices.ble;

import it.univr.vlad.fingerprinting.templates.Node;

public abstract class BleNode extends Node {

    private String uid;

    BleNode(String id, int value, String uid){
        super(id, value, "BLE");
        this.uid = uid;
    }

    @Override
    public String toString() {
        return super.toString() + " uid: " + uid;
    }
}