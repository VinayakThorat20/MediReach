package com.vinayak.medireach;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.vinayak.medireach.models.Hospital;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

/**
 * HospitalDetailActivity displays detailed information about a specific hospital.
 * Shows all resources, contact details, and location information.
 */
public class HospitalDetailActivity extends AppCompatActivity {

    private static final String TAG = "HospitalDetail";
    private static final String HOSPITALS_COLLECTION = "hospitals";

    // UI Views
    private ImageView imageViewBack;
    private TextView textViewHospitalName;
    private TextView textViewAddress;
    private TextView textViewIcuBedsCount;
    private ImageView imageViewIcuIndicator;
    private TextView textViewOxygenCylindersCount;
    private ImageView imageViewOxygenIndicator;
    private TextView textViewVentilatorsCount;
    private ImageView imageViewVentilatorsIndicator;
    private TextView textViewBloodAPositive;
    private TextView textViewBloodANegative;
    private TextView textViewBloodBPositive;
    private TextView textViewBloodBNegative;
    private TextView textViewBloodOPositive;
    private TextView textViewBloodONegative;
    private TextView textViewBloodABPositive;
    private TextView textViewBloodABNegative;
    private Button buttonEmergencyContact;
    private Button buttonGetDirections;
    private TextView textViewLastUpdated;

    private FirebaseFirestore firebaseFirestore;
    private Hospital currentHospital;
    private String hospitalId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospital_detail);

        // Initialize Firebase
        firebaseFirestore = FirebaseFirestore.getInstance();

        // Get hospital ID from intent
        hospitalId = getIntent().getStringExtra("hospitalId");
        if (hospitalId == null || hospitalId.isEmpty()) {
            Log.e(TAG, "Hospital ID not provided");
            Toast.makeText(this, "Hospital information not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Set up click listeners
        setUpClickListeners();

        // Fetch and display hospital details
        fetchHospitalDetails();
    }

    /**
     * Initializes all UI views from the layout.
     */
    private void initializeViews() {
        imageViewBack = findViewById(R.id.imageViewBack);
        textViewHospitalName = findViewById(R.id.textViewHospitalName);
        textViewAddress = findViewById(R.id.textViewAddress);
        textViewIcuBedsCount = findViewById(R.id.textViewIcuBedsCount);
        imageViewIcuIndicator = findViewById(R.id.imageViewIcuIndicator);
        textViewOxygenCylindersCount = findViewById(R.id.textViewOxygenCylindersCount);
        imageViewOxygenIndicator = findViewById(R.id.imageViewOxygenIndicator);
        textViewVentilatorsCount = findViewById(R.id.textViewVentilatorsCount);
        imageViewVentilatorsIndicator = findViewById(R.id.imageViewVentilatorsIndicator);
        textViewBloodAPositive = findViewById(R.id.textViewBloodAPositive);
        textViewBloodANegative = findViewById(R.id.textViewBloodANegative);
        textViewBloodBPositive = findViewById(R.id.textViewBloodBPositive);
        textViewBloodBNegative = findViewById(R.id.textViewBloodBNegative);
        textViewBloodOPositive = findViewById(R.id.textViewBloodOPositive);
        textViewBloodONegative = findViewById(R.id.textViewBloodONegative);
        textViewBloodABPositive = findViewById(R.id.textViewBloodABPositive);
        textViewBloodABNegative = findViewById(R.id.textViewBloodABNegative);
        buttonEmergencyContact = findViewById(R.id.buttonEmergencyContact);
        buttonGetDirections = findViewById(R.id.buttonGetDirections);
        textViewLastUpdated = findViewById(R.id.textViewLastUpdated);
    }

    /**
     * Sets up click listeners for buttons.
     */
    private void setUpClickListeners() {
        imageViewBack.setOnClickListener(v -> navigateToPatientDashboard());
        buttonEmergencyContact.setOnClickListener(v -> openDialer());
        buttonGetDirections.setOnClickListener(v -> openGoogleMaps());
    }

    private void navigateToPatientDashboard() {
        Intent intent = new Intent(this, PatientDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    /**
     * Fetches hospital details from Firestore.
     */
    private void fetchHospitalDetails() {
        firebaseFirestore.collection(HOSPITALS_COLLECTION).document(hospitalId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentHospital = documentSnapshot.toObject(Hospital.class);
                        if (currentHospital != null) {
                            Log.d(TAG, "Hospital details fetched: " + currentHospital.getHospitalName());
                            displayHospitalDetails();
                        }
                    } else {
                        Log.w(TAG, "Hospital document not found");
                        Toast.makeText(HospitalDetailActivity.this,
                                "Hospital information not available",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Failed to fetch hospital details: " + exception.getMessage());
                    Toast.makeText(HospitalDetailActivity.this,
                            "Failed to load hospital details",
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Displays hospital details in the UI.
     */
    private void displayHospitalDetails() {
        if (currentHospital == null) return;

        // Set hospital name and address
        textViewHospitalName.setText(currentHospital.getHospitalName());
        textViewAddress.setText(currentHospital.getAddress());

        // Display ICU Beds with color coding
        int icuBeds = currentHospital.getIcuBeds();
        textViewIcuBedsCount.setText(String.valueOf(icuBeds));
        setResourceColor(imageViewIcuIndicator, icuBeds);

        // Display Oxygen Cylinders with color coding
        int oxygenCylinders = currentHospital.getOxygenCylinders();
        textViewOxygenCylindersCount.setText(String.valueOf(oxygenCylinders));
        setResourceColor(imageViewOxygenIndicator, oxygenCylinders);

        // Display Ventilators with color coding
        int ventilators = currentHospital.getVentilators();
        textViewVentilatorsCount.setText(String.valueOf(ventilators));
        setResourceColor(imageViewVentilatorsIndicator, ventilators);

        // Display blood units
        displayBloodUnits();

        // Set emergency contact button
        buttonEmergencyContact.setText(currentHospital.getEmergencyContact());

        // Set last updated timestamp
        if (currentHospital.getLastUpdated() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            String formattedDate = sdf.format(currentHospital.getLastUpdated());
            textViewLastUpdated.setText(getString(R.string.last_updated, formattedDate));
        } else {
            textViewLastUpdated.setText(getString(R.string.last_updated, getString(R.string.not_available)));
        }
    }

    /**
     * Displays all blood unit counts.
     */
    private void displayBloodUnits() {
        if (currentHospital.getBloodUnits() == null || currentHospital.getBloodUnits().isEmpty()) {
            return;
        }

        Map<String, Integer> bloodUnits = currentHospital.getBloodUnits();

        textViewBloodAPositive.setText(String.valueOf(bloodUnits.getOrDefault("A+", 0)));
        textViewBloodANegative.setText(String.valueOf(bloodUnits.getOrDefault("A-", 0)));
        textViewBloodBPositive.setText(String.valueOf(bloodUnits.getOrDefault("B+", 0)));
        textViewBloodBNegative.setText(String.valueOf(bloodUnits.getOrDefault("B-", 0)));
        textViewBloodOPositive.setText(String.valueOf(bloodUnits.getOrDefault("O+", 0)));
        textViewBloodONegative.setText(String.valueOf(bloodUnits.getOrDefault("O-", 0)));
        textViewBloodABPositive.setText(String.valueOf(bloodUnits.getOrDefault("AB+", 0)));
        textViewBloodABNegative.setText(String.valueOf(bloodUnits.getOrDefault("AB-", 0)));
    }

    /**
     * Sets the color indicator based on resource count.
     * Green (count > 3), Yellow (1-3), Red (0)
     *
     * @param imageView The indicator view
     * @param count     The resource count
     */
    private void setResourceColor(ImageView imageView, int count) {
        int color;
        if (count > 3) {
            color = ContextCompat.getColor(this, android.R.color.holo_green_dark);
        } else if (count >= 1) {
            color = ContextCompat.getColor(this, android.R.color.holo_orange_dark);
        } else {
            color = ContextCompat.getColor(this, android.R.color.holo_red_dark);
        }
        imageView.setColorFilter(color);
    }

    /**
     * Opens phone dialer with emergency contact number.
     */
    private void openDialer() {
        if (currentHospital != null && currentHospital.getEmergencyContact() != null) {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:" + currentHospital.getEmergencyContact()));
            startActivity(dialIntent);
        }
    }

    /**
     * Opens Google Maps with hospital coordinates.
     */
    private void openGoogleMaps() {
        if (currentHospital != null) {
            String mapUri = String.format(Locale.US,
                    "geo:%f,%f?q=%s",
                    currentHospital.getLatitude(),
                    currentHospital.getLongitude(),
                    currentHospital.getHospitalName()
            );
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapUri));
            mapIntent.setPackage("com.google.android.apps.maps");

            try {
                startActivity(mapIntent);
            } catch (Exception e) {
                Log.e(TAG, "Google Maps not installed, opening web version");
                String webMapUri = String.format(Locale.US,
                        "https://www.google.com/maps/search/?api=1&query=%f,%f&query=%s",
                        currentHospital.getLatitude(),
                        currentHospital.getLongitude(),
                        currentHospital.getHospitalName()
                );
                Intent webMapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webMapUri));
                startActivity(webMapIntent);
            }
        }
    }
}
