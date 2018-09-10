package it.univr.vlad.fingerprinting.view;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.couchbase.lite.Document;
import com.leinardi.android.speeddial.SpeedDialView;

import java.util.List;
import java.util.Objects;

import it.univr.vlad.fingerprinting.Application;
import it.univr.vlad.fingerprinting.Location;
import it.univr.vlad.fingerprinting.activity.MainActivity;
import it.univr.vlad.fingerprinting.templates.Node;
import it.univr.vlad.fingerprinting.R;
import it.univr.vlad.fingerprinting.model.CBLDatabase;
import it.univr.vlad.fingerprinting.devices.mv.MagneticVector;
import it.univr.vlad.fingerprinting.util.Dialog;
import it.univr.vlad.fingerprinting.viewmodel.NodeViewModel;

public class LocalizationFragment extends Fragment implements SpeedDialView.OnChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener, Document.ChangeListener {

    private final static String USERNAME_KEY = "username";
    private final static String FINGERPRINT_KEY = "fingerprint";
    private final static String X_KEY = "x";
    private final static String Y_KEY = "y";

    private TextView directionTextView;
    private TextView fingerprintTextView;
    private TextView xTextView;
    private TextView yTextView;
    public AppCompatCheckBox mWifiCheckbox;
    public AppCompatCheckBox mBeaconCheckbox;
    public AppCompatCheckBox mMagneticVectorCheckbox;

    private SharedPreferences mSharedPreferences;

    private NodeViewModel mViewModel;

    private CBLDatabase mDatabase;
    private Document mResultDocument;
    private Location mLocation;
    private String user;

    private SpeedDialView mSpeedDialView;

    private boolean running = false;

    private final Observer<List<Node>> mWifiNodesObserver = nodes -> {
        if (mLocation != null) {
            mLocation.addWifiNodes(nodes);
            mLocation.saveInto(mDatabase.unwrapDatabase());
            mDatabase.startPushReplication(false);
        }
    };

    private final Observer<List<Node>> mBeaconNodesObserver = nodes -> {
        if (mLocation != null) mLocation.addBeaconNodes(nodes);
    };

    private final Observer<MagneticVector> mMagneticVectorObserver = mv -> {
        if (mLocation != null) mLocation.addMagneticVector(mv);
    };


    ///////////////////////////////////////////////
    //            PUBLIC METHODS
    ///////////////////////////////////////////////

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        Activity activity = getActivity();
        if (activity != null) {
            Application application = (Application) activity.getApplication();
            user = mSharedPreferences.getString(USERNAME_KEY, "admin");
            mLocation = new Location(user);
            mDatabase = application.getLocalizationDatabase(); //.setPullFilter("online/resultDoc");
            mResultDocument = mDatabase.getDocument(user + "_result");

            if (savedInstanceState == null && activity instanceof MainActivity) {
                ((MainActivity) activity).turnLocationOn();
            }
        }

        mViewModel = ViewModelProviders.of(this).get(NodeViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_localization, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        directionTextView = view.findViewById(R.id.directionValue);
        fingerprintTextView = view.findViewById(R.id.fingerprintValue);
        xTextView = view.findViewById(R.id.xValue);
        yTextView = view.findViewById(R.id.yValue);
        mSpeedDialView = view.findViewById(R.id.start_stop_button);

        // Start button listener
        mSpeedDialView.setOnChangeListener(this);

        mViewModel.getMv().observe(this, magneticVector ->
                directionTextView.setText(
                        magneticVector != null ? magneticVector.toString() : "NORTH"
                )
        );
    }

    @Override
    public boolean onMainActionSelected() {
        // Click action on SpeedDial Button
        if (!running) showStartLocalizationDialog();
        else stop();
        return false; // True to keep the Speed Dial open
    }

    @Override
    public void onToggleChanged(boolean isOpen) { }

    @Override
    public void onStart() {
        super.onStart();
        if (!mDatabase.isRunning()) mDatabase.startPullReplication(true);
        mResultDocument.addChangeListener(this);
        mViewModel.getWifiList().observe(this, mWifiNodesObserver);
        mViewModel.getBeaconList().observe(this, mBeaconNodesObserver);
    }

    @Override
    public void onStop() {
        stop();
        if (mDatabase.isRunning()) mDatabase.stop();
        mViewModel.getWifiList().removeObserver(mWifiNodesObserver);
        mViewModel.getBeaconList().removeObserver(mBeaconNodesObserver);
        mViewModel.getMv().removeObserver(mMagneticVectorObserver);
        mResultDocument.removeChangeListener(this);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(USERNAME_KEY)) {
            user = sharedPreferences.getString(key, "");
            mLocation = new Location(user);
            mResultDocument = mDatabase.getDocument(user + "_result");
        }
    }

    @Override
    public void changed(Document.ChangeEvent event) {
        if (this.isVisible() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Document document = event.getSource();
                Object property = document.getProperty(FINGERPRINT_KEY);
                if (property != null) fingerprintTextView.setText(property.toString());
                property = document.getProperty(X_KEY);
                if (property != null) xTextView.setText(property.toString());
                property = document.getProperty(Y_KEY);
                if (property != null) yTextView.setText(property.toString());
            });
        }
    }

    ///////////////////////////////////////////////
    //            PRIVATE METHODS
    ///////////////////////////////////////////////

    private void start() {
        mLocation.setDirection(directionTextView.getText().toString());
        if (mWifiCheckbox != null && mWifiCheckbox.isChecked())
            mViewModel.startWifiScanning();
        if (mBeaconCheckbox != null && mBeaconCheckbox.isChecked())
            mViewModel.startBeaconsScanning();
        if (mMagneticVectorCheckbox != null && mMagneticVectorCheckbox.isChecked())
            mViewModel.getMv().observe(this, mMagneticVectorObserver);

        changeSpeedDialIcon(R.drawable.ic_stop_white_24dp);
        running = true;
    }

    private void stop() {
        if (mWifiCheckbox != null && mWifiCheckbox.isChecked())
            mViewModel.stopWifiScanning();
        if (mBeaconCheckbox != null && mBeaconCheckbox.isChecked())
            mViewModel.stopBeaconsScanning();
        if (mMagneticVectorCheckbox != null && mMagneticVectorCheckbox.isChecked())
            mViewModel.getMv().removeObserver(mMagneticVectorObserver);

        changeSpeedDialIcon(R.drawable.ic_play_arrow_white_24dp);
        running = false;
    }

    private void changeSpeedDialIcon(int icon) {
        Drawable drawable = AppCompatResources
                .getDrawable(Objects.requireNonNull(getContext()), icon);
        mSpeedDialView.setMainFabClosedDrawable(drawable);
    }

    private void showStartLocalizationDialog() {
        Activity activity = getActivity();
        assert activity != null;

        Dialog.showStartLocalizationDialog(activity, dialog -> {
            Button start = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
            Button cancel = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
            TextView errorTextView = ((AlertDialog) dialog).findViewById(R.id.errorMessage);

            mWifiCheckbox = ((AlertDialog) dialog).findViewById(R.id.wifi);
            mBeaconCheckbox = ((AlertDialog) dialog).findViewById(R.id.beacons);
            mMagneticVectorCheckbox = ((AlertDialog) dialog).findViewById(R.id.magneticVector);

            mWifiCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && activity instanceof MainActivity) {
                    ((MainActivity) activity).turnWifiOn(this);
                }
            });
            mBeaconCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && activity instanceof MainActivity) {
                    ((MainActivity) activity).turnBluetoothOn(this);
                }
            });

            start.setOnClickListener(v -> {
                // Check user data input
                if (mWifiCheckbox.isChecked() || mBeaconCheckbox.isChecked()) {

                    start();

                    dialog.dismiss();
                } else if (errorTextView != null) {
                    // No such user input
                    errorTextView.setVisibility(View.VISIBLE);
                }
            });

            cancel.setOnClickListener(v -> stop());

        }, null);
    }
}
