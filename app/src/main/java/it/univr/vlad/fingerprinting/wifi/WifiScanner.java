package it.univr.vlad.fingerprinting.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import it.univr.vlad.fingerprinting.Node;
import it.univr.vlad.fingerprinting.R;

public class WifiScanner extends BroadcastReceiver {

    private WifiListener mListener;

    private Context mContext;
    private WifiManager mWifiManager;
    private List<Node> mResults;

    private boolean isScanning = false;

    public WifiScanner(@NotNull Context context) {
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

    @Override public void onReceive(@NotNull Context context, Intent intent) {
        if (!isScanning) return;

        mResults.clear();

        for (ScanResult result : mWifiManager.getScanResults()) {
            mResults.add(new WifiNode(result.BSSID, result.SSID, result.level));
        }

        if (mResults != null && !mResults.isEmpty()) mListener.onResultsChanged(mResults);

        mWifiManager.startScan();
    }

    public void start() {
        mWifiManager.startScan();
        isScanning = true;
    }

    public void stop() {
        isScanning = false;
    }

    public boolean isWifiEnabled() {
        return mWifiManager.isWifiEnabled();
    }

    public void enableWifi() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
        dialog.setTitle(mContext.getString(R.string.wifi_title));
        dialog.setMessage(mContext.getString(R.string.wifi_message));
        dialog.setPositiveButton(android.R.string.ok, (dialog1, which) -> {
            mWifiManager.setWifiEnabled(true);
            Toast.makeText(mContext,
                    mContext.getString(R.string.wifi_enabled),
                    Toast.LENGTH_SHORT).show();
        });
        dialog.setNegativeButton(android.R.string.no, (dialog2, which) -> dialog2.dismiss());
        dialog.setOnCancelListener(diag -> mContext
                .startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS))
        );
        dialog.show();
    }

    public void setWifiListerner(WifiListener wifiListerner) {
        mListener = wifiListerner;
    }

    public interface WifiListener {
        void onResultsChanged(List<Node> mResults);
    }
}
