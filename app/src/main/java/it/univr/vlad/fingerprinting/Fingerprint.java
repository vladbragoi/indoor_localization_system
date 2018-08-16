package it.univr.vlad.fingerprinting;

import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Fingerprint {

    private final static String X_KEY = "x";
    private final static String Y_KEY = "y";
    private final static String BORDERS_KEY = "borders";

    private List<Node> wifiNodes = new ArrayList<>();
    private List<Node> beaconNodes = new ArrayList<>();
    private String number; // The document ID
    private String x;
    private String y;
    private String borders;

    public Fingerprint(String number) {
        this.number = number;
    }

    public void saveIn(Database database) {
        boolean noErrors = true;
        Document document = database.getDocument(number);

        try {
            document.update(newRevision -> {
                Map<String, Object> properties = newRevision.getUserProperties();
                properties.put(X_KEY, x);
                properties.put(Y_KEY, y);
                properties.put(BORDERS_KEY, borders);
                newRevision.setProperties(properties);
                return true;
            });
        } catch (CouchbaseLiteException e) {
            Log.w(number, "DOC: cannot update the document", e);
            noErrors = false;
        }

        if (noErrors) Log.d(number, "DOC: updated document");
    }

    public void addWifiNodes(List<Node> nodes) {
        List<Node> newNodes = new ArrayList<>(nodes);
        this.wifiNodes.addAll(newNodes);
    }

    public void addBeaconNodes(List<Node> nodes) {
        List<Node> newNodes = new ArrayList<>(nodes);
        this.beaconNodes.addAll(newNodes);
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
}
