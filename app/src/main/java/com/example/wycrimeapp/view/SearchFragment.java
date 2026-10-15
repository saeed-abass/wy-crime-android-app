package com.example.wycrimeapp.view;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wycrimeapp.R;
import com.example.wycrimeapp.controller.AppDatabase;
import com.example.wycrimeapp.model.Crime;
import com.example.wycrimeapp.util.AddressUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Search screen.
 *
 * Changes per coursework requirements:
 * - Removed the "Show Results on Map" button.
 * - Each search result click opens a full details dialog.
 * - Dialog provides: Close + Show on map.
 * - When "Show on map" is used, the map exposes a reset button to restore all crimes.
 */
public class SearchFragment extends Fragment implements CrimeAdapter.OnCrimeClickListener {

    private EditText editSearch;
    private RecyclerView recyclerSearch;
    private CrimeAdapter adapter;
    private List<Crime> currentResults = new ArrayList<>();

    private final ExecutorService geoExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        editSearch = view.findViewById(R.id.editSearch);
        recyclerSearch = view.findViewById(R.id.recyclerSearch);

        recyclerSearch.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CrimeAdapter();
        adapter.setOnCrimeClickListener(this);
        recyclerSearch.setAdapter(adapter);

        // Real-time search as user types
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                performSearch(s.toString().trim());
            }
        });

        return view;
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            currentResults.clear();
            adapter.setCrimes(currentResults);
            recyclerSearch.setVisibility(View.GONE);
            return;
        }

        List<Crime> results = AppDatabase.getInstance(requireContext())
                .crimeDao().searchCrimes(query);

        currentResults = results;
        adapter.setCrimes(results);
        recyclerSearch.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCrimeClick(Crime crime) {
        // Per requirement: show a popup with full details, including street address.
        // Provide Close + Show on map.

        final String loadingAddress = "Resolving address…";

        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(crime.getCrimeType() != null ? crime.getCrimeType() : "Crime details")
                .setMessage(buildCrimeDetailsMessage(crime, loadingAddress))
                .setPositiveButton("Close", null)
                .setNegativeButton("Show on map", (d, w) -> showCrimeOnMap(crime))
                .show();

        // Rounded-corner dialog chrome to match the app's visual style.
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }

        geoExecutor.execute(() -> {
            String address = AddressUtils.getStreetAddress(requireContext(), crime.getLatitude(), crime.getLongitude());
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (dialog.isShowing()) {
                    dialog.setMessage(buildCrimeDetailsMessage(crime, address));
                }
            });
        });
    }

    private void showCrimeOnMap(Crime crime) {
        if (!(getActivity() instanceof MainActivity)) return;

        // UX requirement: once a result is selected and we center on it, hide the drop-down results list.
        currentResults.clear();
        adapter.setCrimes(currentResults);
        recyclerSearch.setVisibility(View.GONE);

        // Map tab is position 0; ViewPager2 fragment tag format is "f<index>"
        MapFragment mapFragment = (MapFragment) getParentFragmentManager().findFragmentByTag("f0");
        if (mapFragment != null) {
            mapFragment.showSingleCrimeOnMap(crime);
            ((MainActivity) getActivity()).selectMapTab();
        }
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
    public void onDestroy() {
        super.onDestroy();
        geoExecutor.shutdownNow();
    }
}
