package it.univr.vlad.fingerprinting.viewmodel;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import java.util.List;

import it.univr.vlad.fingerprinting.templates.Node;
import it.univr.vlad.fingerprinting.templates.NodeType;
import it.univr.vlad.fingerprinting.templates.Observer;
import it.univr.vlad.fingerprinting.devices.mv.MagneticVector;
import it.univr.vlad.fingerprinting.devices.mv.MvManager;

class MagneticVectorLiveData extends LiveData<MagneticVector> implements Observer {

    private MvManager mMvManager;

    MagneticVectorLiveData(Context context) {
        mMvManager = new MvManager(context);
        mMvManager.registerObserver(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onActive() {
        super.onActive();
        mMvManager.bind();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onInactive() {
        mMvManager.unbind();
        super.onInactive();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startScanning() {
        // mMvManager.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopScanning() {
        // mMvManager.stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(NodeType type, List<Node> results) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(MagneticVector mv) {
        postValue(mv);
    }
}
