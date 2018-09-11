package it.univr.vlad.fingerprinting.activity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;

import org.jetbrains.annotations.NotNull;

import es.dmoral.toasty.Toasty;
import it.univr.vlad.fingerprinting.R;
import it.univr.vlad.fingerprinting.util.Dialog;
import it.univr.vlad.fingerprinting.view.FingerprintingFragment;
import it.univr.vlad.fingerprinting.view.LocalizationFragment;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final static int PERMISSIONS_REQUEST = 42;
    private final static int LOCATION_RESULT_CODE = 4295;
    private final static String FINGERPRINTING_FRAGMENT = "fing";
    private final static String LOCALIZATION_FRAGMENT = "loc";

    private final static String[] permissions = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.BLUETOOTH};

    private Fragment fingerprintingFragment;
    private Fragment localizationFragment;

    private TextView userTextView;

    private SharedPreferences sharedPreferences;
    SharedPreferences.OnSharedPreferenceChangeListener changeListener = (sharedPreferences, key) -> {
        if (key.equals("username") && userTextView != null) {
            userTextView.setText(sharedPreferences.getString(key, ""));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawer,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        checkForPermissions();

        View navigationHeaderView = navigationView.getHeaderView(0);
        userTextView = navigationHeaderView.findViewById(R.id.username);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(changeListener);
        userTextView.setText(sharedPreferences.getString("username", ""));

        fingerprintingFragment = new FingerprintingFragment();
        localizationFragment = new LocalizationFragment();

        getSupportFragmentManager().beginTransaction().replace(
                R.id.fragment_container,
                fingerprintingFragment,
                FINGERPRINTING_FRAGMENT
        ).commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(changeListener);
        disableBluetooth();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOCATION_RESULT_CODE) {
            if (resultCode != Activity.RESULT_OK) locationNotEnabled();
        } else
            super.onActivityResult(requestCode, resultCode, data);
    }

    ///////////////////////////////////////////////
    //            PUBLIC METHODS
    ///////////////////////////////////////////////

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.fragment_container);

        if (fragment instanceof FingerprintingFragment
                && ((FingerprintingFragment) fragment).closeSpeedDial()) {}
        else if (fragment instanceof LocalizationFragment) {
            NavigationView navigationView = findViewById(R.id.nav_view);
            navigationView.getMenu().findItem(R.id.nav_fingerprinting).setChecked(true);
            super.onBackPressed();
        }
        else if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NotNull MenuItem item) {
        // Handle navigation view item clicks here.

        switch(item.getItemId()) {
            case R.id.nav_fingerprinting: startFingerprintingFragment(); break;
            case R.id.nav_localization: startLocalizationFragment(); break;
            case R.id.nav_info: Dialog.showInfoDialog(this);break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST:
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                            Log.w(getClass().getName(),
                                    "Permission " + permissions[i] + " not granted!");
                    }
                } else {
                    Log.w(getClass().getName(), "No permissions granted!");
                }
                break;

            default:
        }
    }

    /**
     * Enables location using Google API
     */
    public void turnLocationOn() {
        // ENABLE LOCATION WITH GOOGLE-API
        LocationRequest locationRequest= new LocationRequest();

        // Low-Precision Location (low power)
        locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        // Request to enable location
        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(this,
                            LOCATION_RESULT_CODE);
                } catch (IntentSender.SendIntentException e1) {
                    e1.printStackTrace();
                }
            }
        });

        /* ENABLE LOCATION THROUGH SYSTEM SETTINGS
        LocationManager locationManager = (LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;

        if (!isLocationEnabled(locationManager)) {
            final AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setTitle(context.getString(R.string.location_title));
            dialog.setMessage(context.getString(R.string.location_message));
            dialog.setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                *//*Activity activity = getActivity();
                if (activity != null)
                    activity.startActivityForResult(
                            new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                            LOCATION_RESULT_CODE);*//*
            });
            dialog.setNegativeButton(android.R.string.no, (dialog2, which) -> {
                    dialog2.dismiss();
                    locationNotEnabled();
            });
            dialog.show();
        }*/
    }

    /**
     * Enable bluetooth on device
     * @param fragment Calling fragment to set bluetooth checkbox checked
     */
    public void turnBluetoothOn(Fragment fragment) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toasty.warning(this,
                        getString(R.string.ble_not_supported),
                        Toast.LENGTH_SHORT, true).show();
            }
            else {
                /*
                 * Should use this method, but just to uniform the requests
                 * mContext.startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                 */
                final AlertDialog.Builder dialog =
                        new AlertDialog.Builder(this);
                dialog.setTitle(getString(R.string.bluetooth_title));
                dialog.setMessage(getString(R.string.bluetooth_message));
                dialog.setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                    if (!bluetoothAdapter.isEnabled()) {
                        bluetoothAdapter.enable();
                        Toasty.success(this,
                                getString(R.string.bluetooth_enabled),
                                Toast.LENGTH_SHORT, true).show();
                    }
                });
                dialog.setNegativeButton(android.R.string.no, (dialog2, which) -> {
                    if (fragment instanceof LocalizationFragment) {
                        ((LocalizationFragment) fragment).mBeaconCheckbox.setChecked(false);
                    }
                    else if (fragment instanceof FingerprintingFragment) {
                        ((FingerprintingFragment) fragment).mBeaconCheckbox.setChecked(false);
                    }

                    dialog2.dismiss();
                });
                dialog.setOnCancelListener(diag ->
                        startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                );
                dialog.show();
            }
        }
    }

    /**
     * Enable wifi on device
     * @param fragment Calling fragment to set wifi checkbox checked
     */
    public void turnWifiOn(Fragment fragment) {
        WifiManager wifiManager = (WifiManager) this
                .getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        if (wifiManager != null && !wifiManager.isWifiEnabled()) {
            final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.wifi_title));
            dialog.setMessage(getString(R.string.wifi_message));
            dialog.setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                wifiManager.setWifiEnabled(true);
                Toasty.success(this,
                        getString(R.string.wifi_enabled),
                        Toast.LENGTH_SHORT, true).show();
            });
            dialog.setNegativeButton(android.R.string.no, (dialog2, which) -> {
                if (fragment instanceof LocalizationFragment) {
                    ((LocalizationFragment) fragment).mWifiCheckbox.setChecked(false);
                }
                else if (fragment instanceof FingerprintingFragment) {
                    ((FingerprintingFragment) fragment).mWifiCheckbox.setChecked(false);
                }
                dialog2.dismiss();
            });
            dialog.setOnCancelListener(diag ->
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS))
            );
            dialog.show();
        }
    }

    ///////////////////////////////////////////////
    //            PRIVATE METHODS
    ///////////////////////////////////////////////

    private void startFingerprintingFragment() {
        if (!fingerprintingFragment.isAdded()) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.popBackStack();
            fragmentManager.beginTransaction().replace(
                    R.id.fragment_container,
                    fingerprintingFragment,
                    FINGERPRINTING_FRAGMENT
            ).commit();
        }
    }

    private void startLocalizationFragment() {
        if (!localizationFragment.isAdded()) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, localizationFragment, LOCALIZATION_FRAGMENT);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    /**
     * Asks for requested @{@link MainActivity#permissions} permissions
     */
    private void checkForPermissions() {
        if (!permissionsGranted()) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(this.getString(R.string.location_permissions_title));
            builder.setMessage(this.getString(R.string.location_permissions_message));
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(dialog -> ActivityCompat
                    .requestPermissions(this, permissions, PERMISSIONS_REQUEST));
            builder.show();
        }
    }

    /**
     * return whether permissions are granted or not
     * @return true if all permissions are granted, false otherwise
     */
    private boolean permissionsGranted() {
        for (String s: permissions) {
            if (ContextCompat.checkSelfPermission(this, s) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    /**
     * Disables bluetooth on device
     */
    private void disableBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled())
            bluetoothAdapter.disable();
    }

    /**
     * Displays a message in order to inform the user that location is disabled
     */
    private void locationNotEnabled() {
        Toast toast = Toasty.warning(this,
                getString(R.string.location_not_enabled),
                Toast.LENGTH_LONG,
                true);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }


}
