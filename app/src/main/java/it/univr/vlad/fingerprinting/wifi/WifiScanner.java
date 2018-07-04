package it.univr.vlad.fingerprinting.wifi;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import it.univr.vlad.fingerprinting.MainActivity;
import it.univr.vlad.fingerprinting.Node;

public class WifiScanner extends BroadcastReceiver {

    private final static int REQUEST_ACCESS_COARSE_LOCATION = 1;
    private final static int REQUEST_CHANGE_WIFI_STATE = 2;

    private WifiListener mListener;

    private Context mContext;
    private WifiManager mWifiManager;
    private List<Node> mResults;

    private boolean isScanning = false;

    public WifiScanner(Context context) {
        mContext = context;
        mWifiManager = (WifiManager) context
                .getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        mResults = new ArrayList<>();
    }

    public void register() {
        mContext.registerReceiver(this,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    public void unregister() {
        mContext.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!isScanning) return;

        mResults.clear();

        for (ScanResult result : mWifiManager.getScanResults()) {
            mResults.add(new WifiNode(result.BSSID, result.SSID, result.level));
        }

        if (mResults != null) mListener.onResultsChanged(mResults);

        mWifiManager.startScan();
    }

    public void start() {
        if (permissionsGranted() && mWifiManager.isWifiEnabled()) {
            mWifiManager.startScan();
            isScanning = true;
        }
        else if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
            Toast.makeText(mContext, "Wifi enabled", Toast.LENGTH_SHORT).show();
            mWifiManager.startScan();
            isScanning = true;
        }
        else {
            // Request permissions
            ActivityCompat.requestPermissions((MainActivity) mContext,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_ACCESS_COARSE_LOCATION);
            ActivityCompat.requestPermissions((MainActivity) mContext,
                    new String[]{Manifest.permission.CHANGE_WIFI_STATE},
                    REQUEST_CHANGE_WIFI_STATE);
        }
    }

    public void stop() {
        isScanning = false;
    }

    private boolean permissionsGranted() {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(mContext, Manifest.permission.CHANGE_WIFI_STATE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    public void setWifiListerner(WifiListener wifiListerner) {
        mListener = wifiListerner;
    }

    public interface WifiListener {
        void onResultsChanged(List<Node> mResults);
    }
}
