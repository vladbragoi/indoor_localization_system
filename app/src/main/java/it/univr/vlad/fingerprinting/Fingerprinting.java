package it.univr.vlad.fingerprinting;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import it.univr.vlad.fingerprinting.ble.BleManager;
import it.univr.vlad.fingerprinting.mv.MvManager;
import it.univr.vlad.fingerprinting.wifi.WifiManager;

public class Fingerprinting implements Observer {

    private static final int PERMISSIONS_REQUEST = 42;

    private BleManager bleManager;
    private LocationManager mLocationManager;
    private WifiManager wifiManager;
    private MvManager mvManager;
    private Context mContext;

    public Fingerprinting(Context context) {
        mContext = context;
        mLocationManager = (LocationManager)mContext
                .getSystemService(Context.LOCATION_SERVICE);

        mvManager = new MvManager();
        wifiManager = new WifiManager(context);
        bleManager = new BleManager(context);

        checkForPermissions();
    }

    /**
     * @brief registers all observers and bind managers
     * At first the method checks if location is enabled (needed both for wifi and beacons)
     * calling enableLocation() method if not. Then calls all registerObserver() and bind() methods
     * of the managers.
     */
    public void start() {
        if (!isLocationEnabled()) enableLocation();

        mvManager.registerObserver(this);
        wifiManager.registerObserver(this);
        bleManager.registerObserver(this);

        mvManager.bind();
        wifiManager.bind();
        bleManager.bind();
    }

    /**
     * @brief unbinds all managers and unregisters observers
     * The method calls the methods unbind() and unregisterObservers() of all managers due to stop the
     * execution of scans.
     */
    public void stop() {
        bleManager.unbind();
        wifiManager.unbind();
        mvManager.unbind();

        bleManager.unregisterObserver(this);
        wifiManager.unregisterObserver(this);
        mvManager.unregisterObserver(this);
    }

    /**
     * @brief prints wifi and beacons nodes received
     * @param type 0 for WifiManager, 1 for BleManager
     * @param results list of beacons or wifi nodes
     */
    @Override public void update(int type, List<Node> results) {
        if (type == 0) {
            System.out.println("Wifi nodes: " + results);
        }
        if (type == 1) {
            System.out.println("Beacons: " + results);
        }
    }

    @Override public void update(float[] mv) {
        // System.out.println(mv[0] + " " + mv[1] + " " + mv[2]);
    }

    private boolean isLocationEnabled() {
        boolean gps_enabled;
        boolean network_enabled;

        assert mLocationManager != null;
        gps_enabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        network_enabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return gps_enabled || network_enabled;
    }

    /**
     * @brief asks for the localization to be enabled
     */
    private void enableLocation() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
        dialog.setTitle(mContext.getString(R.string.location_title));
        dialog.setMessage(mContext.getString(R.string.location_message));
        dialog.setPositiveButton(android.R.string.ok, (dialog1, which) -> mContext
                .startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
        dialog.setNegativeButton(android.R.string.no, (dialog2, which) -> dialog2.dismiss());
        dialog.show();
    }

    /**
     * @brief asks for permission if not granted
     * For each permission, if not granted, asks it to be given
     */
    private void checkForPermissions() {
        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.BLUETOOTH};

        if (!permissionsGranted((Activity) mContext, permissions)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(mContext.getString(R.string.location_permissions_title));
            builder.setMessage(mContext.getString(R.string.location_permissions_message));
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(dialog -> ActivityCompat
                    .requestPermissions((Activity) mContext, permissions, PERMISSIONS_REQUEST));
            builder.show();
        }
    }

    /**
     * @brief return whether permissions are granted or not
     * @param activity the application activity
     * @param permissions the list of permissions needed
     * @return true if all permissions are granted, false otherwise
     */
    private boolean permissionsGranted(@NotNull Activity activity, String[] permissions) {
        for (String s: permissions) {
            if (ContextCompat.checkSelfPermission(activity, s) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }
}
