package com.porfirio.orariprocida2011.threads.transports;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.porfirio.orariprocida2011.entity.Mezzo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

/**
 * Service to automatically retrieve transport updates.
 */
public class TransportsService extends Service implements TransportsDAO {

    /**
     * Binder for communication with the service.
     */
    public final class LocalBinder extends Binder {
        public TransportsService getService() {
            return TransportsService.this;
        }
    }

    private static final String DATABASE_TAG = "Transports";

    private final IBinder binder = new LocalBinder();
    private DatabaseReference database;
    private MutableLiveData<TransportsUpdate> update;
    private ValueEventListener eventListener;

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
                ArrayList<Mezzo> transportList = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Mezzo mezzo = parseMezzo(snapshot);
                    transportList.add(mezzo);
                }
                Log.d("TransportsService", "Numero di elementi trasporto ricevuti: " + dataSnapshot.getChildrenCount());
                update.postValue(new TransportsUpdate(transportList, LocalDateTime.now()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                update.postValue(new TransportsUpdate(databaseError.toException()));
            }
        };

        this.database.addValueEventListener(this.eventListener);
        Log.d("TransportsService", "Servizio Trasporti avviato!");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.database.removeEventListener(eventListener);
    }

    @Override
    public LiveData<TransportsUpdate> getUpdates() {
        return update;
    }

    private Mezzo parseMezzo(DataSnapshot snapshot) {
        LocalDate exclusionStart = snapshot.hasChild("inizioEsclusione") ? LocalDate.parse(snapshot.child("inizioEsclusione").getValue(String.class)) : null;
        LocalDate exclusionEnd = snapshot.hasChild("fineEsclusione") ? LocalDate.parse(snapshot.child("fineEsclusione").getValue(String.class)) : null;
        LocalTime departureTime = LocalTime.parse(snapshot.child("oraPartenza").getValue(String.class));
        LocalTime arrivalTime = LocalTime.parse(snapshot.child("oraArrivo").getValue(String.class));
        byte activeDays = snapshot.hasChild("giorniSettimana") ? getActiveDays(snapshot.child("giorniSettimana").getValue(String.class)) : Byte.MIN_VALUE;

        return new Mezzo(
                snapshot.getKey(),
                snapshot.child("nomeNave").getValue(String.class),
                snapshot.child("portoPartenza").getValue(String.class), departureTime,
                snapshot.child("portoArrivo").getValue(String.class), arrivalTime,
                exclusionStart, exclusionEnd,
                activeDays,
                snapshot.child("prezzoIntero").getValue(Float.class),
                snapshot.child("prezzoRidotto").getValue(Float.class)
        );
    }

    private byte getActiveDays(String days) {
        byte d = 0;
        for (byte i = 1; i <= 7; i++) {
            if (days.contains(Byte.toString(i)))
                d |= (byte) (1 << i);
        }
        return d;
    }
}
