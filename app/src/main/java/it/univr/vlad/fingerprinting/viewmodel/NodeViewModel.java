package it.univr.vlad.fingerprinting.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import java.util.List;

import it.univr.vlad.fingerprinting.Node;
import it.univr.vlad.fingerprinting.mv.MagneticVector;

public class NodeViewModel extends AndroidViewModel {

    private final WifiLiveData wifiList;
    private final BleLiveData beaconList;
    private final MagneticVectorLiveData mv;

    public NodeViewModel(@NonNull Application application) {
        super(application);
        wifiList = new WifiLiveData(application);
        beaconList = new BleLiveData(application);
        mv = new MagneticVectorLiveData(application);
    }

    public void startWifiScanning() {
        wifiList.startScanning();
    }

    public void stopWifiScanning() {
        wifiList.stopScanning();
    }

    public void startBeaconsScanning() {
        beaconList.startScanning();
    }

    public void stopBeaconsScanning() {
        beaconList.stopScanning();
    }

    public LiveData<List<Node>> getWifiList() {
        return wifiList;
    }

    public LiveData<List<Node>> getBeaconList() {
        return beaconList;
    }

    public LiveData<MagneticVector> getMv() {
        return mv;
    }
}
