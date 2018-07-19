package it.univr.vlad.fingerprinting.mv;

public class MagneticVector {

    private float[] values;
    private float azimut;

    public MagneticVector(float[] values, float azimut) {
        this.values = values;
        this.azimut = azimut;
    }

    public float[] getValues() {
        return values;
    }

    private String getDirection() {
        String direction;
        if (azimut >= 22.5 && azimut < 67.5)
            direction = "NORTH-EAST";
        else if (azimut >= 67.5 && azimut < 112.5)
            direction = "EAST";
        else if (azimut >= 112.5 && azimut < 157.5)
            direction = "SOUTH-EAST";
        else if ((azimut >= 157.5 && azimut < 180)
                || (azimut <= -157.5 && azimut > -180))
            direction = "SOUTH";
        else if (azimut <= -22.5 && azimut > -67.5)
            direction = "NORTH-WEST";
        else if (azimut <= -67.5 && azimut > -112.5)
            direction = "WEST";
        else if (azimut <= -112.5 && azimut >= -157.5)
            direction = "SOUTH-WEST";
        else
            direction = "NORTH";

        return direction;
    }

    @Override
    public String toString() {
        return getDirection();
    }
}
