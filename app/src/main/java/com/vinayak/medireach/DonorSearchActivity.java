package com.vinayak.medireach;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vinayak.medireach.adapters.DonorAdapter;
import com.vinayak.medireach.models.Donor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DonorSearchActivity extends AppCompatActivity {

    private RecyclerView recyclerViewDonors;
    private DonorAdapter adapter;
    private List<Donor> allDonors = new ArrayList<>();
    private List<Donor> filteredDonors = new ArrayList<>();
    
    private Spinner spinnerBloodGroup, spinnerCity;
    private ProgressBar progressBar;
    private TextView textViewNoDonors;
    
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donor_search);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupSpinners();
        fetchDonors();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        recyclerViewDonors = findViewById(R.id.recyclerViewDonors);
        recyclerViewDonors.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DonorAdapter(filteredDonors, this);
        recyclerViewDonors.setAdapter(adapter);

        spinnerBloodGroup = findViewById(R.id.spinnerBloodGroup);
        spinnerCity = findViewById(R.id.spinnerCity);
        progressBar = findViewById(R.id.progressBar);
        textViewNoDonors = findViewById(R.id.textViewNoDonors);
    }

    private void setupSpinners() {
        String[] bloodGroups = {"All", "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};
        ArrayAdapter<String> bgAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bloodGroups);
        bgAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(bgAdapter);

        spinnerBloodGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void fetchDonors() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("donors")
                .whereEqualTo("isAvailable", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    allDonors.clear();
                    Set<String> cities = new HashSet<>();
                    cities.add("All");
                    
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Donor donor = doc.toObject(Donor.class);
                        if (donor != null) {
                            allDonors.add(donor);
                            if (donor.getCity() != null && !donor.getCity().isEmpty()) {
                                cities.add(donor.getCity());
                            }
                        }
                    }
                    
                    updateCitySpinner(cities);
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error fetching donors: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateCitySpinner(Set<String> cities) {
        List<String> cityList = new ArrayList<>(cities);
        Collections.sort(cityList);
        // Move "All" to front if it's there
        if (cityList.contains("All")) {
            cityList.remove("All");
            cityList.add(0, "All");
        }
        
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cityList);
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCity.setAdapter(cityAdapter);
    }

    private void applyFilters() {
        String selectedBG = spinnerBloodGroup.getSelectedItem() != null ? spinnerBloodGroup.getSelectedItem().toString() : "All";
        String selectedCity = spinnerCity.getSelectedItem() != null ? spinnerCity.getSelectedItem().toString() : "All";

        filteredDonors.clear();
        for (Donor donor : allDonors) {
            boolean bgMatch = selectedBG.equals("All") || selectedBG.equals(donor.getBloodGroup());
            boolean cityMatch = selectedCity.equals("All") || selectedCity.equalsIgnoreCase(donor.getCity());
            
            if (bgMatch && cityMatch) {
                filteredDonors.add(donor);
            }
        }

        adapter.updateList(filteredDonors);
        textViewNoDonors.setVisibility(filteredDonors.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
