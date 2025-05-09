package com.porfirio.orariprocida2011.threads.weather;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.porfirio.orariprocida2011.entity.Osservazione;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A service which automatically retrieves weather forecasts.
 */
public class WeatherService extends Service implements WeatherDAO {

    /**
     * A local binder to communicate with the service.
     */
    public final class LocalBinder extends Binder {

        private LocalBinder() {

        }

        /**
         * Returns the service associated with this binder.
         *
         * @return service of this binder
         */
        public WeatherService getService() {
            return WeatherService.this;
        }

    }

    private final IBinder binder = new LocalBinder();

    private final MutableLiveData<WeatherUpdate> updates = new MutableLiveData<>();
    private ScheduledExecutorService service;
    private ScheduledFuture<?> future;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        service = Executors.newSingleThreadScheduledExecutor();
        future = service.scheduleWithFixedDelay(() -> {
            try {
                List<Osservazione> forecasts = WeatherAPI.getForecasts();
                updates.postValue(new WeatherUpdate(forecasts));
            } catch (Exception e) {
                updates.postValue(new WeatherUpdate(e));
            }
        }, 0, 1, TimeUnit.HOURS);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        future.cancel(false);
        service.shutdown();
    }

    @Override
    public LiveData<WeatherUpdate> getUpdates() {
        return updates;
    }

}