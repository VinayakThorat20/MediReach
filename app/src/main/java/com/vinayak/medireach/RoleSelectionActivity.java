package com.vinayak.medireach;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vinayak.medireach.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

/**
 * RoleSelectionActivity allows users to select their role in the MediReach app.
 * Users can choose between Patient, Hospital Admin, or Donor.
 */
public class RoleSelectionActivity extends AppCompatActivity {

    private static final String TAG = "RoleSelectionActivity";
    private static final String USERS_COLLECTION = "users";
    private static final String ROLE_FIELD = "role";

    // Role constants
    private static final String ROLE_PATIENT = SessionManager.ROLE_PATIENT;
    private static final String ROLE_HOSPITAL_ADMIN = SessionManager.ROLE_HOSPITAL_ADMIN;
    private static final String ROLE_DONOR = SessionManager.ROLE_DONOR;

    private CardView cardPatient;
    private CardView cardHospitalAdmin;
    private CardView cardDonor;
    private ImageView buttonLogout;
    private ImageView buttonProfile;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        // Check if user is authenticated
        if (currentUser == null) {
            Log.w(TAG, "User is not authenticated. Redirecting to login.");
            navigateToLoginActivity();
            return;
        }

        // Initialize views
        initializeViews();

        // Set up click listeners
        setUpClickListeners();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitDialog();
            }
        });
    }

    /**
     * Initializes all UI views from the layout.
     */
    private void initializeViews() {
        cardPatient = findViewById(R.id.cardPatient);
        cardHospitalAdmin = findViewById(R.id.cardHospitalAdmin);
        cardDonor = findViewById(R.id.cardDonor);
        buttonLogout = findViewById(R.id.imageViewLogout);
        buttonProfile = findViewById(R.id.imageViewProfile);
    }

    /**
     * Sets up click listeners for role cards.
     */
    private void setUpClickListeners() {
        cardPatient.setOnClickListener(v -> selectRole(ROLE_PATIENT));
        cardHospitalAdmin.setOnClickListener(v -> selectRole(ROLE_HOSPITAL_ADMIN));
        cardDonor.setOnClickListener(v -> selectRole(ROLE_DONOR));
        buttonLogout.setOnClickListener(v -> showLogoutDialog());
        buttonProfile.setOnClickListener(v -> startActivity(new Intent(this, com.vinayak.medireach.ProfileActivity.class)));
    }

    /**
     * Handles role selection, saves it to Firestore, and navigates to the appropriate dashboard.
     *
     * @param role The selected role
     */
    private void selectRole(String role) {
        Log.d(TAG, "Role selected: " + role);
        saveRoleToFirestore(role);
    }

    private void saveRoleToFirestore(String role) {
        String userId = currentUser.getUid();

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put(ROLE_FIELD, role);

        firebaseFirestore.collection(USERS_COLLECTION).document(userId)
                .set(updateMap, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Role saved to Firestore: " + role);
                    getSharedPreferences("MediReachPrefs", MODE_PRIVATE)
                            .edit()
                            .putString("user_role", role)
                            .apply();
                    navigateByRole(role);
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Failed to save role: " + exception.getMessage());
                    Toast.makeText(RoleSelectionActivity.this,
                            "Failed to save role. Please try again.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateByRole(String role) {
        Intent intent;
        if ("hospital_admin".equals(role)) {
            boolean setupDone = getSharedPreferences("MediReachPrefs", MODE_PRIVATE)
                    .getBoolean("hospital_setup_done", false);
            if (setupDone) {
                intent = new Intent(this, HospitalAdminDashboardActivity.class);
            } else {
                intent = new Intent(this, HospitalSetupActivity.class);
            }
        } else if ("patient".equals(role)) {
            intent = new Intent(this, PatientDashboardActivity.class);
        } else if ("donor".equals(role)) {
            intent = new Intent(this, DonorDashboardActivity.class);
        } else {
            intent = new Intent(this, RoleSelectionActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> SessionManager.logoutAndOpenLogin(this))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Exit")
                .setMessage("Do you want to exit the app?")
                .setPositiveButton("Exit", (dialog, which) -> finishAffinity())
                .setNegativeButton("Stay", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Navigates back to LoginActivity when user is not authenticated.
     */
    private void navigateToLoginActivity() {
        Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

