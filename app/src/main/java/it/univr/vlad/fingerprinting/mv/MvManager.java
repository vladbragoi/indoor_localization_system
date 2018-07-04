package it.univr.vlad.fingerprinting.mv;

import android.content.Context;

import java.util.List;

import it.univr.vlad.fingerprinting.Manager;
import it.univr.vlad.fingerprinting.Node;
import it.univr.vlad.fingerprinting.Observer;

public class MvManager extends Manager {

    private Direction direction;
    private float[] mGeomagnetic;

    public MvManager(Context context) {
        direction = Direction.getInstance();
    }

    @Override public void bind() {
        direction.startListening();
        direction.setDirectionListener((float[] mGeomagnetic) -> {
            this.mGeomagnetic = mGeomagnetic;
            notifyObservers();
        });
    }

    @Override public void unBind() {
        direction.stopListening();
    }

    @Override
    public void notifyObservers(List<Node> results) {}

    @Override
    public void notifyObservers() {
        for (Observer o: super.mObservers)
            o.update(mGeomagnetic);
    }
}
