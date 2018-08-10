package it.univr.vlad.fingerprinting.model;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

public class CBLDatabase extends CBLAbstract {

    private final static String THREAD_NAME = "Replication Thread";

    private final HandlerThread thread;
    private final Handler handler;

    private boolean continuous = false;
    private boolean push = false;
    private boolean pull = false;
    private String pullFilter = "";
    private String pushFilter = "";

    public CBLDatabase(Context context, String dbName, String dbUrl) {
        super(context, dbName, dbUrl);
        // Background thread
        thread = new HandlerThread(THREAD_NAME + ": " + dbName);
        thread.start();
        // Handler associated with the background thread
        handler = new Handler(thread.getLooper());
    }

    @Override
    public void start() {
        if (push) handler.post(() -> startPushReplication(continuous, pushFilter));
        if (pull) handler.post(() -> startPullReplication(continuous, pullFilter));
        running = true;
        Log.d(dbName, "DB Started");
    }

    @Override
    public void stop() {
        if (push) handler.post(this::stopPushReplication);
        if (pull) handler.post(this::stopPullReplication);
        running = false;
        Log.d(dbName, "DB Stopped");
    }

    public void setContinuous(boolean continuous) {
        this.continuous = continuous;
    }

    public void setPushReplication(boolean push) {
        this.push = push;
    }

    public void setPullReplication(boolean pull) {
        this.pull = pull;
    }

    public void setPullFilter(String pullFilter) {
        this.pullFilter = pullFilter;
    }

    public void setPushFilter(String pushFilter) {
        this.pushFilter = pushFilter;
    }

    @Override
    public void close() {
        super.close();
        thread.quitSafely();
        Log.d(dbName, "DB Closed");
    }
}
