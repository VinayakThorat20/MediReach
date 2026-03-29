package com.vinayak.medireach;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vinayak.medireach.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

public class HospitalSetupActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST_CODE = 2001;

    private EditText editTextHospitalName;
    private EditText editTextAddress;
    private EditText editTextCity;
    private EditText editTextPincode;
    private EditText editTextEmergencyContact;
    private EditText editTextLatitude;
    private EditText editTextLongitude;
    private Spinner spinnerHospitalType;
    private TextView textViewDetectedLocation;
    private Button buttonDetectLocation;
    private Button buttonVerifyMap;
    private Button buttonSaveContinue;
    private ProgressBar progressSave;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            SessionManager.logoutAndOpenLogin(this);
            return;
        }

        if (SessionManager.isHospitalSetupDone(this)) {
            startActivity(new Intent(this, HospitalAdminDashboardActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_hospital_setup);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupSpinner();
        bindActions();
    }

    private void initViews() {
        editTextHospitalName = findViewById(R.id.editTextHospitalName);
        editTextAddress = findViewById(R.id.editTextAddress);
        editTextCity = findViewById(R.id.editTextCity);
        editTextPincode = findViewById(R.id.editTextPincode);
        editTextEmergencyContact = findViewById(R.id.editTextEmergencyContact);
        editTextLatitude = findViewById(R.id.editTextLatitude);
        editTextLongitude = findViewById(R.id.editTextLongitude);
        spinnerHospitalType = findViewById(R.id.spinnerHospitalType);
        textViewDetectedLocation = findViewById(R.id.textViewDetectedLocation);
        buttonDetectLocation = findViewById(R.id.buttonDetectLocation);
        buttonVerifyMap = findViewById(R.id.buttonVerifyMap);
        buttonSaveContinue = findViewById(R.id.buttonSaveContinue);
        progressSave = findViewById(R.id.progressSaveHospitalSetup);
    }

    private void setupSpinner() {
        String[] hospitalTypes = {"Government", "Private", "Trust", "Clinic"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                hospitalTypes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHospitalType.setAdapter(adapter);
    }

    private void bindActions() {
        buttonDetectLocation.setOnClickListener(v -> detectHospitalLocation());
        buttonVerifyMap.setOnClickListener(v -> verifyOnMap());
        buttonSaveContinue.setOnClickListener(v -> saveHospitalProfile());
    }

    private void detectHospitalLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_REQUEST_CODE);
            return;
        }
        getCurrentLocation();
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        Toast.makeText(this, "Unable to detect location. Please enter manually.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    editTextLatitude.setText(String.valueOf(latitude));
                    editTextLongitude.setText(String.valueOf(longitude));
                    textViewDetectedLocation.setText("Location detected: " + latitude + ", " + longitude);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Location detection failed", Toast.LENGTH_SHORT).show());
    }

    private void verifyOnMap() {
        String latText = editTextLatitude.getText().toString().trim();
        String lonText = editTextLongitude.getText().toString().trim();
        if (TextUtils.isEmpty(latText) || TextUtils.isEmpty(lonText)) {
            Toast.makeText(this, "Enter latitude and longitude first", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            double latitude = Double.parseDouble(latText);
            double longitude = Double.parseDouble(lonText);
            Uri uri = Uri.parse("geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude + "(Hospital Location)");
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid coordinates", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveHospitalProfile() {
        String hospitalName = editTextHospitalName.getText().toString().trim();
        String address = editTextAddress.getText().toString().trim();
        String city = editTextCity.getText().toString().trim();
        String pincode = editTextPincode.getText().toString().trim();
        String emergencyContact = editTextEmergencyContact.getText().toString().trim();
        String latitudeText = editTextLatitude.getText().toString().trim();
        String longitudeText = editTextLongitude.getText().toString().trim();

        if (TextUtils.isEmpty(hospitalName) || TextUtils.isEmpty(address)
                || TextUtils.isEmpty(city) || TextUtils.isEmpty(pincode)
                || TextUtils.isEmpty(emergencyContact)
                || TextUtils.isEmpty(latitudeText) || TextUtils.isEmpty(longitudeText)) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (emergencyContact.length() < 10) {
            editTextEmergencyContact.setError("Emergency contact must be at least 10 digits");
            editTextEmergencyContact.requestFocus();
            return;
        }

        double latitude;
        double longitude;
        try {
            latitude = Double.parseDouble(latitudeText);
            longitude = Double.parseDouble(longitudeText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter valid coordinates", Toast.LENGTH_SHORT).show();
            return;
        }

        if (latitude < -90 || latitude > 90) {
            editTextLatitude.setError("Latitude must be between -90 and 90");
            editTextLatitude.requestFocus();
            return;
        }
        if (longitude < -180 || longitude > 180) {
            editTextLongitude.setError("Longitude must be between -180 and 180");
            editTextLongitude.requestFocus();
            return;
        }

        setLoading(true);

        Map<String, Integer> bloodUnits = new HashMap<>();
        bloodUnits.put("A+", 0);
        bloodUnits.put("A-", 0);
        bloodUnits.put("B+", 0);
        bloodUnits.put("B-", 0);
        bloodUnits.put("O+", 0);
        bloodUnits.put("O-", 0);
        bloodUnits.put("AB+", 0);
        bloodUnits.put("AB-", 0);

        Map<String, Object> hospitalData = new HashMap<>();
        hospitalData.put("hospitalName", hospitalName);
        hospitalData.put("address", address);
        hospitalData.put("city", city);
        hospitalData.put("pincode", pincode);
        hospitalData.put("hospitalType", String.valueOf(spinnerHospitalType.getSelectedItem()));
        hospitalData.put("latitude", latitude);
        hospitalData.put("longitude", longitude);
        hospitalData.put("emergencyContact", emergencyContact);
        hospitalData.put("adminUid", currentUser.getUid());
        hospitalData.put("icuBeds", 0);
        hospitalData.put("oxygenCylinders", 0);
        hospitalData.put("ventilators", 0);
        hospitalData.put("bloodUnits", bloodUnits);
        hospitalData.put("createdAt", FieldValue.serverTimestamp());
        hospitalData.put("lastUpdated", FieldValue.serverTimestamp());

        db.collection("hospitals")
                .document(currentUser.getUid())
                .set(hospitalData)
                .addOnSuccessListener(unused -> {
                    SessionManager.setHospitalSetupDone(this, true);
                    SessionManager.saveHospitalName(this, hospitalName);
                    Toast.makeText(this, "Hospital profile created successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, HospitalAdminDashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to create hospital profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean loading) {
        progressSave.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonSaveContinue.setEnabled(!loading);
        buttonDetectLocation.setEnabled(!loading);
        buttonVerifyMap.setEnabled(!loading);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied. Please enter coordinates manually.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

