package com.example.wycrimeapp.view;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.wycrimeapp.R;
import com.example.wycrimeapp.controller.AppDatabase;
import com.example.wycrimeapp.model.Crime;

public class AddUpdateCrimeActivity extends AppCompatActivity {

    private EditText[] fields = new EditText[12];
    private Crime editingCrime = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_update_crime);

        int[] ids = {R.id.et1, R.id.et2, R.id.et3, R.id.et4, R.id.et5, R.id.et6,
                R.id.et7, R.id.et8, R.id.et9, R.id.et10, R.id.et11, R.id.et12};
        for (int i = 0; i < 12; i++) fields[i] = findViewById(ids[i]);

        // Check if we are editing
        editingCrime = (Crime) getIntent().getSerializableExtra("crime_to_edit");
        if (editingCrime != null) {
            fillFields();
            setTitle("Update Crime");
        } else {
            setTitle("Add New Crime");
        }

        Button btnSave = findViewById(R.id.btnSaveCrime);
        btnSave.setOnClickListener(v -> saveCrime());
    }

    private void fillFields() {
        fields[0].setText(editingCrime.getCrimeId());
        fields[1].setText(editingCrime.getMonth());
        fields[2].setText(editingCrime.getReportedBy());
        fields[3].setText(editingCrime.getFallsWithin());
        fields[4].setText(String.valueOf(editingCrime.getLongitude()));
        fields[5].setText(String.valueOf(editingCrime.getLatitude()));
        fields[6].setText(editingCrime.getLocation());
        fields[7].setText(editingCrime.getLsoaCode());
        fields[8].setText(editingCrime.getLsoaName());
        fields[9].setText(editingCrime.getCrimeType());
        fields[10].setText(editingCrime.getLastOutcomeCategory());
        fields[11].setText(editingCrime.getContext());
    }

    private void saveCrime() {
        Crime crime = editingCrime != null ? editingCrime : new Crime();

        crime.setCrimeId(fields[0].getText().toString());
        crime.setMonth(fields[1].getText().toString());
        crime.setReportedBy(fields[2].getText().toString());
        crime.setFallsWithin(fields[3].getText().toString());
        crime.setLongitude(parseDouble(fields[4].getText().toString()));
        crime.setLatitude(parseDouble(fields[5].getText().toString()));
        crime.setLocation(fields[6].getText().toString());
        crime.setLsoaCode(fields[7].getText().toString());
        crime.setLsoaName(fields[8].getText().toString());
        crime.setCrimeType(fields[9].getText().toString());
        crime.setLastOutcomeCategory(fields[10].getText().toString());
        crime.setContext(fields[11].getText().toString());

        AppDatabase db = AppDatabase.getInstance(this);
        if (editingCrime != null) {
            db.crimeDao().updateCrime(crime);
            Toast.makeText(this, "Crime updated!", Toast.LENGTH_SHORT).show();
        } else {
            db.crimeDao().insertCrime(crime);
            Toast.makeText(this, "Crime added!", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }
}