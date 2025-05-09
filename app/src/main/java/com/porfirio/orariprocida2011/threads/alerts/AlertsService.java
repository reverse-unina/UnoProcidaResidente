package com.porfirio.orariprocida2011.threads.alerts;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.porfirio.orariprocida2011.entity.Alert;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Experimental alerts DAO implemented as a service to automatically retrieve alerts.
 * To access DAO methods, the service should be bound.
 */
public class AlertsService extends Service implements AlertsDAO {

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
        public AlertsService getService() {
            return AlertsService.this;
        }

    }

    private static final String DATABASE_TAG = "alerts";

    private final IBinder binder = new AlertsService.LocalBinder();

    private DatabaseReference database;
    private ValueEventListener eventListener;
    private MutableLiveData<AlertUpdate> update;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.update = new MutableLiveData<>();
        this.database = FirebaseDatabase.getInstance().getReference(DATABASE_TAG);
        this.eventListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<Alert> alerts = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Alert alert = parse(snapshot);
                    alerts.add(alert);
                }

                update.postValue(new AlertUpdate(alerts));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                update.postValue(new AlertUpdate(databaseError.toException()));
            }

        };
        this.database.addValueEventListener(this.eventListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.database.removeEventListener(eventListener);
    }

    @Override
    public LiveData<AlertUpdate> getUpdates() {
        return update;
    }

    @Override
    public void send(Alert alert) {
        String key = database.push().getKey();

        if (key == null)
            throw new IllegalStateException();

        HashMap<String, Object> attributes = new HashMap<>();

        attributes.put("transportDate", DateTimeFormatter.ISO_LOCAL_DATE.format(alert.getTransportDate()));
        attributes.put("details", alert.getDetails());
        attributes.put("reason", alert.getReason());

        database.child(key).setValue(attributes);
    }

    private Alert parse(DataSnapshot snapshot) {
            return new Alert(
                    snapshot.getKey(),
                    snapshot.child("routeId").getValue(String.class),
                    snapshot.child("reason").getValue(Integer.class),
                    snapshot.child("details").getValue(String.class),
                    LocalDate.parse(snapshot.child("transportDate").getValue(String.class))
        );
    }
}

