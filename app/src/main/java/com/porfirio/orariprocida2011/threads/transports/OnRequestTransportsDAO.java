package com.porfirio.orariprocida2011.threads.transports;

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
 * Transports DAO which needs a manual request to receive updates.
 */
public class OnRequestTransportsDAO implements TransportsDAO {

    private final MutableLiveData<TransportsUpdate> update;
    private final DatabaseReference databaseReference;
    private boolean requested;

    public OnRequestTransportsDAO() {
        this.update = new MutableLiveData<>();
        this.databaseReference = FirebaseDatabase.getInstance().getReference("Transports");
    }

    /**
     * Requests an update.
     */
    public synchronized void requestUpdate() {
        if (requested)
            return;

        databaseReference.keepSynced(true);
        requested = true;

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<Mezzo> transportList = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Mezzo mezzo = parseMezzo(snapshot);
                    transportList.add(mezzo);
                }

                update.setValue(new TransportsUpdate(transportList, LocalDateTime.now()));
                databaseReference.keepSynced(false);
                requested = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                update.setValue(new TransportsUpdate(databaseError.toException()));
                databaseReference.keepSynced(false);
                requested = false;
            }

        });
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
