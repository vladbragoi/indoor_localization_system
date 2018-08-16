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

    private enum Session {OFFLINE, ONLINE}

    public static final String OFF_DB_NAME_KEY = "db_name";
    public static final String OFF_DB_URL_KEY = "db_url";
    public static final String OFF_DB_USER_KEY = "username";
    public static final String OFF_DB_PASSWD_KEY = "password";

    public static final String ON_DB_NAME_KEY = "db_name";
    public static final String ON_DB_URL_KEY = "db_url";
    public static final String ON_DB_USER_KEY = "username";
    public static final String ON_DB_PASSWD_KEY = "password";

    protected SharedPreferences sharedPreferences;
    private Manager manager;
    private CBLDatabase database;
    private String mDatabaseName;
    private String mDatabaseUrl;
    private String mUsername;
    private String mPassword;

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

        // enableLogging();
        if (manager != null) startSession(Session.OFFLINE);
        Log.d(TAG, "APP Started");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // TODO: pay attention here
        switch (key) {
            case OFF_DB_NAME_KEY:
                mDatabaseName = sharedPreferences.getString(key, "");
                database.changeName(manager, mDatabaseName);
                break;
            case OFF_DB_URL_KEY:
                mDatabaseUrl = sharedPreferences.getString(key, "");
                database.changeUrl(mDatabaseUrl);
                break;
            case OFF_DB_USER_KEY:
                mUsername = sharedPreferences.getString(key, "");
                database.updateAuthentication(mUsername, mPassword);
                break;
            case OFF_DB_PASSWD_KEY:
                mPassword = sharedPreferences.getString(key, "");
                database.updateAuthentication(mUsername, mPassword);
                break;
        }
        // TODO: ONLINE db preferences change
    }

    public CBLDatabase getDatabase() {
        return database;
    }

    public void close() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        if (database != null) database.stop();
        if (manager != null) manager.close();
        Log.d(TAG, "APP Stopped");
    }

    private void startSession(Session session) {
        switch (session) {
            case ONLINE:
                loadOnlineDatabasePreferences(); break;
            case OFFLINE:
                loadFingerprintingDbPreferences(); break;
        }
        openDatabase();
    }

    private void openDatabase() {
        this.database = new CBLDatabase(manager, mDatabaseName, mDatabaseUrl, mUsername, mPassword)
                .setPushReplication().setContinuous(true);
    }

    private void changeSession(Session session) {
        database.close();
        if (manager != null) startSession(session);
    }

    private void loadOnlineDatabasePreferences() {
        this.mDatabaseName = sharedPreferences.getString(ON_DB_NAME_KEY, "");
        this.mDatabaseUrl = sharedPreferences.getString(ON_DB_URL_KEY, "");
        this.mUsername = sharedPreferences.getString(ON_DB_USER_KEY, "");
        this.mPassword = sharedPreferences.getString(ON_DB_PASSWD_KEY, "");
    }

    private void loadFingerprintingDbPreferences() {
        this.mDatabaseName = sharedPreferences.getString(OFF_DB_NAME_KEY, "");
        this.mDatabaseUrl =  sharedPreferences.getString(OFF_DB_URL_KEY, "");
        this.mUsername =  sharedPreferences.getString(OFF_DB_USER_KEY, "");
        this.mPassword = sharedPreferences.getString(OFF_DB_PASSWD_KEY, "");
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
