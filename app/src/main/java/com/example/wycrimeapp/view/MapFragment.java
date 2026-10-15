package com.example.wycrimeapp.view;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import android.text.Editable;
import android.text.TextWatcher;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wycrimeapp.R;
import com.example.wycrimeapp.controller.AppDatabase;
import com.example.wycrimeapp.model.Crime;
import com.example.wycrimeapp.util.AddressUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Google Maps screen.
 *
 * Current core behaviours preserved/enhanced:
 * - Displays all crimes by default
 * - Allows Search page to show a filtered subset (or a single crime) on the map
 * - Shows the user's current location (when permission is granted)
 * - Uses a consistent blue marker icon for all crimes (per UI spec)
 * - Provides an explicit "Show all crimes" reset action when the map is filtered
 */
public class MapFragment extends Fragment implements OnMapReadyCallback, CrimeAdapter.OnCrimeClickListener {

    private GoogleMap googleMap;
    private MaterialButton btnResetMap;
    private FusedLocationProviderClient fusedLocationClient;

    // Embedded search UI (moved from SearchFragment per requirement)
    private TextInputEditText editMapSearch;
    private RecyclerView recyclerMapResults;
    private CrimeAdapter mapResultsAdapter;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

    private final ExecutorService geoExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // When true, the reset button is visible because the map is showing a filtered subset.
    private boolean isFilteredView = false;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    enableMyLocation();
                    moveCameraToUserIfAvailable();
                } else {
                    Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        btnResetMap = view.findViewById(R.id.btnResetMap);
        btnResetMap.setOnClickListener(v -> showAllCrimesOnMap());

        editMapSearch = view.findViewById(R.id.editMapSearch);
        recyclerMapResults = view.findViewById(R.id.recyclerMapResults);

        recyclerMapResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        mapResultsAdapter = new CrimeAdapter();
        mapResultsAdapter.setOnCrimeClickListener(this);
        recyclerMapResults.setAdapter(mapResultsAdapter);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        wireUpSearch();

        return view;
    }

    private void wireUpSearch() {
        if (editMapSearch == null) return;

        // Real-time filtering with a small debounce to avoid over-querying Room on each keystroke.
        editMapSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                final String q = (s == null) ? "" : s.toString().trim();
                scheduleSearch(q);
            }
        });

        editMapSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        editMapSearch.setOnEditorActionListener((v, actionId, event) -> {
            // Ensure pressing the keyboard search button triggers an immediate search.
            scheduleSearch(v.getText() != null ? v.getText().toString().trim() : "", true);
            return false;
        });
    }

    private void scheduleSearch(@NonNull String query) {
        scheduleSearch(query, false);
    }

    private void scheduleSearch(@NonNull String query, boolean immediate) {
        if (pendingSearch != null) {
            debounceHandler.removeCallbacks(pendingSearch);
        }
        pendingSearch = () -> performMapSearch(query);
        debounceHandler.postDelayed(pendingSearch, immediate ? 0L : 250L);
    }

    private void performMapSearch(@NonNull String query) {
        if (!isAdded()) return;

        if (query.isEmpty()) {
            recyclerMapResults.setVisibility(View.GONE);
            mapResultsAdapter.setCrimes(Collections.emptyList());
            showAllCrimesOnMap();
            return;
        }

        List<Crime> results = AppDatabase.getInstance(requireContext())
                .crimeDao().searchCrimes(query);

        mapResultsAdapter.setCrimes(results);
        recyclerMapResults.setVisibility(results.isEmpty() ? View.GONE : View.VISIBLE);
        showCrimesOnMapInternal(results, true);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        // Basic UX polish
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        requestOrEnableLocation();

        // Show all crimes by default
        showAllCrimesOnMap();
    }

    /**
     * Public API used by SearchFragment.
     * Shows a filtered subset and reveals a "Show all crimes" reset button.
     */
    public void showCrimesOnMap(@NonNull List<Crime> crimes) {
        showCrimesOnMapInternal(crimes, true);
    }

    /**
     * Convenience method for showing a single crime (e.g., from Search result click).
     */
    public void showSingleCrimeOnMap(@NonNull Crime crime) {
        showCrimesOnMapInternal(Collections.singletonList(crime), true);
    }

    private void showAllCrimesOnMap() {
        List<Crime> allCrimes = AppDatabase.getInstance(requireContext()).crimeDao().getAllCrimes();
        showCrimesOnMapInternal(allCrimes, false);
    }

    private void showCrimesOnMapInternal(@NonNull List<Crime> crimes, boolean filtered) {
        if (googleMap == null) return;

        isFilteredView = filtered;
        btnResetMap.setVisibility(isFilteredView ? View.VISIBLE : View.GONE);

        googleMap.clear();

        for (Crime crime : crimes) {
            LatLng location = new LatLng(crime.getLatitude(), crime.getLongitude());
            googleMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title(crime.getCrimeType())
                    .snippet(crime.getLocation())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        }

        // Camera behaviour:
        // - If filtered and results exist: zoom to first result
        // - Otherwise: attempt to move to user location; if unavailable, fall back to a reasonable default zoom
        if (!crimes.isEmpty() && filtered) {
            LatLng first = new LatLng(crimes.get(0).getLatitude(), crimes.get(0).getLongitude());
            // Use an explicit duration for smoother, less "stiff" camera movement.
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(first, 12f), 850, null);
        } else {
            moveCameraToUserIfAvailable();
        }
    }

    @Override
    public void onCrimeClick(Crime crime) {
        // Selecting a crime from the embedded search results should:
        // - hide the results dropdown
        // - show the selected crime marker
        // - display the crime details popup
        if (!isAdded()) return;

        // Hide dropdown immediately to reduce visual clutter and match expected behaviour.
        recyclerMapResults.setVisibility(View.GONE);
        mapResultsAdapter.setCrimes(Collections.emptyList());

        // Ensure the marker is visible (single-crime view) before showing details.
        showSingleCrimeOnMap(crime);

        final String loadingAddress = "Resolving address…";

        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(crime.getCrimeType() != null ? crime.getCrimeType() : "Crime details")
                .setMessage(buildCrimeDetailsMessage(crime, loadingAddress))
                .setPositiveButton("Close", null)
                .setNegativeButton("Center on map", (d, w) -> showSingleCrimeOnMap(crime))
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

    // NOTE: Marker colouring is intentionally uniform (blue) to align with the requested branding.

    private void requestOrEnableLocation() {
        if (!isAdded() || googleMap == null) return;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
            moveCameraToUserIfAvailable();
        } else {
            // Ask once; if denied, we still keep the rest of the map fully functional.
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void enableMyLocation() {
        if (!isAdded() || googleMap == null) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        googleMap.setMyLocationEnabled(true);
    }

    private void moveCameraToUserIfAvailable() {
        if (!isAdded() || googleMap == null) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // No permission: leave the camera where it is (or at the crimes when loaded).
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null && !isFilteredView) {
                        LatLng me = new LatLng(location.getLatitude(), location.getLongitude());
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 11f), 850, null);
                    }
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        geoExecutor.shutdownNow();
        if (pendingSearch != null) {
            debounceHandler.removeCallbacks(pendingSearch);
        }
    }
}
