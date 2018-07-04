package it.univr.vlad.fingerprinting;

public interface ScanResults {
    void registerObserver(Observer observer);
    void unregisterObserver(Observer observer);
    void notifyObservers();
}
