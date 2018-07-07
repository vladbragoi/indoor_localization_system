package it.univr.vlad.fingerprinting.ble;

import it.univr.vlad.fingerprinting.Node;

public class BleNode extends Node {

    private String uid;

    //only for ibeacons else "null"
    private String major;
    private String minor;

    public BleNode(String id, int value, String uid, String major, String minor){
        super(id, value);
        this.uid = uid;
        this.major = major;
        this.minor = minor;
    }
}