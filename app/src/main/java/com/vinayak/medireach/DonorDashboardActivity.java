package com.vinayak.medireach;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.vinayak.medireach.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * DonorDashboardActivity allows donors to manage their profile and donation information.
 * Loads existing profiles and allows saving/updating donor information to Firestore.
 */
public class DonorDashboardActivity extends AppCompatActivity {

    private static final String TAG = "DonorDashboard";
    private static final String DONORS_COLLECTION = "donors";

    // UI Fields
    private EditText editTextFullName;
    private Spinner spinnerBloodGroup;
    private EditText editTextCity;
    private EditText editTextPhoneNumber;
    private Button buttonLastDonationDate;
    private Switch switchAvailable;
    private CheckBox checkBoxOrganDonor;
    private Button buttonSaveProfile;
    private CardView confirmationCard;
    private TextView textViewConfirmationMessage;
    private View progressSave;
    private ImageView imageViewLogout;
    private ImageView imageViewProfile;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseUser currentUser;
    private final Calendar selectedDate = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donor_dashboard);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        // Verify user is logged in
        if (currentUser == null) {
            SessionManager.logoutAndOpenLogin(this);
            return;
        }

        // Initialize views
        initViews();

        // Set up blood group spinner
        setupBloodGroupSpinner();

        // Set up click listeners
        setupActions();

        // Handle back press
        setupBackPress();

        // Load existing donor profile
        loadDonorProfile();
    }

    /**
     * Initializes all UI views from the layout.
     */
    private void initViews() {
        editTextFullName = findViewById(R.id.editTextFullName);
        spinnerBloodGroup = findViewById(R.id.spinnerBloodGroup);
        editTextCity = findViewById(R.id.editTextCity);
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        buttonLastDonationDate = findViewById(R.id.buttonLastDonationDate);
        switchAvailable = findViewById(R.id.switchAvailable);
        checkBoxOrganDonor = findViewById(R.id.checkBoxOrganDonor);
        buttonSaveProfile = findViewById(R.id.buttonSaveProfile);
        confirmationCard = findViewById(R.id.cardViewConfirmation);
        textViewConfirmationMessage = findViewById(R.id.textViewConfirmationMessage);
        progressSave = findViewById(R.id.progressSave);
        imageViewLogout = findViewById(R.id.imageViewLogout);
        imageViewProfile = findViewById(R.id.imageViewProfile);

        // Initialize date to today
        buttonLastDonationDate.setText(dateFormat.format(selectedDate.getTime()));

        // Initially hide confirmation card
        confirmationCard.setVisibility(View.GONE);
    }

    /**
     * Sets up the blood group spinner with 8 blood types.
     */
    private void setupBloodGroupSpinner() {
        String[] bloodGroups = {"Select Blood Group", "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                bloodGroups
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(adapter);
    }

    /**
     * Sets up click listeners for buttons and image views.
     */
    private void setupActions() {
        imageViewLogout.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> SessionManager.logoutAndOpenLogin(this))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show());

        imageViewProfile.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));

        buttonLastDonationDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        selectedDate.set(year, month, dayOfMonth);
                        buttonLastDonationDate.setText(dateFormat.format(selectedDate.getTime()));
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });

        buttonSaveProfile.setOnClickListener(v -> saveProfile());
    }

    /**
     * Sets up back press handling to show exit confirmation dialog.
     */
    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new AlertDialog.Builder(DonorDashboardActivity.this)
                        .setTitle("Exit")
                        .setMessage("Do you want to exit the app?")
                        .setPositiveButton("Exit", (dialog, which) -> finishAffinity())
                        .setNegativeButton("Stay", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        });
    }

    /**
     * Loads existing donor profile from Firestore if it exists.
     */
    private void loadDonorProfile() {
        firebaseFirestore.collection("donors").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        return;
                    }
                    editTextFullName.setText(snapshot.getString("fullName"));
                    editTextCity.setText(snapshot.getString("city"));
                    editTextPhoneNumber.setText(snapshot.getString("phoneNumber"));

                    String bloodGroup = snapshot.getString("bloodGroup");
                    if (!TextUtils.isEmpty(bloodGroup)) {
                        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerBloodGroup.getAdapter();
                        spinnerBloodGroup.setSelection(Math.max(0, adapter.getPosition(bloodGroup)));
                    }

                    String date = snapshot.getString("lastDonationDate");
                    if (!TextUtils.isEmpty(date)) {
                        buttonLastDonationDate.setText(date);
                    }

                    Boolean isAvailable = snapshot.getBoolean("isAvailable");
                    switchAvailable.setChecked(isAvailable != null && isAvailable);

                    Boolean organ = snapshot.getBoolean("isOrganDonor");
                    checkBoxOrganDonor.setChecked(organ != null && organ);

                    if (isAvailable != null && isAvailable) {
                        showConfirmationCard();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    /**
     * Saves the donor profile to Firestore.
     */
    private void saveProfile() {
        if (!validate()) {
            return;
        }

        setSavingState(true);
        String selectedBloodGroup = String.valueOf(spinnerBloodGroup.getSelectedItem());

        Map<String, Object> donorData = new HashMap<>();
        donorData.put("fullName", editTextFullName.getText().toString().trim());
        donorData.put("bloodGroup", selectedBloodGroup);
        donorData.put("city", editTextCity.getText().toString().trim());
        donorData.put("phoneNumber", editTextPhoneNumber.getText().toString().trim());
        donorData.put("lastDonationDate", buttonLastDonationDate.getText().toString());
        donorData.put("isAvailable", true);
        donorData.put("isOrganDonor", checkBoxOrganDonor.isChecked());

        firebaseFirestore.collection("donors").document(currentUser.getUid())
                .set(donorData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    setSavingState(false);
                    switchAvailable.setChecked(true);
                    showConfirmationCard();
                    Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    setSavingState(false);
                    Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Validates all input fields.
     *
     * @return true if all inputs are valid, false otherwise
     */
    private boolean validate() {
        // Validate full name
        String fullName = editTextFullName.getText().toString().trim();
        if (TextUtils.isEmpty(fullName)) {
            editTextFullName.setError("Full name is required");
            editTextFullName.requestFocus();
            return false;
        }

        if (fullName.length() < 2) {
            editTextFullName.setError("Full name should be at least 2 characters");
            editTextFullName.requestFocus();
            return false;
        }

        // Validate blood group
        String selectedBloodGroup = (String) spinnerBloodGroup.getSelectedItem();
        if (selectedBloodGroup.equals("Select Blood Group")) {
            Toast.makeText(this, "Please select a blood group", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate city
        String city = editTextCity.getText().toString().trim();
        if (TextUtils.isEmpty(city)) {
            editTextCity.setError("City is required");
            editTextCity.requestFocus();
            return false;
        }

        // Validate phone number
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();
        if (TextUtils.isEmpty(phoneNumber)) {
            editTextPhoneNumber.setError("Phone number is required");
            editTextPhoneNumber.requestFocus();
            return false;
        }

        if (phoneNumber.length() < 10) {
            editTextPhoneNumber.setError("Please enter a valid phone number");
            editTextPhoneNumber.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Shows the confirmation card for active donor registration.
     */
    private void showConfirmationCard() {
        confirmationCard.setVisibility(View.VISIBLE);
        textViewConfirmationMessage.setText("You are registered as an active donor");
    }

    private void setSavingState(boolean saving) {
        progressSave.setVisibility(saving ? View.VISIBLE : View.GONE);
        buttonSaveProfile.setEnabled(!saving);
    }

    /**
     * Hides the confirmation card.
     */
    private void hideConfirmationCard() {
        confirmationCard.setVisibility(View.GONE);
    }
}

