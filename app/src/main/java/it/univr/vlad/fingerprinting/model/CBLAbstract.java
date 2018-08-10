package it.univr.vlad.fingerprinting.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Patterns;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.AuthenticatorFactory;
import com.couchbase.lite.replicator.RemoteRequestResponseException;
import com.couchbase.lite.replicator.Replication;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

public abstract class CBLAbstract implements Replication.ChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String DB_NAME_KEY = "db_name";
    public static final String DB_URL_KEY = "db_url";
    public static final String DB_USER_KEY = "username";
    public static final String DB_PASSWD_KEY = "password";

    private AndroidContext context;
    private Manager manager;
    private com.couchbase.lite.Database database;
    private SharedPreferences sharedPreferences;

    private Replication pushReplication;
    private Replication pullReplication;

    protected String dbName;
    protected String dbUrl;

    protected boolean running = false;

    public CBLAbstract(Context context, String dbNameKey, String dbUrlKey) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        this.context = new AndroidContext(context);
        this.manager = getManager();
        this.dbName = sharedPreferences.getString(dbNameKey, "");
        this.dbUrl =  sharedPreferences.getString(dbUrlKey, "");
        this.database = openDatabase();

        // CouchbaseLiteHttpClientFactory clientFactory = new CouchbaseLiteHttpClientFactory(database.getPersistentCookieStore());
        // clientFactory.allowSelfSignedSSLCertificates();
        // manager.setDefaultHttpClientFactory(clientFactory);
    }

    private Database openDatabase() {
        try {
            return manager.getDatabase(dbName);
        } catch (CouchbaseLiteException e) {
            Log.e(dbName, "Cannot get " + dbName, e);
        }
        return null;
    }

    public abstract void start();

    public abstract void stop();

    public void close() {
        stopPushReplication();
        stopPullReplication();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        if (database != null) database.close();
        if (manager != null) manager.close();
    }

    private void restart(String name, String url) {
        boolean wasRunning = running;
        if (running) stop();
        this.dbName = name;
        this.dbUrl = url;
        database = openDatabase();
        Log.d(dbName, "DB changed: " + dbName + " URL: " + this.dbUrl);
        if (wasRunning) start();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case DB_NAME_KEY:
                // TODO: Close/Restart
                String name = sharedPreferences.getString(key, "");
                restart(name, dbUrl);
                break;
            case DB_URL_KEY:
                // TODO: Close/Restart
                String url = sharedPreferences.getString(key, "");
                restart(dbName, url);
                break;
            case DB_USER_KEY:
            case DB_PASSWD_KEY:
                if (pullReplication != null) updateAuthenticator(pullReplication);
                if (pushReplication != null) updateAuthenticator(pushReplication);
                break;
        }
    }

    private void updateAuthenticator(Replication replication) {
        boolean wasRunning = running;
        if (running) stop();
        replication.clearAuthenticationStores();
        replication.setAuthenticator(getAuth());
        if (wasRunning) start();
        Log.d(dbName, "DB Authentication credentials updated");
    }

    void startPushReplication(boolean continuous, String filter) {
        if (pushReplication == null) {
            try {
                pushReplication = database.createPushReplication(getSyncUrl());
            } catch (MalformedURLException e) {
                Log.e(dbName, "PUSH: url error");
                return;
            }
            pushReplication.setAuthenticator(getAuth());
            pushReplication.setContinuous(continuous);
            if (filter != null && !filter.equals(""))
                pushReplication.setFilter(filter);
        }

        pushReplication.addChangeListener(this);
        if (!database.isOpen()) {
            try {
                database.open();
            } catch (CouchbaseLiteException e) {
                Log.w(dbName, "Cannot open database");
            }
        }
        pushReplication.start();
    }

    void startPullReplication(boolean continuous, String filter) {
        if (pullReplication == null) {
            try {
                pullReplication = database.createPullReplication(getSyncUrl());
            } catch (MalformedURLException e) {
                Log.e(dbName, "PULL: url error");
                return;
            }
            pullReplication.setAuthenticator(getAuth());
            pullReplication.setContinuous(continuous);
            if (filter != null && !filter.equals(""))
                pullReplication.setFilter(filter);
        }

        pullReplication.addChangeListener(this);
        if (!database.isOpen()) {
            try {
                database.open();
            } catch (CouchbaseLiteException e) {
                Log.w(dbName, "Cannot open database");
            }
        }
        pullReplication.start();
    }

    void stopPushReplication() {
        if (pushReplication != null) {
            pushReplication.stop();
            pushReplication.removeChangeListener(this);
        }
    }

    void stopPullReplication() {
        if (pullReplication != null) {
            pullReplication.stop();
            pullReplication.removeChangeListener(this);
        }
    }

    @Override
    public void changed(Replication.ChangeEvent event) {
        if (event.getError() != null) {
            Throwable lastError = event.getError();
            if (lastError instanceof UnknownHostException) stop();
            if (lastError instanceof RemoteRequestResponseException) {
                RemoteRequestResponseException exception = (RemoteRequestResponseException) lastError;
                switch (exception.getCode()) {
                    case 401:
                        Log.e(database.getName(), "Authentication failed");
                        break;
                    case 400:
                        Log.e(database.getName(), "Bad request");
                        break;
                    case 404:
                        Log.e(database.getName(), "Not found");
                        break;
                    default:
                        Log.e(database.getName(), "Code error: " + exception.getCode());
                }
            }
        }
    }

    private Manager getManager() {
        if (manager == null) {
            try {
                manager = new Manager(context, Manager.DEFAULT_OPTIONS);
            } catch (Exception e) {
                Log.e("MANAGER", "Cannot create Manager object", e);
            }
        }

        /*Manager.enableLogging(Log.TAG, Log.WARN);
        Manager.enableLogging(Log.TAG, Log.VERBOSE);
        Manager.enableLogging(Log.TAG_ROUTER, Log.VERBOSE);
        Manager.enableLogging(Log.TAG_VIEW, Log.VERBOSE);
        Manager.enableLogging(Log.TAG_QUERY, Log.VERBOSE);
        Manager.enableLogging(Log.TAG_MULTI_STREAM_WRITER, Log.VERBOSE);
        Manager.enableLogging(Log.TAG_LISTENER, Log.VERBOSE);
        Manager.enableLogging(Log.TAG_BLOB_STORE, Log.VERBOSE);
        Manager.enableLogging(Log.TAG_SYNC_ASYNC_TASK, Log.VERBOSE);
        Manager.enableLogging(Log.TAG_SYNC, Log.VERBOSE);
        Manager.enableLogging(Log.TAG_BATCHER, Log.VERBOSE);
        Manager.enableLogging(Log.TAG_CHANGE_TRACKER, Log.VERBOSE);
        Manager.enableLogging(Log.TAG_REMOTE_REQUEST, Log.VERBOSE);
        Manager.enableLogging(Log.TAG_DATABASE, Log.VERBOSE);*/

        return manager;
    }

    private URL getSyncUrl() throws MalformedURLException {
        if (dbUrl == null || dbName == null) {
            dbUrl = sharedPreferences.getString("db_url", "");
            dbName = sharedPreferences.getString("db_name", "");
        }
        String url = dbUrl + dbName;
        if (Patterns.WEB_URL.matcher(url).matches())
            return new URL(dbUrl + dbName);
        else throw new MalformedURLException();
    }

    public Document getDocument(String docName) {
        return database.getDocument(docName);
    }

    private Authenticator getAuth() {
        String username = sharedPreferences.getString(DB_USER_KEY, "");
        String password = sharedPreferences.getString(DB_PASSWD_KEY, "");
        return AuthenticatorFactory.createBasicAuthenticator(username, password);
    }

    @Override public String toString() {
        return this.dbName;
    }

    public boolean isRunning() {
        return running;
    }
}
