package it.univr.vlad.fingerprinting;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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

    private static final String DEFAULT_DB_NAME = "database";
    private static final String DEFAULT_DB_URL = "http://192.168.1.1:5984/";
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin";

    protected SharedPreferences sharedPreferences;
    private Manager manager;
    private CBLDatabase fingerprintingDatabase;
    private CBLDatabase localizationDatabase;

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

        loadPreferences();

        initFingerprintingDatabase();
        initLocalizationDatabase();

        // enableLogging();
        Log.d(TAG, "APP Started");
    }

    public void refresh() {

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case FING_DB_NAME_KEY:
                mFingDbName = sharedPreferences.getString(key, "");
                fingerprintingDatabase.changeName(mFingDbName);
                break;
            case FING_DB_URL_KEY:
                mFingDbUrl= sharedPreferences.getString(key, "");
                fingerprintingDatabase.changeUrl(mFingDbUrl);
                break;
            case FING_DB_USER_KEY:
                mFingUsername = sharedPreferences.getString(key, "");
                fingerprintingDatabase.updateAuthentication(mFingUsername, mFingPassword);
                break;
            case FING_DB_PASSWD_KEY:
                mFingPassword = sharedPreferences.getString(key, "");
                fingerprintingDatabase.updateAuthentication(mFingUsername, mFingPassword);
                break;
            case LOC_DB_NAME_KEY:
                mLocDbName = sharedPreferences.getString(key, "");
                localizationDatabase.changeName(mLocDbName);
                break;
            case LOC_DB_URL_KEY:
                mLocDbUrl= sharedPreferences.getString(key, "");
                localizationDatabase.changeUrl(mLocDbUrl);
                break;
            case LOC_DB_USER_KEY:
                mLocUsername = sharedPreferences.getString(key, "");
                localizationDatabase.updateAuthentication(mLocUsername, mLocPassword);
                break;
            case LOC_DB_PASSWD_KEY:
                mLocPassword = sharedPreferences.getString(key, "");
                localizationDatabase.updateAuthentication(mLocUsername, mLocPassword);
                break;
        }
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
        if (fingerprintingDatabase == null) {
            fingerprintingDatabase = new CBLDatabase(
                    manager,
                    mFingDbName,
                    mFingDbUrl,
                    mFingUsername,
                    mFingPassword);
        }
    }

    private void initLocalizationDatabase() {
        if (localizationDatabase == null) {
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
        this.mFingDbName = sharedPreferences.getString(FING_DB_NAME_KEY, DEFAULT_DB_NAME);
        this.mFingDbUrl =  sharedPreferences.getString(FING_DB_URL_KEY, DEFAULT_DB_URL);
        this.mFingUsername =  sharedPreferences.getString(FING_DB_USER_KEY, DEFAULT_USERNAME);
        this.mFingPassword = sharedPreferences.getString(FING_DB_PASSWD_KEY, DEFAULT_PASSWORD);

        // Localization Database
        this.mLocDbName = sharedPreferences.getString(LOC_DB_NAME_KEY, DEFAULT_DB_NAME);
        this.mLocDbUrl = sharedPreferences.getString(LOC_DB_URL_KEY, DEFAULT_DB_URL);
        this.mLocUsername = sharedPreferences.getString(LOC_DB_USER_KEY, DEFAULT_USERNAME);
        this.mLocPassword = sharedPreferences.getString(LOC_DB_PASSWD_KEY, DEFAULT_PASSWORD);
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
}
