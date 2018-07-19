package it.univr.vlad.fingerprinting.mv;

import android.content.Context;

import java.util.List;

import it.univr.vlad.fingerprinting.Manager;
import it.univr.vlad.fingerprinting.Node;
import it.univr.vlad.fingerprinting.Observer;

public class MvManager extends Manager {

    private Direction direction;
    private float[] mGeomagneticField;

    public MvManager(Context context) {
        direction = Direction.getInstance(context);
    }

    @Override public void bind() {
        direction.startListening();
        direction.setDirectionListener((float[] geomagneticField, float azimut) -> {
            this.mGeomagneticField = geomagneticField;
            notifyObservers(mGeomagneticField, azimut);
        });
    }

    @Override public void unbind() {
        direction.stopListening();
    }

    @Override
    public boolean isDeviceEnabled() {
        return true;
    }

    @Override
    public void enableDevice() {}

    @Override
    public void notifyObservers(List<Node> results) {}

    @Override
    public void notifyObservers(float[] geomagneticField, float azimut) {
        for (Observer o: super.mObservers)
            o.update(new MagneticVector(geomagneticField, azimut));
    }
}
