package com.vinayak.medireach;

import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vinayak.medireach.utils.NetworkUtil;
import com.vinayak.medireach.utils.SessionManager;

import android.view.View;

/**
 * SplashActivity displays a splash screen on app launch.
 * Handles internet connectivity check, authentication verification, and navigation.
 */
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final long SPLASH_DISPLAY_LENGTH = 2000; // 2 seconds

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        rootView = findViewById(android.R.id.content);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        // Check internet connectivity
        if (!NetworkUtil.isNetworkAvailable(this)) {
            showNoInternetSnackbar();
            scheduleNavigation(SPLASH_DISPLAY_LENGTH);
            return;
        }

        // Schedule navigation after splash delay
        scheduleNavigation(SPLASH_DISPLAY_LENGTH);
    }

    /**
     * Schedules navigation after the splash screen duration.
     *
     * @param delayMillis The delay in milliseconds
     */
    private void scheduleNavigation(long delayMillis) {
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToNextScreen, delayMillis);
    }

    /**
     * Determines the next screen based on authentication status.
     * If user is logged in, go to RoleSelectionActivity.
     * Otherwise, go to LoginActivity.
     */
    private void navigateToNextScreen() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "User is not logged in");
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        Log.d(TAG, "User is logged in: " + currentUser.getUid());
        SharedPreferences prefs = getSharedPreferences("MediReachPrefs", MODE_PRIVATE);
        String role = prefs.getString("user_role", "");

        if (!TextUtils.isEmpty(role)) {
            navigateByRole(role);
        } else {
            firebaseFirestore.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        String fetchedRole = snapshot.getString("role");
                        if (fetchedRole == null) fetchedRole = "";
                        prefs.edit().putString("user_role", fetchedRole).apply();
                        navigateByRole(fetchedRole);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to fetch role", e);
                        navigateByRole("");
                    });
        }
    }

    private void navigateByRole(String role) {
        Intent intent;
        if ("hospital_admin".equals(role)) {
            SharedPreferences prefs = getSharedPreferences("MediReachPrefs", MODE_PRIVATE);
            boolean setupDone = prefs.getBoolean("hospital_setup_done", false);
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

    /**
     * Displays a Snackbar indicating no internet connection.
     */
    private void showNoInternetSnackbar() {
        Log.w(TAG, "No internet connection detected");
        Snackbar snackbar = Snackbar.make(rootView, "No Internet Connection", Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        snackbar.show();
    }
}

