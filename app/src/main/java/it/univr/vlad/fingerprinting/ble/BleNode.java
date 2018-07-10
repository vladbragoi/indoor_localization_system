package it.univr.vlad.fingerprinting.ble;

import it.univr.vlad.fingerprinting.Node;

public abstract class BleNode extends Node {

    private String uid;

    public BleNode(String id, int value, String uid){
        super(id, value);
        this.uid = uid;
    }

    @Override
    public String toString() {
        return super.toString() + " uid: " + uid;
    }
}