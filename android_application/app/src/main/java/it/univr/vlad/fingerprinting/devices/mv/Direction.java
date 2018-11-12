package it.univr.vlad.fingerprinting.devices.mv;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.view.Gravity;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import es.dmoral.toasty.Toasty;
import it.univr.vlad.fingerprinting.R;

public class Direction implements SensorEventListener {

    private static Direction direction;
    private DirectionListener listener;

    private Toast toast;
    private boolean shown = false;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private float azimut;
    private float[] mGravity;
    private float[] mGeomagneticField;

    /**
     * Singleton class.
     * @param context context
     * @return the instance of this singleton class
     */
    public static Direction getInstance(Context context) {
        if (direction == null) direction = new Direction(context);
        return direction;
    }

    private Direction(@NotNull Context context) {
        // Centered text
        SpannableString string = new SpannableString(context.getString(R.string.tilt_mode));
        string.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                0,
                string.length(),
                Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        // Custom toast for no flat device
        toast = Toasty.warning(context,
                string,
                Toast.LENGTH_SHORT,
                true);
        toast.setGravity(Gravity.CENTER, 0 , 0);

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        assert sensorManager != null;
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void setDirectionListener(DirectionListener listener) {
        this.listener = listener;
    }

    /**
     * Starts listening for accelerometer and magnetometer sensor data.
     */
    public void startListening() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * Stops listening for sensor data.
     */
    public void stopListening() {
        sensorManager.unregisterListener(this, magnetometer);
        sensorManager.unregisterListener(this, accelerometer);
    }

    /**
     * {@inheritDoc}
     */
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

                if (!shown) { // Check if device is flat or not
                    float[] g = mGravity.clone();
                    double normOfG= Math.sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]);
                    g[0] = (float) (g[0] / normOfG);
                    g[1] = (float) (g[1] / normOfG);
                    g[2] = (float) (g[2] / normOfG);

                    int inclination = (int) Math.round(Math.toDegrees(Math.acos(g[2])));

                    if (inclination > 60 && inclination < 145) { // Device is not flat
                        toast.show();
                        shown = true;
                    }
                }
            }
        }
    }

    /**
     * Gets the device orientation.
     * @return a string containing the orientation (NORTH, SOUTH,...).
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    /**
     * Listener interface in order to be notified for changed values.
     */
    public interface DirectionListener {
        void onDirectionUpdated(float[] geomagneticField, float azimut);
    }
}
