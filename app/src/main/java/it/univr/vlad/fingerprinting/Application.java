package it.univr.vlad.fingerprinting;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;

import java.io.IOException;

import it.univr.vlad.fingerprinting.model.CBLDatabase;

public class Application extends android.app.Application
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "Fingerprint";

    public static final String FING_DB_NAME_KEY = "fing_name";
    public static final String FING_DB_URL_KEY = "fing_url";
    public static final String FING_DB_USER_KEY = "fing_username";
    public static final String FING_DB_PASSWD_KEY = "fing_password";

    public static final String LOC_DB_NAME_KEY = "loc_name";
    public static final String LOC_DB_URL_KEY = "loc_url";
    public static final String LOC_DB_USER_KEY = "loc_username";
    public static final String LOC_DB_PASSWD_KEY = "loc_password";

    protected SharedPreferences sharedPreferences;
    private Manager manager;
    private CBLDatabase fingerprintingDatabase;
    private CBLDatabase localizationDatabase;
    private DatabaseListener mDatabaseListener;

    private String mFingDbName;
    private String mFingDbUrl;
    private String mFingUsername;
    private String mFingPassword;

    private String mLocDbName;
    private String mLocDbUrl;
    private String mLocUsername;
    private String mLocPassword;

    ///////////////////////////////////////////////
    //            PUBLIC METHODS
    ///////////////////////////////////////////////

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            manager = new Manager(new AndroidContext(getApplicationContext()), Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        refresh();

        // enableLogging();
        Log.d(TAG, "APP Started");
    }

    public void refresh() {
        loadPreferences();

        initFingerprintingDatabase();
        initLocalizationDatabase();

        if (fingerprintingDatabase != null || localizationDatabase != null) {
            if (mDatabaseListener != null) mDatabaseListener.onDatabaseCreatedListener();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case FING_DB_NAME_KEY:
                mFingDbName = sharedPreferences.getString(key, "");

                if (fingerprintingDatabase == null) initFingerprintingDatabase();
                else fingerprintingDatabase.changeName(manager, mFingDbName);

                break;
            case FING_DB_URL_KEY:
                mFingDbUrl= sharedPreferences.getString(key, "");

                if (fingerprintingDatabase == null) initFingerprintingDatabase();
                else fingerprintingDatabase.changeUrl(mFingDbUrl);

                break;
            case FING_DB_USER_KEY:
                mFingUsername = sharedPreferences.getString(key, "");

                if (fingerprintingDatabase == null) initFingerprintingDatabase();
                else fingerprintingDatabase.updateAuthentication(mFingUsername, mFingPassword);

                break;
            case FING_DB_PASSWD_KEY:
                mFingPassword = sharedPreferences.getString(key, "");

                if (fingerprintingDatabase == null) initFingerprintingDatabase();
                else fingerprintingDatabase.updateAuthentication(mFingUsername, mFingPassword);

                break;
            case LOC_DB_NAME_KEY:
                mLocDbName = sharedPreferences.getString(key, "");

                if (localizationDatabase == null) initLocalizationDatabase();
                else localizationDatabase.changeName(manager, mLocDbName);

                break;
            case LOC_DB_URL_KEY:
                mLocDbUrl= sharedPreferences.getString(key, "");

                if (localizationDatabase == null) initLocalizationDatabase();
                else localizationDatabase.changeUrl(mLocDbUrl);

                break;
            case LOC_DB_USER_KEY:
                mLocUsername = sharedPreferences.getString(key, "");

                if (localizationDatabase == null) initLocalizationDatabase();
                else localizationDatabase.updateAuthentication(mLocUsername, mLocPassword);

                break;
            case LOC_DB_PASSWD_KEY:
                mLocPassword = sharedPreferences.getString(key, "");

                if (localizationDatabase == null) initLocalizationDatabase();
                else localizationDatabase.updateAuthentication(mLocUsername, mLocPassword);

                break;
        }
    }

    public void setOnDatabaseListener(DatabaseListener databaseListener) {
        this.mDatabaseListener = databaseListener;
    }

    public CBLDatabase getFingerprintingDatabase() {
        if (fingerprintingDatabase == null || !fingerprintingDatabase.isOpen()) {
            initFingerprintingDatabase();
        }
        return fingerprintingDatabase;
    }

    public CBLDatabase getLocalizationDatabase() {
        if (localizationDatabase == null || !localizationDatabase.isOpen()) {
            initLocalizationDatabase();
        }
        return localizationDatabase;
    }

    public void close() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        if (localizationDatabase.isOpen()) localizationDatabase.close();
        if (fingerprintingDatabase.isOpen()) fingerprintingDatabase.close();
        if (manager != null) manager.close();
        Log.d(TAG, "APP Stopped");
    }

    ///////////////////////////////////////////////
    //            PRIVATE METHODS
    ///////////////////////////////////////////////

    private void initFingerprintingDatabase() {
        if (fingerprintingDatabase == null
                && !TextUtils.isEmpty(mFingDbName)
                && !TextUtils.isEmpty(mFingDbUrl)) {
            fingerprintingDatabase = new CBLDatabase(
                    manager,
                    mFingDbName,
                    mFingDbUrl,
                    mFingUsername,
                    mFingPassword);
        }
    }

    private void initLocalizationDatabase() {
        if (localizationDatabase == null
                && !TextUtils.isEmpty(mLocDbName)
                && !TextUtils.isEmpty(mLocDbUrl)) {
            localizationDatabase = new CBLDatabase(
                    manager,
                    mLocDbName,
                    mLocDbUrl,
                    mLocUsername,
                    mLocPassword);
        }
    }

    private void loadPreferences() {
        // Fingerprinting Database
        this.mFingDbName = sharedPreferences.getString(FING_DB_NAME_KEY, "");
        this.mFingDbUrl =  sharedPreferences.getString(FING_DB_URL_KEY, "");
        this.mFingUsername =  sharedPreferences.getString(FING_DB_USER_KEY, "");
        this.mFingPassword = sharedPreferences.getString(FING_DB_PASSWD_KEY, "");

        // Localization Database
        this.mLocDbName = sharedPreferences.getString(LOC_DB_NAME_KEY, "");
        this.mLocDbUrl = sharedPreferences.getString(LOC_DB_URL_KEY, "");
        this.mLocUsername = sharedPreferences.getString(LOC_DB_USER_KEY, "");
        this.mLocPassword = sharedPreferences.getString(LOC_DB_PASSWD_KEY, "");
    }

    private void enableLogging() {
        com.couchbase.lite.Manager.enableLogging(TAG, Log.VERBOSE);
        com.couchbase.lite.Manager.enableLogging(com.couchbase.lite.util.Log.TAG, Log.VERBOSE);
        com.couchbase.lite.Manager.enableLogging(com.couchbase.lite.util.Log.TAG_SYNC_ASYNC_TASK, Log.VERBOSE);
        com.couchbase.lite.Manager.enableLogging(com.couchbase.lite.util.Log.TAG_SYNC, Log.VERBOSE);
        com.couchbase.lite.Manager.enableLogging(com.couchbase.lite.util.Log.TAG_QUERY, Log.VERBOSE);
        com.couchbase.lite.Manager.enableLogging(com.couchbase.lite.util.Log.TAG_VIEW, Log.VERBOSE);
        Manager.enableLogging(com.couchbase.lite.util.Log.TAG_DATABASE, Log.VERBOSE);
    }

    public interface DatabaseListener {
        void onDatabaseCreatedListener();
    }
}
