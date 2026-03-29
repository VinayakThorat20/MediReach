package com.vinayak.medireach;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vinayak.medireach.utils.SessionManager;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private View sectionPatient;
    private View sectionHospital;
    private View sectionDonor;
    private TextView textViewRoleBadge;
    private TextView textViewHospitalLocationReadonly;
    private ProgressBar progressLoad;
    private ProgressBar progressSave;

    private EditText editTextFullName;
    private EditText editTextPhone;
    private EditText editTextCity;

    private EditText editTextHospitalName;
    private EditText editTextHospitalAddress;
    private EditText editTextHospitalCity;
    private EditText editTextHospitalPincode;
    private EditText editTextHospitalEmergencyContact;

    private Spinner spinnerBloodGroup;
    private Spinner spinnerHospitalType;
    private Button buttonLastDonationDate;
    private Switch switchAvailability;
    private CheckBox checkBoxOrganDonor;

    private Button buttonSaveChanges;
    private Button buttonChangePassword;
    private Button buttonChangeEmail;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            SessionManager.logoutAndOpenLogin(this);
            return;
        }

        db = FirebaseFirestore.getInstance();

        initViews();
        setupSpinners();
        setupActions();
        loadRoleAndProfile();
    }

    private void initViews() {
        sectionPatient = findViewById(R.id.sectionPatient);
        sectionHospital = findViewById(R.id.sectionHospital);
        sectionDonor = findViewById(R.id.sectionDonor);
        textViewRoleBadge = findViewById(R.id.textViewRoleBadge);
        textViewHospitalLocationReadonly = findViewById(R.id.textViewHospitalLocationReadonly);
        progressLoad = findViewById(R.id.progressLoadEditProfile);
        progressSave = findViewById(R.id.progressSaveChanges);

        editTextFullName = findViewById(R.id.editTextFullName);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextCity = findViewById(R.id.editTextCity);

        editTextHospitalName = findViewById(R.id.editTextHospitalName);
        editTextHospitalAddress = findViewById(R.id.editTextHospitalAddress);
        editTextHospitalCity = findViewById(R.id.editTextHospitalCity);
        editTextHospitalPincode = findViewById(R.id.editTextHospitalPincode);
        editTextHospitalEmergencyContact = findViewById(R.id.editTextHospitalEmergencyContact);

        spinnerBloodGroup = findViewById(R.id.spinnerBloodGroup);
        spinnerHospitalType = findViewById(R.id.spinnerHospitalType);
        buttonLastDonationDate = findViewById(R.id.buttonLastDonationDate);
        switchAvailability = findViewById(R.id.switchAvailability);
        checkBoxOrganDonor = findViewById(R.id.checkBoxOrganDonor);

        buttonSaveChanges = findViewById(R.id.buttonSaveChanges);
        buttonChangePassword = findViewById(R.id.buttonChangePassword);
        buttonChangeEmail = findViewById(R.id.buttonChangeEmail);
    }

    private void setupSpinners() {
        String[] bloodGroups = {"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};
        ArrayAdapter<String> bloodAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                bloodGroups
        );
        bloodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(bloodAdapter);

        String[] hospitalTypes = {"Government", "Private", "Trust", "Clinic"};
        ArrayAdapter<String> hospitalTypeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                hospitalTypes
        );
        hospitalTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHospitalType.setAdapter(hospitalTypeAdapter);
    }

    private void setupActions() {
        buttonSaveChanges.setOnClickListener(v -> saveChanges());
        buttonChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        buttonChangeEmail.setOnClickListener(v -> showChangeEmailDialog());

        final Calendar calendar = Calendar.getInstance();
        buttonLastDonationDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                        buttonLastDonationDate.setText(date);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });
    }

    private void loadRoleAndProfile() {
        progressLoad.setVisibility(View.VISIBLE);
        String cachedRole = SessionManager.normalizeRole(SessionManager.getSavedRole(this));
        if (!TextUtils.isEmpty(cachedRole)) {
            role = cachedRole;
            bindRoleUI();
            loadProfileData();
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    role = SessionManager.normalizeRole(snapshot.getString("role"));
                    SessionManager.saveRole(this, role);
                    bindRoleUI();
                    loadProfileData();
                })
                .addOnFailureListener(e -> {
                    progressLoad.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load role", Toast.LENGTH_SHORT).show();
                });
    }

    private void bindRoleUI() {
        sectionPatient.setVisibility(View.GONE);
        sectionHospital.setVisibility(View.GONE);
        sectionDonor.setVisibility(View.GONE);

        if (SessionManager.ROLE_HOSPITAL_ADMIN.equals(role)) {
            textViewRoleBadge.setText("Hospital Admin");
            sectionHospital.setVisibility(View.VISIBLE);
        } else if (SessionManager.ROLE_DONOR.equals(role)) {
            textViewRoleBadge.setText("Donor");
            sectionPatient.setVisibility(View.VISIBLE);
            sectionDonor.setVisibility(View.VISIBLE);
        } else {
            textViewRoleBadge.setText("Patient");
            sectionPatient.setVisibility(View.VISIBLE);
        }
    }

    private void loadProfileData() {
        if (SessionManager.ROLE_HOSPITAL_ADMIN.equals(role)) {
            db.collection("hospitals").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        progressLoad.setVisibility(View.GONE);
                        if (!snapshot.exists()) {
                            return;
                        }
                        editTextHospitalName.setText(snapshot.getString("hospitalName"));
                        editTextHospitalAddress.setText(snapshot.getString("address"));
                        editTextHospitalCity.setText(snapshot.getString("city"));
                        editTextHospitalPincode.setText(snapshot.getString("pincode"));
                        editTextHospitalEmergencyContact.setText(snapshot.getString("emergencyContact"));

                        String hospitalType = snapshot.getString("hospitalType");
                        if (!TextUtils.isEmpty(hospitalType)) {
                            int pos = ((ArrayAdapter<String>) spinnerHospitalType.getAdapter()).getPosition(hospitalType);
                            spinnerHospitalType.setSelection(Math.max(pos, 0));
                        }

                        Double latitude = snapshot.getDouble("latitude");
                        Double longitude = snapshot.getDouble("longitude");
                        textViewHospitalLocationReadonly.setText("Location: "
                                + (latitude == null ? "N/A" : latitude)
                                + ", "
                                + (longitude == null ? "N/A" : longitude)
                                + " (Contact support to change)");
                    })
                    .addOnFailureListener(e -> {
                        progressLoad.setVisibility(View.GONE);
                        Toast.makeText(this, "Failed to load hospital profile", Toast.LENGTH_SHORT).show();
                    });
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    editTextFullName.setText(snapshot.getString("fullName"));
                    editTextPhone.setText(snapshot.getString("phoneNumber"));
                    editTextCity.setText(snapshot.getString("city"));
                    if (SessionManager.ROLE_DONOR.equals(role)) {
                        loadDonorData();
                    } else {
                        progressLoad.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressLoad.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadDonorData() {
        db.collection("donors").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    progressLoad.setVisibility(View.GONE);
                    if (!snapshot.exists()) {
                        return;
                    }
                    String bloodGroup = snapshot.getString("bloodGroup");
                    if (!TextUtils.isEmpty(bloodGroup)) {
                        int pos = ((ArrayAdapter<String>) spinnerBloodGroup.getAdapter()).getPosition(bloodGroup);
                        spinnerBloodGroup.setSelection(Math.max(0, pos));
                    }
                    buttonLastDonationDate.setText(TextUtils.isEmpty(snapshot.getString("lastDonationDate"))
                            ? "Last Donation Date"
                            : snapshot.getString("lastDonationDate"));
                    switchAvailability.setChecked(Boolean.TRUE.equals(snapshot.getBoolean("isAvailable")));
                    checkBoxOrganDonor.setChecked(Boolean.TRUE.equals(snapshot.getBoolean("isOrganDonor")));
                })
                .addOnFailureListener(e -> {
                    progressLoad.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load donor profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveChanges() {
        if (SessionManager.ROLE_HOSPITAL_ADMIN.equals(role)) {
            saveHospitalProfile();
        } else if (SessionManager.ROLE_DONOR.equals(role)) {
            saveDonorProfile();
        } else {
            savePatientProfile();
        }
    }

    private void savePatientProfile() {
        String fullName = editTextFullName.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String city = editTextCity.getText().toString().trim();

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(city)) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        setSaving(true);
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("phoneNumber", phone);
        updates.put("city", city);

        db.collection("users").document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(unused -> onSaveSuccess(fullName))
                .addOnFailureListener(e -> onSaveFailure("Failed to update profile"));
    }

    private void saveDonorProfile() {
        String fullName = editTextFullName.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String city = editTextCity.getText().toString().trim();

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(city)) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        setSaving(true);

        Map<String, Object> donorUpdates = new HashMap<>();
        donorUpdates.put("fullName", fullName);
        donorUpdates.put("city", city);
        donorUpdates.put("phoneNumber", phone);
        donorUpdates.put("bloodGroup", String.valueOf(spinnerBloodGroup.getSelectedItem()));
        donorUpdates.put("lastDonationDate", buttonLastDonationDate.getText().toString());
        donorUpdates.put("isAvailable", switchAvailability.isChecked());
        donorUpdates.put("isOrganDonor", checkBoxOrganDonor.isChecked());

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("fullName", fullName);
        userUpdates.put("city", city);
        userUpdates.put("phoneNumber", phone);

        db.collection("donors").document(currentUser.getUid())
                .update(donorUpdates)
                .addOnSuccessListener(unused -> db.collection("users").document(currentUser.getUid())
                        .update(userUpdates)
                        .addOnSuccessListener(unused2 -> onSaveSuccess(fullName))
                        .addOnFailureListener(e -> onSaveFailure("Failed to update profile")))
                .addOnFailureListener(e -> onSaveFailure("Failed to update profile"));
    }

    private void saveHospitalProfile() {
        String hospitalName = editTextHospitalName.getText().toString().trim();
        String address = editTextHospitalAddress.getText().toString().trim();
        String city = editTextHospitalCity.getText().toString().trim();
        String pincode = editTextHospitalPincode.getText().toString().trim();
        String emergencyContact = editTextHospitalEmergencyContact.getText().toString().trim();

        if (TextUtils.isEmpty(hospitalName) || TextUtils.isEmpty(address)
                || TextUtils.isEmpty(city) || TextUtils.isEmpty(pincode)
                || TextUtils.isEmpty(emergencyContact)) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        setSaving(true);
        Map<String, Object> updates = new HashMap<>();
        updates.put("hospitalName", hospitalName);
        updates.put("address", address);
        updates.put("city", city);
        updates.put("pincode", pincode);
        updates.put("emergencyContact", emergencyContact);
        updates.put("hospitalType", String.valueOf(spinnerHospitalType.getSelectedItem()));

        db.collection("hospitals").document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    SessionManager.saveHospitalName(this, hospitalName);
                    onSaveSuccess(hospitalName);
                })
                .addOnFailureListener(e -> onSaveFailure("Failed to update hospital profile"));
    }

    private void onSaveSuccess(String updatedName) {
        if (!TextUtils.isEmpty(updatedName)) {
            currentUser.updateProfile(new UserProfileChangeRequest.Builder()
                    .setDisplayName(updatedName)
                    .build());
        }
        setSaving(false);
        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void onSaveFailure(String message) {
        setSaving(false);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void setSaving(boolean saving) {
        progressSave.setVisibility(saving ? View.VISIBLE : View.GONE);
        buttonSaveChanges.setEnabled(!saving);
        buttonChangePassword.setEnabled(!saving);
        buttonChangeEmail.setEnabled(!saving);
    }

    private void showChangePasswordDialog() {
        if (currentUser.getEmail() == null) {
            Toast.makeText(this, "User email not found", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, null);
        EditText currentPassword = new EditText(this);
        currentPassword.setHint("Current Password");
        currentPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        EditText newPassword = new EditText(this);
        newPassword.setHint("New Password");
        newPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        EditText confirmPassword = new EditText(this);
        confirmPassword.setHint("Confirm New Password");
        confirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, 0);
        container.addView(currentPassword);
        container.addView(newPassword);
        container.addView(confirmPassword);

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(container)
                .setPositiveButton("Submit", (dialog, which) -> {
                    String current = currentPassword.getText().toString().trim();
                    String next = newPassword.getText().toString().trim();
                    String confirm = confirmPassword.getText().toString().trim();

                    if (next.length() < 6) {
                        Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!next.equals(confirm)) {
                        Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    currentUser.reauthenticate(EmailAuthProvider.getCredential(currentUser.getEmail(), current))
                            .addOnSuccessListener(unused -> currentUser.updatePassword(next)
                                    .addOnSuccessListener(unused2 -> Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to change password", Toast.LENGTH_SHORT).show()))
                            .addOnFailureListener(e -> Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangeEmailDialog() {
        if (currentUser.getEmail() == null) {
            Toast.makeText(this, "User email not found", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText newEmail = new EditText(this);
        newEmail.setHint("New Email");
        newEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        EditText password = new EditText(this);
        password.setHint("Current Password");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, 0);
        container.addView(newEmail);
        container.addView(password);

        new AlertDialog.Builder(this)
                .setTitle("Change Email")
                .setView(container)
                .setPositiveButton("Submit", (dialog, which) -> {
                    String updatedEmail = newEmail.getText().toString().trim();
                    String currentPassword = password.getText().toString().trim();

                    if (TextUtils.isEmpty(updatedEmail) || !android.util.Patterns.EMAIL_ADDRESS.matcher(updatedEmail).matches()) {
                        Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    currentUser.reauthenticate(EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword))
                            .addOnSuccessListener(unused -> currentUser.updateEmail(updatedEmail)
                                    .addOnSuccessListener(unused2 -> db.collection("users").document(currentUser.getUid())
                                            .update("email", updatedEmail)
                                            .addOnSuccessListener(unused3 -> Toast.makeText(this, "Email updated successfully", Toast.LENGTH_SHORT).show())
                                            .addOnFailureListener(e -> Toast.makeText(this, "Email updated in auth but Firestore update failed", Toast.LENGTH_SHORT).show()))
                                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update email", Toast.LENGTH_SHORT).show()))
                            .addOnFailureListener(e -> Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}

