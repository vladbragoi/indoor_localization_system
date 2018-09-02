package it.univr.vlad.fingerprinting.view;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import it.univr.vlad.fingerprinting.MainActivity;
import it.univr.vlad.fingerprinting.Node;
import it.univr.vlad.fingerprinting.R;
import it.univr.vlad.fingerprinting.model.CBLDatabase;
import it.univr.vlad.fingerprinting.mv.MagneticVector;
import it.univr.vlad.fingerprinting.viewmodel.NodeViewModel;

public class LocalizationFragment extends Fragment implements SpeedDialView.OnChangeListener {

    private TextView directionTextView;
    private TextView fingerprintTextView;
    private TextView xTextView;
    private TextView yTextView;
    public AppCompatCheckBox mWifiCheckbox;
    public AppCompatCheckBox mBeaconCheckbox;
    public AppCompatCheckBox mMagneticVectorCheckbox;

    private NodeViewModel mViewModel;

    private CBLDatabase mDatabase;
    private Document mUserDataDocument;
    private Document mUserResultDocument;

    private SpeedDialView mSpeedDialView;

    private boolean running = false;

    private final Observer<List<Node>> mWifiNodesObserver = nodes -> {
        // TODO
    };

    private final Observer<List<Node>> mBeaconNodesObserver = nodes -> {
        // TODO
    };

    private final Observer<MagneticVector> mMagneticVectorObserver = mv -> {
        // TODO
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity activity = getActivity();
        if (activity != null) {
            Application application = (Application) activity.getApplication();
            application.changeSession(Application.Session.ONLINE);
            mDatabase = application.getDatabase();
            mDatabase.setPullFilter("online/resultDoc");

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

        mViewModel.getMv().observe(this,
                magneticVector -> directionTextView.setText(magneticVector != null ?
                                magneticVector.toString() : "NORTH")
        );
    }

    @Override
    public boolean onMainActionSelected() {
        if (!running) {

            showStartLocalizationDialog();

            Drawable drawable = AppCompatResources
                    .getDrawable(Objects.requireNonNull(getContext()), R.drawable.ic_stop_white_24dp);
            mSpeedDialView.setMainFabClosedDrawable(drawable);
            running = true;
        }
        else {
            Drawable drawable = AppCompatResources
                    .getDrawable(Objects.requireNonNull(getContext()), R.drawable.ic_play_arrow_white_24dp);
            mSpeedDialView.setMainFabClosedDrawable(drawable);
            running = false;
        }

        return false; // True to keep the Speed Dial open
    }

    private void showStartLocalizationDialog() {
        Activity activity = getActivity();
        assert activity != null;

        Dialog.showStartLocalizationDialog(activity, dialog -> {
            Button start = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
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

                    // TODO: start

                    dialog.dismiss();
                } else if (errorTextView != null) {
                    // No such user input
                    errorTextView.setVisibility(View.VISIBLE);
                }
            });
        }, null);
    }

    @Override public void onToggleChanged(boolean isOpen) { }


    @Override public void onResume() {
        super.onResume();
        mViewModel.getWifiList().observe(this, mWifiNodesObserver);
        mViewModel.getBeaconList().observe(this, mBeaconNodesObserver);
    }

    @Override public void onStop() {
        mViewModel.getWifiList().removeObserver(mWifiNodesObserver);
        mViewModel.getBeaconList().removeObserver(mBeaconNodesObserver);
        mViewModel.getMv().removeObserver(mMagneticVectorObserver);
        super.onStop();
    }
}
