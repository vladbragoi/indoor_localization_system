package it.univr.vlad.fingerprinting.wifi;

import android.Manifest;
import android.app.Activity;
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

import it.univr.vlad.fingerprinting.Node;

public class WifiScanner extends BroadcastReceiver {

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

        int permissions_code = 0;
        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE};

        if(!permissionsGranted((Activity) context, permissions)){
            ActivityCompat.requestPermissions((Activity) context, permissions, permissions_code);
        }
    }

    private boolean permissionsGranted(Activity activity, String[] permissions) {
        for (String s: permissions) {
            if (ContextCompat.checkSelfPermission(activity, s) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
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

        if (mResults != null && !mResults.isEmpty()) mListener.onResultsChanged(mResults);

        mWifiManager.startScan();
    }

    public void start() {
        if (!mWifiManager.isWifiEnabled()){
            mWifiManager.setWifiEnabled(true);
            Toast.makeText(mContext, "Wifi enabled", Toast.LENGTH_SHORT).show();
        }
        mWifiManager.startScan();
        isScanning = true;
    }

    public void stop() {
        isScanning = false;
    }

    public void setWifiListerner(WifiListener wifiListerner) {
        mListener = wifiListerner;
    }

    public interface WifiListener {
        void onResultsChanged(List<Node> mResults);
    }
}
