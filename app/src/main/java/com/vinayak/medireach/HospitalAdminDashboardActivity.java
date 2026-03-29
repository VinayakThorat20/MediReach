package com.vinayak.medireach;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.vinayak.medireach.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * HospitalAdminDashboardActivity allows hospital admins to manage and publish resource availability.
 * Validates user role and updates Firestore with hospital resource data.
 */
public class HospitalAdminDashboardActivity extends AppCompatActivity {

    private EditText editTextIcuBeds;
    private EditText editTextOxygenCylinders;
    private EditText editTextVentilators;
    private EditText editTextEmergencyContact;
    private EditText editTextBloodAPositive;
    private EditText editTextBloodANegative;
    private EditText editTextBloodBPositive;
    private EditText editTextBloodBNegative;
    private EditText editTextBloodOPositive;
    private EditText editTextBloodONegative;
    private EditText editTextBloodABPositive;
    private EditText editTextBloodABNegative;
    private Button buttonSaveAndPublish;
    private Spinner spinnerQuickResource;
    private EditText editTextQuickValue;
    private Button buttonQuickUpdate;
    private TextView textViewLastUpdate;
    private TextView textViewOutdatedBanner;
    private View progressSave;
    private ImageView imageViewLogout;
    private ImageView imageViewProfile;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospital_admin_dashboard);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            SessionManager.logoutAndOpenLogin(this);
            return;
        }

        initViews();
        initToolbarActions();
        initBackPress();
        initQuickUpdateControls();

        verifyRole();
        loadExistingHospitalData();
    }

    private void initViews() {
        editTextIcuBeds = findViewById(R.id.editTextIcuBeds);
        editTextOxygenCylinders = findViewById(R.id.editTextOxygenCylinders);
        editTextVentilators = findViewById(R.id.editTextVentilators);
        editTextEmergencyContact = findViewById(R.id.editTextEmergencyContact);
        editTextBloodAPositive = findViewById(R.id.editTextBloodAPositive);
        editTextBloodANegative = findViewById(R.id.editTextBloodANegative);
        editTextBloodBPositive = findViewById(R.id.editTextBloodBPositive);
        editTextBloodBNegative = findViewById(R.id.editTextBloodBNegative);
        editTextBloodOPositive = findViewById(R.id.editTextBloodOPositive);
        editTextBloodONegative = findViewById(R.id.editTextBloodONegative);
        editTextBloodABPositive = findViewById(R.id.editTextBloodABPositive);
        editTextBloodABNegative = findViewById(R.id.editTextBloodABNegative);
        buttonSaveAndPublish = findViewById(R.id.buttonSaveAndPublish);
        spinnerQuickResource = findViewById(R.id.spinnerQuickResource);
        editTextQuickValue = findViewById(R.id.editTextQuickValue);
        buttonQuickUpdate = findViewById(R.id.buttonQuickUpdate);
        textViewLastUpdate = findViewById(R.id.textViewLastUpdate);
        textViewOutdatedBanner = findViewById(R.id.textViewOutdatedBanner);
        progressSave = findViewById(R.id.progressSave);
        imageViewLogout = findViewById(R.id.imageViewLogout);
        imageViewProfile = findViewById(R.id.imageViewProfile);

        buttonSaveAndPublish.setOnClickListener(v -> saveAndPublish());
        buttonQuickUpdate.setOnClickListener(v -> runQuickUpdate());
    }

    private void initToolbarActions() {
        imageViewLogout.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> SessionManager.logoutAndOpenLogin(this))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show());

        imageViewProfile.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));
    }

    private void initBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new AlertDialog.Builder(HospitalAdminDashboardActivity.this)
                        .setTitle("Exit")
                        .setMessage("Do you want to exit the app?")
                        .setPositiveButton("Exit", (dialog, which) -> finishAffinity())
                        .setNegativeButton("Stay", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        });
    }

    private void initQuickUpdateControls() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"ICU Beds", "Oxygen Cylinders", "Ventilators"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerQuickResource.setAdapter(adapter);
    }

    private void verifyRole() {
        firestore.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String role = snapshot.getString("role");
                    boolean isAdmin = SessionManager.ROLE_HOSPITAL_ADMIN.equalsIgnoreCase(role)
                            || "Hospital Admin".equalsIgnoreCase(role);
                    if (!isAdmin) {
                        startActivity(new Intent(this, PatientDashboardActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Role verification failed", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, PatientDashboardActivity.class));
                    finish();
                });
    }

    private void loadExistingHospitalData() {
        firestore.collection("hospitals").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        textViewLastUpdate.setText("Last update: N/A");
                        return;
                    }

                    Number icu = snapshot.getLong("icuBeds");
                    Number oxygen = snapshot.getLong("oxygenCylinders");
                    Number vent = snapshot.getLong("ventilators");
                    String contact = snapshot.getString("emergencyContact");
                    Map<String, Long> blood = (Map<String, Long>) snapshot.get("bloodUnits");
                    Timestamp timestamp = snapshot.getTimestamp("lastUpdated");

                    editTextIcuBeds.setText(String.valueOf(icu == null ? 0 : icu.intValue()));
                    editTextOxygenCylinders.setText(String.valueOf(oxygen == null ? 0 : oxygen.intValue()));
                    editTextVentilators.setText(String.valueOf(vent == null ? 0 : vent.intValue()));
                    editTextEmergencyContact.setText(contact == null ? "" : contact);

                    setBloodValue(editTextBloodAPositive, blood, "A+");
                    setBloodValue(editTextBloodANegative, blood, "A-");
                    setBloodValue(editTextBloodBPositive, blood, "B+");
                    setBloodValue(editTextBloodBNegative, blood, "B-");
                    setBloodValue(editTextBloodOPositive, blood, "O+");
                    setBloodValue(editTextBloodONegative, blood, "O-");
                    setBloodValue(editTextBloodABPositive, blood, "AB+");
                    setBloodValue(editTextBloodABNegative, blood, "AB-");

                    if (timestamp != null) {
                        Date last = timestamp.toDate();
                        textViewLastUpdate.setText("Last update: " + new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(last));
                        long diff = System.currentTimeMillis() - last.getTime();
                        if (diff > 24L * 60L * 60L * 1000L) {
                            textViewOutdatedBanner.setVisibility(View.VISIBLE);
                        } else {
                            textViewOutdatedBanner.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void setBloodValue(EditText target, Map<String, Long> blood, String key) {
        long value = 0;
        if (blood != null && blood.containsKey(key) && blood.get(key) != null) {
            value = blood.get(key);
        }
        target.setText(String.valueOf(value));
    }

    private void saveAndPublish() {
        if (!validateAllFields()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Confirm Publication")
                .setMessage("Are you sure you want to publish this data?")
                .setPositiveButton("Yes", (dialog, which) -> publishAllData())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private boolean validateAllFields() {
        EditText[] fields = {
                editTextIcuBeds, editTextOxygenCylinders, editTextVentilators,
                editTextBloodAPositive, editTextBloodANegative, editTextBloodBPositive,
                editTextBloodBNegative, editTextBloodOPositive, editTextBloodONegative,
                editTextBloodABPositive, editTextBloodABNegative
        };

        for (EditText field : fields) {
            if (!isValidNonNegativeNumber(field)) {
                field.setError("Enter a valid non-negative number");
                field.requestFocus();
                return false;
            }
        }

        if (TextUtils.isEmpty(editTextEmergencyContact.getText())) {
            editTextEmergencyContact.setError("Emergency contact is required");
            editTextEmergencyContact.requestFocus();
            return false;
        }
        return true;
    }

    private void publishAllData() {
        setSavingState(true);
        Map<String, Object> data = new HashMap<>();
        data.put("hospitalId", currentUser.getUid());
        data.put("adminUid", currentUser.getUid());
        data.put("icuBeds", parseInt(editTextIcuBeds));
        data.put("oxygenCylinders", parseInt(editTextOxygenCylinders));
        data.put("ventilators", parseInt(editTextVentilators));
        data.put("emergencyContact", editTextEmergencyContact.getText().toString().trim());

        Map<String, Integer> blood = new HashMap<>();
        blood.put("A+", parseInt(editTextBloodAPositive));
        blood.put("A-", parseInt(editTextBloodANegative));
        blood.put("B+", parseInt(editTextBloodBPositive));
        blood.put("B-", parseInt(editTextBloodBNegative));
        blood.put("O+", parseInt(editTextBloodOPositive));
        blood.put("O-", parseInt(editTextBloodONegative));
        blood.put("AB+", parseInt(editTextBloodABPositive));
        blood.put("AB-", parseInt(editTextBloodABNegative));
        data.put("bloodUnits", blood);
        data.put("lastUpdated", FieldValue.serverTimestamp());

        firestore.collection("hospitals").document(currentUser.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    setSavingState(false);
                    Toast.makeText(this, "Data updated successfully", Toast.LENGTH_SHORT).show();
                    loadExistingHospitalData();
                })
                .addOnFailureListener(e -> {
                    setSavingState(false);
                    Toast.makeText(this, "Failed to update data", Toast.LENGTH_SHORT).show();
                });
    }

    private void runQuickUpdate() {
        if (!isValidNonNegativeNumber(editTextQuickValue)) {
            editTextQuickValue.setError("Enter a valid value");
            return;
        }
        String key;
        String selected = String.valueOf(spinnerQuickResource.getSelectedItem());
        if ("Oxygen Cylinders".equals(selected)) {
            key = "oxygenCylinders";
        } else if ("Ventilators".equals(selected)) {
            key = "ventilators";
        } else {
            key = "icuBeds";
        }

        Map<String, Object> quickData = new HashMap<>();
        quickData.put(key, parseInt(editTextQuickValue));
        quickData.put("lastUpdated", FieldValue.serverTimestamp());

        setSavingState(true);
        firestore.collection("hospitals").document(currentUser.getUid())
                .set(quickData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    setSavingState(false);
                    Toast.makeText(this, "Quick update saved", Toast.LENGTH_SHORT).show();
                    loadExistingHospitalData();
                })
                .addOnFailureListener(e -> {
                    setSavingState(false);
                    Toast.makeText(this, "Quick update failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void setSavingState(boolean saving) {
        progressSave.setVisibility(saving ? View.VISIBLE : View.GONE);
        buttonSaveAndPublish.setEnabled(!saving);
        buttonQuickUpdate.setEnabled(!saving);
    }

    private boolean isValidNonNegativeNumber(EditText field) {
        String value = field.getText().toString().trim();
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        try {
            return Integer.parseInt(value) >= 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private int parseInt(EditText field) {
        String value = field.getText().toString().trim();
        return TextUtils.isEmpty(value) ? 0 : Integer.parseInt(value);
    }
}
