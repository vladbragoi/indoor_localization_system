package it.univr.vlad.fingerprinting.mv;

public class MagneticVector {

    private float[] values;

    public MagneticVector(float[] values) {
        this.values = values;
    }

    public float[] getValues() {
        return values;
    }

    @Override
    public String toString() {
        return "" + values[0] + " " + values[1] + " " + values[2];
    }
}
