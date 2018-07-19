package it.univr.vlad.fingerprinting;

import java.util.List;

public interface ScanResults {
    void registerObserver(Observer observer);
    void unregisterObserver(Observer observer);
    void notifyObservers(List<Node> results);
    void notifyObservers(float[] geomagneticField, float azimut);
}
