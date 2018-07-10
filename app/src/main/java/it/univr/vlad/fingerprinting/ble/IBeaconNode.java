package it.univr.vlad.fingerprinting.ble;

public class IBeaconNode extends BleNode {

    private String major;
    private String minor;

    public IBeaconNode(String id, int value, String uid, String major, String minor) {
        super(id, value, uid);
        this.major = major;
        this.minor = minor;
    }

    @Override
    public String toString() {
        return super.toString() + " major: " + major + " minor: " + minor;
    }

}
