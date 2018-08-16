package it.univr.vlad.fingerprinting.model;

import android.util.Log;
import android.util.Patterns;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.AuthenticatorFactory;
import com.couchbase.lite.replicator.RemoteRequestResponseException;
import com.couchbase.lite.replicator.Replication;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

public class CBLDatabase implements Replication.ChangeListener {

    private String mDatabaseName;
    private String mDatabaseUrl;

    private Database mDatabase;
    private Replication mPushReplication;
    private Replication mPullReplication;
    private Authenticator mAuthenticator;

    private boolean continuous = false;
    private boolean running = false;
    private boolean push = false;
    private boolean pull = false;
    private String pullFilter = "";
    private String pushFilter = "";

    public CBLDatabase(Manager manager, String databaseName, String databaseUrl,
                       String username, String password) {

        this.mDatabaseName = databaseName;
        this.mDatabaseUrl = databaseUrl;

        openDatabase(manager);
        setAuthentication(username, password);
    }

    private void openDatabase(Manager manager) {
        /*DatabaseOptions options = new DatabaseOptions();
        options.setCreate(true);
        options.setEncryptionKey(key);

        try {
            mDatabase = manager.openDatabase(mDatabaseName, options);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        CouchbaseLiteHttpClientFactory clientFactory = new CouchbaseLiteHttpClientFactory(database.getPersistentCookieStore());
        clientFactory.allowSelfSignedSSLCertificates();
        manager.setDefaultHttpClientFactory(clientFactory);*/
        try {
            mDatabase = manager.getDatabase(mDatabaseName);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        if (push) startPushReplication(continuous, pushFilter);
        if (pull) startPullReplication(continuous, pullFilter);
        running = true;
        Log.d(mDatabaseName, "DB Started");
    }

    public void stop() {
        if (push) stopPushReplication();
        if (pull) stopPullReplication();
        running = false;
        Log.d(mDatabaseName, "DB Stopped");
    }

    public CBLDatabase setContinuous(boolean continuous) {
        this.continuous = continuous;
        return this;
    }

    public CBLDatabase setPushReplication() {
        this.push = true;
        return this;
    }

    public CBLDatabase setPullReplication() {
        this.pull = true;
        return this;
    }

    public CBLDatabase setPullFilter(String pullFilter) {
        this.pullFilter = pullFilter;
        return this;
    }

    public CBLDatabase setPushFilter(String pushFilter) {
        this.pushFilter = pushFilter;
        return this;
    }

    private void startPushReplication(boolean continuous, String filter) {
        if (!mDatabase.isOpen()) {
            try {
                mDatabase.open();
            } catch (CouchbaseLiteException e) {
                Log.w(mDatabaseName, "Cannot open database");
            }
        }
        try {
            mPushReplication = mDatabase.createPushReplication(getSyncUrl());
        } catch (MalformedURLException e) {
            Log.e(mDatabaseName, "PUSH: url error");
            return;
        }
        mPushReplication.setAuthenticator(mAuthenticator);
        mPushReplication.setContinuous(continuous);
        if (filter != null && !filter.equals(""))
            mPushReplication.setFilter(filter);

        mPushReplication.addChangeListener(this);
        mPushReplication.start();
    }

    private void startPullReplication(boolean continuous, String filter) {
        if (!mDatabase.isOpen()) {
            try {
                mDatabase.open();
            } catch (CouchbaseLiteException e) {
                Log.w(mDatabaseName, "Cannot open database");
            }
        }
        try {
            mPullReplication = mDatabase.createPullReplication(getSyncUrl());
        } catch (MalformedURLException e) {
            Log.e(mDatabaseName, "PULL: url error");
            return;
        }
        mPullReplication.setAuthenticator(mAuthenticator);
        mPullReplication.setContinuous(continuous);
        if (filter != null && !filter.equals(""))
            mPullReplication.setFilter(filter);

        mPullReplication.addChangeListener(this);
        mPullReplication.start();
    }

    private void stopPushReplication() {
        if (mPushReplication != null) {
            mPushReplication.stop();
            mPushReplication.removeChangeListener(this);
        }
    }

    private void stopPullReplication() {
        if (mPullReplication != null) {
            mPullReplication.stop();
            mPullReplication.removeChangeListener(this);
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
                        Log.e(mDatabase.getName(), "Authentication failed");
                        break;
                    case 400:
                        Log.e(mDatabase.getName(), "Bad request");
                        break;
                    case 404:
                        Log.e(mDatabase.getName(), "Not found");
                        break;
                    default:
                        Log.e(mDatabase.getName(), "Code error: " + exception.getCode());
                }
            }
        }
    }

    private URL getSyncUrl() throws MalformedURLException {
        String url = mDatabaseUrl + mDatabaseName;
        if (Patterns.WEB_URL.matcher(url).matches()) {
            return new URL(url);
        } else
            throw new MalformedURLException();
    }

    private void setAuthentication(String username, String password) {
        mAuthenticator = AuthenticatorFactory.createBasicAuthenticator(username, password);
    }

    @Override public String toString() {
        return mDatabaseUrl + mDatabaseName;
    }

    public void updateAuthentication(String username, String password) {
        boolean wasRunning = running;
        if (running) stop();

        setAuthentication(username, password);

        if (push) {
            mPushReplication.clearAuthenticationStores();
            mPushReplication.setAuthenticator(mAuthenticator);
        }

        if (pull) {
            mPullReplication.clearAuthenticationStores();
            mPullReplication.setAuthenticator(mAuthenticator);
        }

        if (wasRunning) start();
        Log.d(mDatabaseName, "DB Authentication credentials updated");
    }

    public void changeName(Manager manager, String databaseName) {
        boolean wasRunning = running;
        if (running) stop();
        mDatabase.close();

        this.mDatabaseName = databaseName;
        openDatabase(manager);

        if (wasRunning) start();
        Log.d(mDatabaseName, "DB name changed");
    }

    public void changeUrl(String databaseUrl) {
        boolean wasRunning = running;
        if (running) stop();
        mDatabase.close();

        this.mDatabaseUrl = databaseUrl;

        if (wasRunning) start();
        Log.d(mDatabaseName, "DB url changed");
    }

    public void close() {
        stop();
        mDatabase.close();
    }

    public boolean isRunning() {
        return running;
    }

    public Document getDocument(String docName) {
        return mDatabase.getDocument(docName);
    }

    public Database unwrapDatabase() {
        return mDatabase;
    }
}
