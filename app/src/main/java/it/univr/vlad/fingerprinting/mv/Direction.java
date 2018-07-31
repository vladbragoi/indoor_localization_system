package it.univr.vlad.fingerprinting.mv;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class Direction implements SensorEventListener {

    private static Direction direction;
    private DirectionListener listener;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private float azimut;
    private float[] mGravity;
    private float[] mGeomagneticField;

    public static Direction getInstance(Context context) {
        if (direction == null) direction = new Direction(context);
        return direction;
    }

    private Direction(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        assert sensorManager != null;
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void setDirectionListener(DirectionListener listener) {
        this.listener = listener;
    }

    public void startListening() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stopListening() {
        sensorManager.unregisterListener(this, magnetometer);
        sensorManager.unregisterListener(this, accelerometer);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = sensorEvent.values;
        if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagneticField = sensorEvent.values;
        if (mGravity != null && mGeomagneticField != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagneticField);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                // orientation contains: azimut, pitch and roll
                azimut = (float) Math.toDegrees(orientation[0]);

                if (listener != null) listener.onDirectionUpdated(mGeomagneticField, azimut);
            }
        }
    }

    public String getDirection() {
        String direction;
        if (azimut >= 22.5 && azimut < 67.5)
            direction = "NORTH - EAST";
        else if (azimut >= 67.5 && azimut < 112.5)
            direction = "EAST";
        else if (azimut >= 112.5 && azimut < 157.5)
            direction = "SOUTH - EAST";
        else if ((azimut >= 157.5 && azimut < 180)
                || (azimut <= -157.5 && azimut > -180))
            direction = "SOUTH";
        else if (azimut <= -22.5 && azimut > -67.5)
            direction = "NORTH - WEST";
        else if (azimut <= -67.5 && azimut > -112.5)
            direction = "WEST";
        else if (azimut <= -112.5 && azimut >= -157.5)
            direction = "SOUTH - WEST";
        else
            direction = "NORTH";

        return direction;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    public interface DirectionListener {
        void onDirectionUpdated(float[] geomagneticField, float azimut);
    }
}
