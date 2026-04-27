package com.vinayak.medireach;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.vinayak.medireach.adapters.DonorAdapter;
import com.vinayak.medireach.models.Donor;
import com.vinayak.medireach.utils.LocaleHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DonorSearchActivity extends AppCompatActivity {

    private Spinner spinnerBloodGroup;
    private Spinner spinnerCity;
    private Button buttonSearch;
    private RecyclerView recyclerView;
    private TextView emptyStateView;

    private final List<Donor> donorList = new ArrayList<>();
    private final List<Donor> filteredDonorList = new ArrayList<>();
    private DonorAdapter donorAdapter;
    private FirebaseFirestore db;
    private ListenerRegistration donorListener;

    private String selectedBloodGroup = "All";
    private String selectedCity = "All";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donor_search);

        db = FirebaseFirestore.getInstance();
        initViews();
        setupSpinners();
        setupRecyclerView();
        bindActions();
        listenForDonors();
    }

    private void initViews() {
        spinnerBloodGroup = findViewById(R.id.spinnerBloodGroupFilter);
        spinnerCity = findViewById(R.id.spinnerCityFilter);
        buttonSearch = findViewById(R.id.buttonSearchDonors);
        recyclerView = findViewById(R.id.recyclerViewDonors);
        emptyStateView = findViewById(R.id.textViewDonorEmptyState);
    }

    private void setupSpinners() {
        ArrayAdapter<String> bloodAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"All", "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"}
        );
        bloodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(bloodAdapter);

        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new ArrayList<String>()
        );
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCity.setAdapter(cityAdapter);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        donorAdapter = new DonorAdapter(filteredDonorList, this);
        recyclerView.setAdapter(donorAdapter);
        emptyStateView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void bindActions() {
        buttonSearch.setOnClickListener(v -> {
            selectedBloodGroup = valueOrAll(spinnerBloodGroup.getSelectedItem());
            selectedCity = valueOrAll(spinnerCity.getSelectedItem());
            applyFilters();
        });
    }

    private void listenForDonors() {
        donorListener = db.collection("donors")
                .whereEqualTo("isAvailable", true)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    donorList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Donor donor = doc.toObject(Donor.class);
                        if (donor != null) {
                            if (TextUtils.isEmpty(donor.getUid())) {
                                donor.setUid(doc.getId());
                            }
                            donorList.add(donor);
                        }
                    }
                    updateCityList();
                    applyFilters();
                });
    }

    private void updateCityList() {
        Set<String> cities = new LinkedHashSet<>();
        for (Donor donor : donorList) {
            if (!TextUtils.isEmpty(donor.getCity())) {
                cities.add(donor.getCity().trim());
            }
        }
        List<String> cityItems = new ArrayList<>();
        cityItems.add("All");
        cityItems.addAll(cities);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                cityItems
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCity.setAdapter(adapter);
    }

    private void applyFilters() {
        filteredDonorList.clear();
        for (Donor donor : donorList) {
            boolean bloodGroupMatch =
                    selectedBloodGroup.equals("All") ||
                            (donor.getBloodGroup() != null && donor.getBloodGroup().equals(selectedBloodGroup));
            boolean cityMatch =
                    selectedCity.equals("All") ||
                            (donor.getCity() != null && donor.getCity().equalsIgnoreCase(selectedCity));
            if (bloodGroupMatch && cityMatch) {
                filteredDonorList.add(donor);
            }
        }
        donorAdapter.notifyDataSetChanged();
        if (filteredDonorList.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private String valueOrAll(Object value) {
        return value == null ? "All" : String.valueOf(value);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (donorListener != null) donorListener.remove();
    }
}

