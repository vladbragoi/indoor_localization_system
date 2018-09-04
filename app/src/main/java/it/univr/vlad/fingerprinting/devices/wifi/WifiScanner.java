package it.univr.vlad.fingerprinting.devices.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.univr.vlad.fingerprinting.templates.Node;

public class WifiScanner extends BroadcastReceiver {

    private static Set<String> addresses;

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

        if (addresses == null) new LoadDataAsyncTask(context).execute();
    }

    public void register() {
        mContext.registerReceiver(this,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    public void unregister() {
        mContext.unregisterReceiver(this);
    }

    @Override
    public void onReceive(@NotNull Context context, Intent intent) {
        if (!isScanning) return;

        mResults.clear();

        for (ScanResult result : mWifiManager.getScanResults()) {
            //if (addresses.contains(result.BSSID)) {
                mResults.add(new WifiNode(result.BSSID, result.SSID, result.level));
            //}
        }

        Collections.sort(mResults, (o1, o2) -> o2.getId().compareTo(o1.getId()));

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
        mWifiManager.setWifiEnabled(true);
    }

    public void setWifiListerner(WifiListener wifiListerner) {
        mListener = wifiListerner;
    }

    public interface WifiListener {
        void onResultsChanged(List<Node> mResults);
    }

    private static class LoadDataAsyncTask extends AsyncTask<Void,Void,Set<String>> {

        private WeakReference<Context> context;

        LoadDataAsyncTask(Context context){
            this.context = new WeakReference<>(context);
        }

        @Override
        protected Set<String> doInBackground(Void... voids) {
            String rawJson;
            try {
                InputStream is = context.get().getAssets().open("access_points.json");

                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();

                rawJson = new String(buffer, "UTF-8");

            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
            return parseAddresses(rawJson);
        }

        @Override
        protected void onPostExecute(Set<String> data) {
            addresses = data;
        }

        private Set<String> parseAddresses(String rawJson) {
            Set<String> addresses = new HashSet<>();
            JsonElement jElement = new JsonParser().parse(rawJson).getAsJsonObject();

            JsonObject jobject = jElement.getAsJsonObject();
            jobject = jobject.getAsJsonObject("mac_addresses");

            JsonArray jarray = jobject.getAsJsonArray("5GHz");
            for (JsonElement je: jarray) addresses.add(je.getAsString());

            jarray = jobject.getAsJsonArray("24GHz");
            for (JsonElement je: jarray) addresses.add(je.getAsString());

            return addresses;
        }
    }
}
