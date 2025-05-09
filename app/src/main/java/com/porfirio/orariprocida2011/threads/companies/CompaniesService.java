package com.porfirio.orariprocida2011.threads.companies;

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
import com.porfirio.orariprocida2011.entity.Compagnia;

import java.util.ArrayList;

public class CompaniesService extends Service implements CompaniesDAO {

    private static final String DATABASE_TAG = "companies";

    public class LocalBinder extends Binder {
        public CompaniesService getService() {
            return CompaniesService.this;
        }
    }

    private final IBinder binder = new LocalBinder();
    private final MutableLiveData<CompaniesUpdate> updates = new MutableLiveData<>();
    private final DatabaseReference database = FirebaseDatabase.getInstance().getReference(DATABASE_TAG);

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        requestUpdate();
    }

    public synchronized void requestUpdate() {
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    ArrayList<Compagnia> companies = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        companies.add(parse(snapshot));
                    }
                    updates.postValue(new CompaniesUpdate(companies));
                } catch (Exception e) {
                    updates.postValue(new CompaniesUpdate(e));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                updates.postValue(new CompaniesUpdate(databaseError.toException()));
            }
        });
    }

    @Override
    public LiveData<CompaniesUpdate> getUpdates() {
        return updates;
    }

    private Compagnia parse(DataSnapshot snapshot) {
        Compagnia company = new Compagnia(snapshot.getKey(), snapshot.child("name").getValue(String.class));
        if (snapshot.hasChild("contacts")) {
            DataSnapshot contacts = snapshot.child("contacts");
            for (DataSnapshot contact : contacts.getChildren()) {
                String name = contact.getKey();
                for (DataSnapshot number : contact.getChildren()) {
                    company.addContact(name, number.getValue(String.class));
                }
            }
        }
        return company;
    }
}