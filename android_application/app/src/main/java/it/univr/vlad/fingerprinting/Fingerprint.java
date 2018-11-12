package it.univr.vlad.fingerprinting;

import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.univr.vlad.fingerprinting.devices.mv.MagneticVector;
import it.univr.vlad.fingerprinting.templates.Node;

public class Fingerprint {

    private final static String X_KEY = "x";
    private final static String Y_KEY = "y";
    private final static String BORDERS_KEY = "borders";
    private final static String MEASURE_KEY = "measures";

    private Map<String, Measure> measures = new HashMap<>();
    private Measure measure = new Measure();
    private String direction;

    private String number; // The document ID
    private String x;
    private String y;
    private String borders;
    private boolean running = false;

    public Fingerprint() { }

    /*public Fingerprint(String number) {
        this.number = number;
    }*/

    public void saveInto(Database database) {
        boolean noErrors = true;
        Document document = database.getDocument(number);
        measures.put(this.direction, measure);
        try {
            document.update(newRevision -> {
                Map<String, Object> properties = newRevision.getUserProperties();
                properties.put(X_KEY, x);
                properties.put(Y_KEY, y);
                properties.put(BORDERS_KEY, borders);
                properties.put(MEASURE_KEY, measures);
                newRevision.setProperties(properties);
                return true;
            });
        } catch (CouchbaseLiteException e) {
            Log.w(number, "DOC: cannot update the document", e);
            noErrors = false;
        }

        if (noErrors) Log.d(number, "DOC: updated document");
    }

    public void newMeasure(String direction) {
        measures.put(this.direction, measure);
        measure = new Measure();
        this.direction = direction;
    }

    public void addWifiNodes(List<Node> nodes) {
        measure.addWifiNodes(nodes);
    }

    public void addBeaconNodes(List<Node> nodes) {
        measure.addBleNodes(nodes);
    }

    public void addMagneticVector(MagneticVector mv) {
        measure.addMagneticVector(mv);
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setX(String x) {
        this.x = x;
    }

    public void setY(String y) {
        this.y = y;
    }

    public void setBorders(String borders) {
        this.borders = borders;
    }

    public void startMeasuring(String direction) {
        this.direction = direction;
        this.running = true;
    }

    public void stopMeasuring() {
        this.running = false;
    }

    public boolean isRunning() {
        return running;
    }
}