package it.univr.vlad.fingerprinting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Manager implements ScanResults {

    private List<Node> mNodes;
    private Set<Observer> mObservers;

    public Manager() {
        mNodes = new ArrayList<>();
        mObservers = new HashSet<>();
    }

    protected abstract void bind();
    protected abstract void unBind();

    @Override public void registerObserver(Observer observer) {
        mObservers.add(observer);
    }

    @Override public void unregisterObserver(Observer observer) {
        mObservers.remove(observer);
    }

    @Override public void notifyObservers() {
        if (mNodes != null && !mNodes.isEmpty()) {
            for (Observer o: mObservers)
                o.update(mNodes);
        }
    }
}
