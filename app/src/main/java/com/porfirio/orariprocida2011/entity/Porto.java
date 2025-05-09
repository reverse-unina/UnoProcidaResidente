package com.porfirio.orariprocida2011.entity;

import android.content.Context;
import android.location.Location;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Porto {
    public final String nome;
    public final double latitudine;
    public final double longitudine;

    public Porto(String nome, double latitudine, double longitudine) {
        this.nome = nome;
        this.latitudine = latitudine;
        this.longitudine = longitudine;
    }

    public static List<Porto> caricaPorti(Context context) {
        List<Porto> porti = new ArrayList<>();
        try {
            InputStream is = context.getAssets().open("porti.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                porti.add(new Porto(
                        obj.getString("nome"),
                        obj.getDouble("latitudine"),
                        obj.getDouble("longitudine")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return porti;
    }

    public static String calcolaPortoPiuVicino(Location location, List<Porto> porti, double sogliaMetri) {
        double minDistanza = Double.MAX_VALUE;
        String nomePorto = null;

        for (Porto porto : porti) {
            float[] results = new float[1];
            Location.distanceBetween(
                    location.getLatitude(), location.getLongitude(),
                    porto.latitudine, porto.longitudine,
                    results
            );
            if (results[0] < minDistanza) {
                minDistanza = results[0];
                nomePorto = porto.nome;
            }
        }

        return (minDistanza <= sogliaMetri) ? nomePorto : null;
    }
}
