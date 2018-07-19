package it.univr.vlad.fingerprinting.viewmodel;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import java.sql.Time;
import java.util.List;

import it.univr.vlad.fingerprinting.Node;
import it.univr.vlad.fingerprinting.Observer;
import it.univr.vlad.fingerprinting.mv.MagneticVector;
import it.univr.vlad.fingerprinting.mv.MvManager;

class MagneticVectorLiveData extends LiveData<MagneticVector> implements Observer {

    private MvManager mMvManager;

    MagneticVectorLiveData(Context context) {
        mMvManager = new MvManager(context);
        mMvManager.registerObserver(this);
    }

    @Override
    protected void onActive() {
        super.onActive();
        mMvManager.bind();
    }

    @Override
    protected void onInactive() {
        mMvManager.unbind();
        super.onInactive();
    }

    @Override
    public void update(int type, List<Node> results) {}

    @Override
    public void update(MagneticVector mv) {
        setValue(mv);
    }
}
