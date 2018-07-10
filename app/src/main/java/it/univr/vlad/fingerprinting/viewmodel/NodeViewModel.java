package it.univr.vlad.fingerprinting.viewmodel;


import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import java.util.List;

import it.univr.vlad.fingerprinting.Node;

public class NodeViewModel extends AndroidViewModel {

    private MutableLiveData<List<Node>> nodes;

    public NodeViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<Node>> getNodes() {
        if (nodes == null) {
            nodes = new MutableLiveData<List<Node>>();
            loadNodes();
        }
        return nodes;
    }


    private void loadNodes() {
        // Do an asynchronous operation to fetch users.
    }
}
