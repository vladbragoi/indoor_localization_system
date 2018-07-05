package it.univr.vlad.fingerprinting;

public interface ScanResults {
    void registerObserver(Observer scanResultsObserver);
    void unregisterObserver(Observer scanResultsObserver);
    void notifyObservers();
}
