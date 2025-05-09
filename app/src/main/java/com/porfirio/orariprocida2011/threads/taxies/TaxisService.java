package com.porfirio.orariprocida2011.threads.taxies;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.porfirio.orariprocida2011.entity.Taxi;
import java.util.ArrayList;

public class TaxisService extends Service implements TaxisDAO {

    private static final String DATABASE_TAG = "taxis";

    public class LocalBinder extends Binder {
        public TaxisService getService() {
            return TaxisService.this;
        }
    }

    private final IBinder binder = new LocalBinder();
    private final MutableLiveData<TaxisUpdate> updates = new MutableLiveData<>();
    private final DatabaseReference database = FirebaseDatabase.getInstance().getReference(DATABASE_TAG);

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("TaxisService", "TaxisService avviato!");
        requestUpdate();
    }

    public synchronized void requestUpdate() {
        Log.d("TaxisService", "Richiesta aggiornamento taxi inviata");
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    ArrayList<Taxi> taxies = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        taxies.add(parse(snapshot));
                    }
                    updates.postValue(new TaxisUpdate(taxies));
                    Log.d("TaxisService", "Dati taxi aggiornati: " + taxies.size() + " taxi trovati");
                } catch (Exception e) {
                    updates.postValue(new TaxisUpdate(e));
                    Log.e("TaxisService", "Errore durante l'aggiornamento dei taxi", e);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                updates.postValue(new TaxisUpdate(databaseError.toException()));
                Log.e("TaxisService", "Errore nella richiesta dati: " + databaseError.getMessage());
            }
        });
    }

    @Override
    public LiveData<TaxisUpdate> getUpdates() {
        return updates;
    }

    private Taxi parse(DataSnapshot snapshot) {
        String location = snapshot.child("location").getValue(String.class);
        String name = snapshot.child("name").getValue(String.class);
        String number = snapshot.child("number").getValue(String.class);
        return new Taxi(name, number, location);
    }
}