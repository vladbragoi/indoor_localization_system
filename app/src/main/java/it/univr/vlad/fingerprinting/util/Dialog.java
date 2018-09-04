package it.univr.vlad.fingerprinting.util;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.View;

import it.univr.vlad.fingerprinting.R;

public final class Dialog {

    public static void showInfoDialog(Activity activity) {
        View view = View.inflate(activity, R.layout.dialog_info, null);

        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.fingerprinting))
                .setView(view)
                .create()
                .show();
    }

    /**
     * A dialog to ask user which device(s) should be used for "localization"
     * @param activity activity
     * @param showListener Show Listener
     * @param dismissListener Dismiss Listener
     */
    public static void showStartLocalizationDialog(Activity activity,
                                                   DialogInterface.OnShowListener showListener,
                                                   DialogInterface.OnDismissListener dismissListener) {
        AlertDialog dialog = showDialog(
                activity,
                activity.getString(R.string.localization),
                R.layout.dialog_localization_start);

        dialog.setOnShowListener(showListener);
        if (dismissListener != null) dialog.setOnDismissListener(dismissListener);
        dialog.show();
    }

    /**
     * A dialog to ask user which device(s) should be used for "fingerprinting"
     * and how many seconds to scan values
     * @param activity activity
     * @param showListener Show Listener
     * @param dismissListener Dismiss Listener
     */
    public static void showStartDialog(Activity activity, DialogInterface.OnShowListener showListener,
                                       @Nullable  DialogInterface.OnDismissListener dismissListener) {
        AlertDialog dialog = showDialog(
                activity,
                activity.getString(R.string.fingerprinting),
                R.layout.dialog_start);

        dialog.setOnShowListener(showListener);
        if (dismissListener != null) dialog.setOnDismissListener(dismissListener);
        dialog.show();
    }

    /**
     * A dialog for setting the fingerprint's property
     * @param activity activity
     * @param showListener Show Listener
     * @param dismissListener Dismiss Listener
     */
    public static void showSetFingerprintDialog(Activity activity,
                                                DialogInterface.OnShowListener showListener,
                                                @Nullable  DialogInterface.OnDismissListener dismissListener) {
        AlertDialog dialog = showDialog(
                activity,
                activity.getString(R.string.fingerprint),
                R.layout.dialog_set_fingerprint);

        dialog.setOnShowListener(showListener);
        if (dismissListener != null) dialog.setOnDismissListener(dismissListener);
        dialog.show();
    }

    private static AlertDialog showDialog(Activity activity, String title, int layout) {
        View view = View.inflate(activity, layout, null);

        return new AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }
}
