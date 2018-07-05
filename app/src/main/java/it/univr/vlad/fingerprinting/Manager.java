package it.univr.vlad.fingerprinting;

import java.util.HashSet;
import java.util.Set;

public abstract class Manager implements ScanResults {

    protected Set<Observer> mObservers;

    protected Manager() {
        mObservers = new HashSet<>();
    }

    protected abstract void bind();
    protected abstract void unbind();

    @Override public void registerObserver(Observer observer) {
        mObservers.add(observer);
    }

    @Override public void unregisterObserver(Observer observer) {
        mObservers.remove(observer);
    }
}
