package it.univr.vlad.fingerprinting.templates;

import java.util.HashSet;
import java.util.Set;

public abstract class Manager implements ScanResults {

    protected Set<Observer> mObservers;

    protected Manager() {
        mObservers = new HashSet<>();
    }

    /**
     * You need to override this method if you want to implement
     * start scanning data from the specific device.
     */
    public abstract void start();

    /**
     * You need to override this method if you want to implement
     * stop scanning data mechanism.
     */
    public abstract void stop();

    /**
     * You need to override this method if order to bind to the
     * specific service of the desired device.
     */
    protected abstract void bind();

    /**
     * You need to override this method if order to unbind from the
     * service of the device.
     */
    protected abstract void unbind();

    @Override
    public void registerObserver(Observer observer) {
        mObservers.add(observer);
    }

    @Override
    public void unregisterObserver(Observer observer) {
        mObservers.remove(observer);
    }
}
