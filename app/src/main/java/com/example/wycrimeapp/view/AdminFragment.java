package com.example.wycrimeapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.example.wycrimeapp.R;
import com.example.wycrimeapp.controller.AppDatabase;

public class AdminFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin, container, false);

        Button btnAdd = view.findViewById(R.id.btnAddCrime);
        Button btnImport = view.findViewById(R.id.btnReimportCsv);

        btnAdd.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddUpdateCrimeActivity.class)));

        btnImport.setOnClickListener(v -> {
            SplashActivity.importCsvFromAssets(requireContext());  // <-- added requireContext()
            Toast.makeText(requireContext(), "CSV Re-imported! 1610 crimes refreshed.", Toast.LENGTH_LONG).show();

            // Refresh List tab
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshListTab();
            }
        });

        return view;
    }
}