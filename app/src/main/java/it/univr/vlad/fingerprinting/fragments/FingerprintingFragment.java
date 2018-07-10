package it.univr.vlad.fingerprinting.fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;

import it.univr.vlad.fingerprinting.R;
import it.univr.vlad.fingerprinting.viewmodel.NodeViewModel;

/**
 * Activities that contain this fragment must implement the
 * {@link FingerprintingFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class FingerprintingFragment extends Fragment {

    // private OnFragmentInteractionListener mListener;
    private SpeedDialView mSpeedDialView;

    private String[] devices = new String[]{"wifi", "beacons"};
    private boolean[] devicesChecked = new boolean[]{false, false};

    private NodeViewModel viewModel;

    public FingerprintingFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(this).get(NodeViewModel.class);
        viewModel.getNodes().observe(this, nodes -> {
            // update UI
        });

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fingerprinting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getUserVisibleHint()) {
            mSpeedDialView = view.findViewById(R.id.start_stop_button);
            initSpeedDial(savedInstanceState == null);
        }
        else {
            Log.e(getClass().getName(),"User visibility = " + getUserVisibleHint());
        }
    }

    private void initSpeedDial(boolean addActionItems) {
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

    public boolean closeDial() {
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
    public void onDetach() {
        super.onDetach();
        //mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public class NodeViewHolder extends RecyclerView.ViewHolder {

        private TextView type;
        private TextView ssid;
        private TextView value;

        public NodeViewHolder(View itemView) {
            super(itemView);
            /*type = itemView.findViewById();
            ssid = itemView.findViewById();
            value = itemView.findViewById();*/
        }
    }
}
