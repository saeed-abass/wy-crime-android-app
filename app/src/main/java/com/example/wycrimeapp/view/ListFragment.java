package com.example.wycrimeapp.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.wycrimeapp.R;
import com.example.wycrimeapp.controller.AppDatabase;
import com.example.wycrimeapp.model.Crime;
import com.example.wycrimeapp.util.AddressUtils;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListFragment extends Fragment implements CrimeAdapter.OnCrimeLongClickListener, CrimeAdapter.OnCrimeActionListener, CrimeAdapter.OnCrimeClickListener {

    private RecyclerView recyclerView;
    private CrimeAdapter adapter;
    private TextView tvTitle;
    private LinearLayout adminActions;
    private ImageButton btnAddCrime;
    private ImageButton btnReimportCsv;
    private TextInputEditText editListSearch;
    private boolean isAdmin = false;
    private final ExecutorService geoExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        tvTitle = view.findViewById(R.id.tvTitle);
        adminActions = view.findViewById(R.id.adminActions);
        btnAddCrime = view.findViewById(R.id.btnAddCrime);
        btnReimportCsv = view.findViewById(R.id.btnReimportCsv);
        editListSearch = view.findViewById(R.id.editListSearch);
        recyclerView = view.findViewById(R.id.recyclerCrimes);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CrimeAdapter();
        adapter.setOnCrimeLongClickListener(this);
        adapter.setOnCrimeActionListener(this);
        adapter.setOnCrimeClickListener(this);
        recyclerView.setAdapter(adapter);

        SharedPreferences prefs = requireContext().getSharedPreferences("login_prefs", 0);
        isAdmin = prefs.getBoolean("isAdmin", false);
        adapter.setAdmin(isAdmin);

        // Admin-only top-bar actions (replaces the separate Admin page)
        adminActions.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        btnAddCrime.setOnClickListener(v -> {
            if (!isAdmin) return;
            startActivity(new Intent(requireContext(), AddUpdateCrimeActivity.class));
        });
        btnReimportCsv.setOnClickListener(v -> {
            if (!isAdmin) return;
            SplashActivity.importCsvFromAssets(requireContext());
            Toast.makeText(requireContext(), "CSV re-imported", Toast.LENGTH_SHORT).show();
            loadCrimes();
        });

        wireUpListSearch();

        loadCrimes();
        return view;
    }

    private void wireUpListSearch() {
        if (editListSearch == null) return;

        editListSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String q = (s == null) ? "" : s.toString().trim();
                scheduleSearch(q);
            }
        });
    }

    private void scheduleSearch(String query) {
        if (pendingSearch != null) {
            debounceHandler.removeCallbacks(pendingSearch);
        }
        pendingSearch = () -> applyListFilter(query);
        debounceHandler.postDelayed(pendingSearch, 200L);
    }

    private void applyListFilter(String query) {
        if (!isAdded()) return;

        List<Crime> crimes;
        if (query == null || query.trim().isEmpty()) {
            crimes = AppDatabase.getInstance(requireContext()).crimeDao().getAllCrimes();
        } else {
            crimes = AppDatabase.getInstance(requireContext()).crimeDao().searchCrimes(query.trim());
        }

        tvTitle.setText("Crimes (" + crimes.size() + ")");
        adapter.setCrimes(crimes);
    }

    public void loadCrimes() {
        // Respect any active list filter.
        String q = (editListSearch != null && editListSearch.getText() != null)
                ? editListSearch.getText().toString().trim()
                : "";
        applyListFilter(q);
    }

    @Override
    public void onCrimeLongClick(Crime crime) {
        if (!isAdmin) {
            Toast.makeText(requireContext(), "Only Admin can edit/delete", Toast.LENGTH_SHORT).show();
            return;
        }

        // Legacy fallback: long-press shows action sheet.
        // Core actions are also available via explicit Edit/Delete buttons for admins.
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Crime Actions")
                .setItems(new String[]{"Edit", "Delete"}, (dialog, which) -> {
                    if (which == 0) {
                        onEdit(crime);
                    } else {
                        onDelete(crime);
                    }
                })
                .show();
    }

    @Override
    public void onCrimeClick(Crime crime) {
        // Tap on a row shows a full details popup.
        // Per requirement, show a human-friendly street address rather than raw lat/lon.

        final String loadingAddress = "Resolving address…";
        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(crime.getCrimeType() != null ? crime.getCrimeType() : "Crime details")
                .setMessage(buildCrimeDetailsMessage(crime, loadingAddress))
                .setPositiveButton("Close", null)
                .show();

        // Rounded-corner dialog chrome to match the app's visual style.
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }

        geoExecutor.execute(() -> {
            String address = AddressUtils.getStreetAddress(requireContext(), crime.getLatitude(), crime.getLongitude());
            mainHandler.post(() -> {
                if (!isAdded()) return;
                // Update the dialog message with the resolved address.
                if (dialog.isShowing()) {
                    dialog.setMessage(buildCrimeDetailsMessage(crime, address));
                }
            });
        });
    }

    private String buildCrimeDetailsMessage(Crime crime, String streetAddress) {
        StringBuilder sb = new StringBuilder();
        sb.append("Crime ID: ").append(nullToDash(crime.getCrimeId())).append("\n\n");

        sb.append("Month: ").append(nullToDash(crime.getMonth())).append("\n");
        sb.append("Reported By: ").append(nullToDash(crime.getReportedBy())).append("\n");
        sb.append("Falls Within: ").append(nullToDash(crime.getFallsWithin())).append("\n\n");

        sb.append("Location (label): ").append(nullToDash(crime.getLocation())).append("\n");
        sb.append("Street Address: ").append(nullToDash(streetAddress)).append("\n\n");

        sb.append("LSOA Code: ").append(nullToDash(crime.getLsoaCode())).append("\n");
        sb.append("LSOA Name: ").append(nullToDash(crime.getLsoaName())).append("\n\n");

        sb.append("Crime Type: ").append(nullToDash(crime.getCrimeType())).append("\n");
        sb.append("Outcome: ").append(nullToDash(crime.getLastOutcomeCategory())).append("\n\n");

        sb.append("Context: ").append(nullToDash(crime.getContext()));

        return sb.toString();
    }

    private String nullToDash(String value) {
        return (value == null || value.trim().isEmpty()) ? "-" : value;
    }

    @Override
    public void onEdit(Crime crime) {
        if (!isAdmin) return;

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Edit crime")
                .setMessage("Do you want to edit this crime record?")
                .setPositiveButton("Edit", (d, w) -> {
                    Intent intent = new Intent(requireContext(), AddUpdateCrimeActivity.class);
                    intent.putExtra("crime_to_edit", crime);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDelete(Crime crime) {
        if (!isAdmin) return;

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete crime")
                .setMessage("This action cannot be undone. Delete this crime record?")
                .setPositiveButton("Delete", (d, w) -> {
                    AppDatabase.getInstance(requireContext()).crimeDao().deleteCrime(crime);
                    Toast.makeText(requireContext(), "Crime deleted!", Toast.LENGTH_SHORT).show();
                    loadCrimes();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Respect any active filter when returning to the screen.
        String q = editListSearch != null && editListSearch.getText() != null ? editListSearch.getText().toString() : "";
        applyListFilter(q);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Avoid leaking a background thread if the fragment is destroyed.
        geoExecutor.shutdownNow();
        if (pendingSearch != null) {
            debounceHandler.removeCallbacks(pendingSearch);
        }
    }
}