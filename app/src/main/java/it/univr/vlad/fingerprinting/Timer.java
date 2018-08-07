package it.univr.vlad.fingerprinting;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Timer {
    private final static int NUMBER_OF_THREADS = 1;
    private final static String ONE_DIGIT = "%d";
    private final static String TWO_DIGITS = "%02d";
    private final static long PERIOD_UNIT = 1L; // unit in seconds

    private TimerListener listener;

    public enum TimerStatus {STOPPED, RUNNING}
    private TimerStatus statusResult = TimerStatus.STOPPED;

    private long elapsedTime = 0L;
    private long startTime = 0L;

    private ScheduledExecutorService service;
    private ScheduledFuture<?> task;

    private Handler handler;

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("Timer",
                    "elapsed = " + elapsedTime + " remaining = " + (startTime - elapsedTime));
            elapsedTime++;

            if (elapsedTime >= startTime) {
                statusResult = TimerStatus.STOPPED;
                stop();
            }
            handler.post(() -> onTimeChanged());
        }
    };

    public Timer() {
        service = Executors.newScheduledThreadPool(NUMBER_OF_THREADS);
        handler = new Handler(Looper.getMainLooper());
    }

    /**
     *
     * @param time time in seconds
     */
    public void startCountFrom(long time) {
        elapsedTime = 0L;
        startTime = time;
        statusResult = TimerStatus.RUNNING;
        task = service.scheduleAtFixedRate(mRunnable, 0, PERIOD_UNIT, TimeUnit.SECONDS);
    }

    private void onTimeChanged() {
        if (listener != null) {
            listener.onTimeChanged(getHours(), getMinutes(), getSeconds());
            listener.onTimerStopped(statusResult);
        }
    }

    public void stop() {
        statusResult = TimerStatus.STOPPED;
        task.cancel(true);
    }

    public String getHours() {
        long time = startTime - elapsedTime;
        return String.format(Locale.getDefault(), ONE_DIGIT, time / 3600);
    }

    public String getSeconds() {
        long time = startTime - elapsedTime;
        return String.format(Locale.getDefault(), TWO_DIGITS, time % 60);
    }

    public String getMinutes() {
        long time = startTime - elapsedTime;
        return String.format(Locale.getDefault(), TWO_DIGITS, (time % 3600) / 60);
    }

    public boolean isRunning() {
        return statusResult.equals(TimerStatus.RUNNING);
    }

    public void destroy() {
        service.shutdown();
    }

    public void setTimerListener(TimerListener listener) {
        this.listener = listener;
    }

    public interface TimerListener {
        void onTimeChanged(String hours, String minutes, String seconds);
        void onTimerStopped(TimerStatus status);
    }
}

/* IMPLEMENTATION WITH A VIEW-MODEL

Register for changes:
    mTimerViewModel = ViewModelProviders.of(this).get(TimerViewModel.class);
    mTimerViewModel.getTime()
            .observe(this, t -> mMinutes.setText(mTimerViewModel.getMinutes()));
    mTimerViewModel.getTime()
            .observe(this, t -> mSeconds.setText(mTimerViewModel.getSeconds()));

    mTimerViewModel.onStop().observe(this, timerStopped -> {
        if (timerStopped != null && timerStopped) stop();
    });

Start timer:
    mTimerViewModel.startCountFrom(duration);

public class TimerViewModel extends ViewModel{

    private static final String TWO_DIGITS = "%02d";
    private static final long PERIOD_UNIT = 1L; // unit in seconds

    private MutableLiveData<Boolean> statusResult = new MutableLiveData<>();
    private MutableLiveData<Integer> elapsedTimeLiveData = new MutableLiveData<>();

    private Integer startTime;

    private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> task;

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            Integer elapsedTime = elapsedTimeLiveData.getValue();
            if (elapsedTime != null) {
                elapsedTime++;
                elapsedTimeLiveData.postValue(elapsedTime);
                if (elapsedTime >= startTime) {
                    statusResult.postValue(true);
                    stop();
                }
            }
        }
    };

    public TimerViewModel() {
        elapsedTimeLiveData.setValue(0);
    }


    \/**
     *
     * @param time time in seconds
     *\/
    public void startCountFrom(int time) {
        startTime = time;
        statusResult.setValue(false);
        elapsedTimeLiveData.setValue(0);
        task = service.scheduleAtFixedRate(mRunnable, 0, PERIOD_UNIT, TimeUnit.SECONDS);
    }

    public void stop() {
        task.cancel(true);
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
        return statusResult;
    }

    public boolean isStopped() {
        return statusResult.getValue() != null && statusResult.getValue();
    }
}*/
