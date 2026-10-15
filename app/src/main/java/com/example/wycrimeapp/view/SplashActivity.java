package com.example.wycrimeapp.view;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import com.example.wycrimeapp.R;
import com.example.wycrimeapp.controller.AppDatabase;
import com.example.wycrimeapp.model.Crime;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            boolean alreadyImported = prefs.getBoolean("csv_imported", false);

            if (!alreadyImported) {
                importCsvFromAssets(this);  // ← CHANGED: added "this"
                prefs.edit().putBoolean("csv_imported", true).apply();
            }

            SharedPreferences loginPrefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
            boolean isLoggedIn = loginPrefs.getBoolean("isLoggedIn", false);

            if (isLoggedIn) {
                startActivity(new Intent(this, MainActivity.class));
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
            finish();
        }, SPLASH_DELAY);
    }

    // PUBLIC STATIC METHOD – used by AdminFragment too
    public static void importCsvFromAssets(Context context) {
        try {
            InputStream is = context.getAssets().open("West_Yorkshire_Crimes.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            reader.readLine(); // skip header

            AppDatabase db = AppDatabase.getInstance(context);
            db.crimeDao().deleteAllCrimes();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (values.length < 12) continue;

                Crime crime = new Crime();
                crime.setCrimeId(values[0].replace("\"", ""));
                crime.setMonth(values[1].replace("\"", ""));
                crime.setReportedBy(values[2].replace("\"", ""));
                crime.setFallsWithin(values[3].replace("\"", ""));
                crime.setLongitude(values[4].isEmpty() ? 0 : Double.parseDouble(values[4]));
                crime.setLatitude(values[5].isEmpty() ? 0 : Double.parseDouble(values[5]));
                crime.setLocation(values[6].replace("\"", ""));
                crime.setLsoaCode(values[7].replace("\"", ""));
                crime.setLsoaName(values[8].replace("\"", ""));
                crime.setCrimeType(values[9].replace("\"", ""));
                crime.setLastOutcomeCategory(values[10].replace("\"", ""));
                crime.setContext(values.length > 11 ? values[11].replace("\"", "") : "");

                db.crimeDao().insertCrime(crime);
            }
            reader.close();
        } catch (Exception ignored) {}
    }
}