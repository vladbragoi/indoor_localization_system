package it.univr.vlad.fingerprinting;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TimerViewModel extends ViewModel{

    private static final String TWO_DIGITS = "%02d";
    private static final long PERIOD_UNIT = 1L; // unit in seconds

    private MutableLiveData<Boolean> stopResult = new MutableLiveData<>();
    private MutableLiveData<Integer> elapsedTimeLiveData = new MutableLiveData<>();

    private Integer startTime;

    private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> task;

    // private static final int INTERVAL_IN_MILLISECONDS = 1000;
    // private Handler handler;

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            Integer elapsedTime = elapsedTimeLiveData.getValue();
            if (elapsedTime != null) {
                elapsedTime++;
                elapsedTimeLiveData.postValue(elapsedTime);
                if (elapsedTime >= startTime) {
                    stopResult.postValue(true);
                    stop();
                }
            }
            // handler.postDelayed(this, INTERVAL_IN_MILLISECONDS);
        }
    };

    public TimerViewModel() {
        elapsedTimeLiveData.setValue(0);

        // HandlerThread handlerThread = new HandlerThread("TimerThread");
        // handlerThread.start();
        // handler = new Handler(handlerThread.getLooper());
    }

    /**
     *
     * @param time time in seconds
     */
    public void startCountFrom(int time) {
        startTime = time;
        stopResult.setValue(false);
        elapsedTimeLiveData.setValue(0);
        task = service.scheduleAtFixedRate(mRunnable, 0, PERIOD_UNIT, TimeUnit.SECONDS);
        // handler.postDelayed(mRunnable, INTERVAL_IN_MILLISECONDS);
    }

    public void stop() {
        task.cancel(true);
        // handler.removeCallbacks(mRunnable);
    }

    public String getSeconds() {
        Integer elapsedTime = elapsedTimeLiveData.getValue();
        Log.d("Timer", "" + elapsedTime);

        if (elapsedTime != null && startTime != null) {
            int time = startTime - elapsedTime;
            if (time > 0 && time >= 60)
                return String.format(Locale.getDefault(), TWO_DIGITS, time - 60);
            else if (time > 0)
                return String.format(Locale.getDefault(), TWO_DIGITS, time);
        }
        return "00";
    }

    public String getMinutes() {
        Integer elapsedTime = elapsedTimeLiveData.getValue();
        if (elapsedTime != null && startTime != null) {
            int time = startTime - elapsedTime;
            if (time >= 60)
                return String.format(Locale.getDefault(), TWO_DIGITS, time/60);
        }
        return "00";
    }

    public LiveData<Integer> getTime() {
        return elapsedTimeLiveData;
    }

    public LiveData<Boolean> onStop() {
        return stopResult;
    }

    public boolean isStopped() {
        return stopResult.getValue() != null && stopResult.getValue();
    }
}