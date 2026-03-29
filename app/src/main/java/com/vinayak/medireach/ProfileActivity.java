package com.vinayak.medireach;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.vinayak.medireach.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView textEmail;
    private TextView textRole;
    private TextView textRegistrationDate;
    private TextView textDonorSummary;
    private TextView textAppVersion;
    private EditText editDisplayName;
    private Button buttonSaveName;
    private Button buttonEditProfile;
    private Button buttonChangeLanguage;
    private Button buttonLogout;
    private Button buttonDeleteAccount;
    private ImageView imageViewBack;

    private FirebaseFirestore firestore;
    private FirebaseUser user;
    private String currentRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        if (user == null) {
            SessionManager.logoutAndOpenLogin(this);
            return;
        }

        setContentView(R.layout.activity_profile);
        firestore = FirebaseFirestore.getInstance();

        initViews();
        bindBaseData();
        bindActions();
        loadUserData();

        if (getIntent().getBooleanExtra("retry_delete", false)) {
            // Retry account deletion automatically after re-login.
            deleteUserAccount();
        }
    }

    private void initViews() {
        textEmail = findViewById(R.id.textViewProfileEmail);
        textRole = findViewById(R.id.textViewProfileRole);
        textRegistrationDate = findViewById(R.id.textViewProfileRegistrationDate);
        textDonorSummary = findViewById(R.id.textViewDonorSummary);
        textAppVersion = findViewById(R.id.textViewAppVersion);
        editDisplayName = findViewById(R.id.editTextProfileDisplayName);
        buttonSaveName = findViewById(R.id.buttonSaveDisplayName);
        buttonEditProfile = findViewById(R.id.buttonEditProfile);
        buttonChangeLanguage = findViewById(R.id.buttonChangeLanguage);
        buttonLogout = findViewById(R.id.buttonProfileLogout);
        buttonDeleteAccount = findViewById(R.id.buttonDeleteAccount);
        imageViewBack = findViewById(R.id.imageViewProfileBack);
    }

    private void bindBaseData() {
        textEmail.setText(user.getEmail() == null ? "N/A" : user.getEmail());
        currentRole = SessionManager.normalizeRole(SessionManager.getSavedRole(this));
        textRole.setText(TextUtils.isEmpty(currentRole) ? "N/A" : currentRole);
        textRegistrationDate.setText("Registration date: N/A");
        textAppVersion.setText("App version: 1.0");
    }

    private void bindActions() {
        imageViewBack.setOnClickListener(v -> finish());
        buttonSaveName.setOnClickListener(v -> saveName());
        buttonEditProfile.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));
        buttonChangeLanguage.setOnClickListener(v -> startActivity(new Intent(this, LanguageSelectionActivity.class)));
        buttonLogout.setOnClickListener(v -> showLogoutDialog());
        buttonDeleteAccount.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void loadUserData() {
        firestore.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        return;
                    }
                    String role = SessionManager.normalizeRole(snapshot.getString("role"));
                    String fullName = snapshot.getString("fullName");
                    Timestamp createdAt = snapshot.getTimestamp("createdAt");

                    if (!TextUtils.isEmpty(role)) {
                        currentRole = role;
                        textRole.setText(role);
                        SessionManager.saveRole(this, role);
                    }
                    if (!TextUtils.isEmpty(fullName)) {
                        editDisplayName.setText(fullName);
                    }
                    if (createdAt != null) {
                        String date = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(createdAt.toDate());
                        textRegistrationDate.setText("Registration date: " + date);
                    }

                    if (SessionManager.ROLE_DONOR.equals(currentRole)) {
                        loadDonorSummary();
                    }
                });
    }

    private void loadDonorSummary() {
        firestore.collection("donors").document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        return;
                    }
                    textDonorSummary.setVisibility(View.VISIBLE);
                    String blood = valueOrNa(snapshot.getString("bloodGroup"));
                    String city = valueOrNa(snapshot.getString("city"));
                    boolean available = Boolean.TRUE.equals(snapshot.getBoolean("isAvailable"));
                    textDonorSummary.setText("Donor summary: " + blood + " | " + city + " | Active: " + available);
                });
    }

    private void saveName() {
        String fullName = editDisplayName.getText().toString().trim();
        if (TextUtils.isEmpty(fullName)) {
            editDisplayName.setError("Display name is required");
            return;
        }

        buttonSaveName.setEnabled(false);

        Map<String, Object> map = new HashMap<>();
        map.put("fullName", fullName);

        firestore.collection("users").document(user.getUid())
                .set(map, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    buttonSaveName.setEnabled(true);
                    Toast.makeText(this, "Name updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    buttonSaveName.setEnabled(true);
                    Toast.makeText(this, "Failed to update name", Toast.LENGTH_SHORT).show();
                });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> SessionManager.logoutAndOpenLogin(this))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showDeleteConfirmationDialog() {
        if (SessionManager.ROLE_HOSPITAL_ADMIN.equals(currentRole)) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Account Permanently?")
                    .setMessage("This will also remove your hospital from the patient search. Are you absolutely sure?")
                    .setPositiveButton("DELETE", (dialog, which) -> deleteUserAccount())
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Account Permanently?")
                .setMessage("This will permanently delete your account and all your data from MediReach. This action cannot be undone.")
                .setPositiveButton("DELETE", (dialog, which) -> deleteUserAccount())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteUserAccount() {
        if (user == null) {
            return;
        }

        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Please wait")
                .setMessage("Deleting your account...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        String uid = user.getUid();
        String role = SessionManager.normalizeRole(SessionManager.getSavedRole(this));

        if (TextUtils.isEmpty(role)) {
            role = currentRole;
        }

        performRoleBasedFirestoreDelete(uid, role, progressDialog);
    }

    private void performRoleBasedFirestoreDelete(String uid, String role, AlertDialog progressDialog) {
        if (SessionManager.ROLE_HOSPITAL_ADMIN.equals(role)) {
            firestore.collection("users").document(uid).delete()
                    .addOnSuccessListener(unused -> firestore.collection("hospitals").document(uid).delete()
                            .addOnSuccessListener(unused2 -> deleteFirebaseAuthAccount(progressDialog))
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(this, "Failed to delete hospital data", Toast.LENGTH_SHORT).show();
                            }))
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Failed to delete user data", Toast.LENGTH_SHORT).show();
                    });
            return;
        }

        if (SessionManager.ROLE_DONOR.equals(role)) {
            firestore.collection("users").document(uid).delete()
                    .addOnSuccessListener(unused -> firestore.collection("donors").document(uid).delete()
                            .addOnSuccessListener(unused2 -> deleteFirebaseAuthAccount(progressDialog))
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(this, "Failed to delete donor data", Toast.LENGTH_SHORT).show();
                            }))
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Failed to delete user data", Toast.LENGTH_SHORT).show();
                    });
            return;
        }

        firestore.collection("users").document(uid).delete()
                .addOnSuccessListener(unused -> deleteFirebaseAuthAccount(progressDialog))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to delete user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteFirebaseAuthAccount(AlertDialog progressDialog) {
        if (user == null) {
            progressDialog.dismiss();
            return;
        }

        user.delete()
                .addOnSuccessListener(unused -> {
                    progressDialog.dismiss();
                    SharedPreferences prefs = getSharedPreferences(SessionManager.PREFS_NAME, MODE_PRIVATE);
                    prefs.edit().clear().apply();

                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finishAffinity();
                    Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                        showReauthRequiredDialog();
                    } else {
                        Toast.makeText(this, "Failed to delete authentication account", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showReauthRequiredDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Re-authentication required")
                .setMessage("Please login again to confirm account deletion")
                .setPositiveButton("Login Again", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.putExtra("require_reauth", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private String valueOrNa(String value) {
        return TextUtils.isEmpty(value) ? "N/A" : value;
    }
}
