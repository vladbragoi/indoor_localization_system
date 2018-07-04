package it.univr.vlad.fingerprinting.mv;

import android.content.Context;

import it.univr.vlad.fingerprinting.Manager;
import it.univr.vlad.fingerprinting.Observer;
import it.univr.vlad.fingerprinting.mv.Direction;

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

    @Override
    protected void unBind() {
        direction.stopListening();
    }

    @Override
    public void notifyObservers() {
        for (Observer o: super.mObservers)
            o.update(mGeomagnetic);
    }
}
