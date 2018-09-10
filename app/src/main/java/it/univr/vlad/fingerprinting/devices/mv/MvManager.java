package it.univr.vlad.fingerprinting.devices.mv;

import android.content.Context;

import java.util.List;

import it.univr.vlad.fingerprinting.templates.Manager;
import it.univr.vlad.fingerprinting.templates.Node;
import it.univr.vlad.fingerprinting.templates.Observer;

public class MvManager extends Manager implements Direction.DirectionListener {

    private Direction direction;

    public MvManager(Context context) {
        direction = Direction.getInstance(context);
    }

    @Override
    public void bind() {
        start();
        direction.setDirectionListener(this);
    }

    @Override
    public void unbind() {
        stop();
    }

    @Override
    public void start() {
        direction.startListening();
    }

    @Override
    public void stop() {
        direction.stopListening();
    }

    @Override
    public void notifyObservers(List<Node> results) {}

    @Override
    public void notifyObservers(float[] geomagneticField, float azimut) {
        for (Observer o: super.mObservers)
            o.update(new MagneticVector(geomagneticField, azimut));
    }

    @Override
    public void onDirectionUpdated(float[] geomagneticField, float azimut) {
        notifyObservers(geomagneticField, azimut);
    }
}
