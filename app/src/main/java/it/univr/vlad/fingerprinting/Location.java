package it.univr.vlad.fingerprinting;

import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;

import java.util.List;
import java.util.Map;

import it.univr.vlad.fingerprinting.devices.mv.MagneticVector;
import it.univr.vlad.fingerprinting.templates.Node;

public class Location {

    private final static String DIRECTION_KEY = "direction";
    private final static String MEASURE_KEY = "measures";

    private Measure measuration = new Measure();
    private String user;
    private String direction;

    public Location(String user) {
        this.user = user;
    }

    /**
     * Saves Location data on database.
     * @param database CouchDB database
     */
    public void saveInto(Database database) {
        boolean noErrors = true;
        Document document = database.getDocument(user);
        try {
            document.update(newRevision -> {
                Map<String, Object> properties = newRevision.getUserProperties();
                properties.put("type", "data_doc");
                properties.put(DIRECTION_KEY, direction);
                properties.put(MEASURE_KEY, measuration);
                newRevision.setProperties(properties);
                return true;
            });
        } catch (CouchbaseLiteException e) {
            Log.w(user, "DOC: cannot update the document", e);
            noErrors = false;
        }

        if (noErrors) Log.d(user, "DOC: updated document");

        measuration.clear();
    }

    public void addWifiNodes(List<Node> nodes) {
        measuration.addWifiNodes(nodes);
    }

    public void addBeaconNodes(List<Node> nodes) {
        measuration.addBleNodes(nodes);
    }

    public void addMagneticVector(MagneticVector mv) {
        measuration.addMagneticVector(mv);
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}
