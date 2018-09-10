package it.univr.vlad.fingerprinting.devices.ble;

public class IBeaconNode extends BleNode {

    private String major;
    private String minor;

    /**
     * iBeacon Node differs from a generic BleNode in a major and minor number.
     * @param id mac address
     * @param value value
     * @param uid user ID
     * @param major major number
     * @param minor minor number
     */
    IBeaconNode(String id, int value, String uid, String major, String minor) {
        super(id, value, uid);
        this.major = major;
        this.minor = minor;
    }

    @Override
    public String toString() {
        return super.toString() + " major: " + major + " minor: " + minor;
    }

}
