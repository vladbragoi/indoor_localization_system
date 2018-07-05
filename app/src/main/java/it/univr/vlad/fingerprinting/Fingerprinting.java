package it.univr.vlad.fingerprinting;

import android.util.Log;

import java.util.List;

public class Fingerprinting {

    Observer observer = new Observer() {
        @Override
        public void update(float[] mv) {

        }

        @Override
        public void update(List results) {
            Log.d("LISTA", results.toString());
        }
    };
}
