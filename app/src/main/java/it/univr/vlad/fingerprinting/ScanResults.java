package it.univr.vlad.fingerprinting;

public interface ScanResults<T> {
    void registerObserver(Observer<T> scanResultsObserver);
    void unregisterObserver(Observer<T> scanResultsObserver);
    void notifyObservers();
}
