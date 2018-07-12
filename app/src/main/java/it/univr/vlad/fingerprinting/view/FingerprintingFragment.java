package it.univr.vlad.fingerprinting.view;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import it.univr.vlad.fingerprinting.Node;
import it.univr.vlad.fingerprinting.R;
import it.univr.vlad.fingerprinting.viewmodel.NodeViewModel;

public class FingerprintingFragment extends Fragment {

    // private OnFragmentInteractionListener mListener;
    private SpeedDialView mSpeedDialView;

    private String[] devices = new String[]{"wifi", "beacons"};
    private boolean[] devicesChecked = new boolean[]{false, false};

    private RecyclerView mRecyclerView;
    private NodeListAdapter mAdapter;
    private NodeViewModel viewModel;

    public FingerprintingFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(this).get(NodeViewModel.class);
        mAdapter = new NodeListAdapter();

        viewModel.getWifiList()
                .observe(this, wifiNodes -> mAdapter.setNodes(wifiNodes));

        viewModel.getBeaconList()
                .observe(this, beaconNodes -> mAdapter.setNodes(beaconNodes));

        viewModel.getMv().observe(this, mv -> mAdapter.setMv(mv));

        Context context = getContext();
        if (context != null) {
            turnWifiOn(context);
            turnBluetoothOn(context);
            turnLocationOn(context);
        }
    }

    private void turnLocationOn(@NotNull Context context) {
        LocationManager locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        if (!isLocationEnabled(locationManager)) {
            final AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setTitle(context.getString(R.string.location_title));
            dialog.setMessage(context.getString(R.string.location_message));
            dialog.setPositiveButton(android.R.string.ok, (dialog1, which) -> context
                    .startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
            dialog.setNegativeButton(android.R.string.no, (dialog2, which) -> dialog2.dismiss());
            dialog.show();
        }
    }

    private boolean isLocationEnabled(@NotNull LocationManager locationManager) {
        boolean gps_enabled;
        boolean network_enabled;

        gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return gps_enabled || network_enabled;
    }

    private void turnBluetoothOn(Context context) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(context,
                        R.string.ble_not_supported,
                        Toast.LENGTH_SHORT).show();
            }
            else {
                /*
                 * Should use this method, but just to uniform the requests
                 * mContext.startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                 */
                final android.app.AlertDialog.Builder dialog =
                        new android.app.AlertDialog.Builder(context);
                dialog.setTitle(context.getString(R.string.bluetooth_title));
                dialog.setMessage(context.getString(R.string.bluetooth_message));
                dialog.setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                    bluetoothAdapter.enable();
                    Toast.makeText(context,
                            context.getString(R.string.bluetooth_enabled),
                            Toast.LENGTH_SHORT).show();
                });
                dialog.setNegativeButton(android.R.string.no, (dialog2, which) -> dialog2.dismiss());
                dialog.setOnCancelListener(diag -> context
                        .startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                );
                dialog.show();
            }
        }
    }

    private void turnWifiOn(@NotNull Context context) {
        WifiManager wifiManager = (WifiManager) context
                .getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        if (wifiManager != null && !wifiManager.isWifiEnabled()) {
            final AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setTitle(context.getString(R.string.wifi_title));
            dialog.setMessage(context.getString(R.string.wifi_message));
            dialog.setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                wifiManager.setWifiEnabled(true);
                Toast.makeText(context,
                        context.getString(R.string.wifi_enabled),
                        Toast.LENGTH_SHORT).show();
            });
            dialog.setNegativeButton(android.R.string.no, (dialog2, which) -> dialog2.dismiss());
            dialog.setOnCancelListener(diag -> context
                    .startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS))
            );
            dialog.show();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fingerprinting, container, false);
        mRecyclerView = rootView.findViewById(R.id.nodesRecyclerView);
        mRecyclerView.setItemAnimator(null);
        mRecyclerView.setAdapter(mAdapter);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getUserVisibleHint()) {
            mSpeedDialView = view.findViewById(R.id.start_stop_button);
            setupSpeedDial(savedInstanceState == null);
        }
        else {
            Log.e(getClass().getName(),"User visibility = " + getUserVisibleHint());
        }
    }

    private void setupSpeedDial(boolean addActionItems) {
        if (addActionItems && getContext() != null) {
            Drawable drawable = AppCompatResources
                    .getDrawable(getContext(), R.drawable.ic_replay_white_24dp);
            mSpeedDialView.addActionItem(
                    new SpeedDialActionItem.Builder(R.id.fab_replay, drawable)
                            .setLabel(getString(R.string.dial_restart))
                            .setTheme(R.style.AppTheme_Fab)
                            .create());

            drawable = AppCompatResources
                    .getDrawable(getContext(), R.drawable.ic_stop_white_24dp);
            mSpeedDialView.addActionItem(
                    new SpeedDialActionItem.Builder(R.id.fab_stop, drawable)
                            .setLabel(getString(R.string.dial_stop))
                            .setTheme(R.style.AppTheme_Fab)
                            .create());

            drawable = AppCompatResources
                    .getDrawable(getContext(), R.drawable.ic_save_white_24dp);
            mSpeedDialView.addActionItem(
                    new SpeedDialActionItem.Builder(R.id.fab_save, drawable)
                            .setLabel(getString(R.string.dial_save))
                            .setTheme(R.style.AppTheme_Fab)
                            .create());
        }

        mSpeedDialView.setOnChangeListener(new SpeedDialView.OnChangeListener() {
            @Override
            public boolean onMainActionSelected() {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Dispositivi");
                builder.setMultiChoiceItems(devices, devicesChecked, null);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            // START COUNTDOWN
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.no, null);
                builder.show();
                return false; // True to keep the Speed Dial open
            }

            @Override
            public void onToggleChanged(boolean isOpen) {}
        });

        mSpeedDialView.setOnActionSelectedListener(actionItem -> {
            Toast.makeText(getContext(), "DialAction", Toast.LENGTH_SHORT).show();
            return false; // True to keep the Speed Dial open
        });

    }

    public boolean closeSpeedDial() {
        //Closes menu if its opened.
        if (mSpeedDialView.isOpen()) {
            mSpeedDialView.close();
            return true;
        }
        return false;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        /*if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }*/
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /*if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }*/
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
