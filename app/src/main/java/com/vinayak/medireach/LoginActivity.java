package com.vinayak.medireach;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vinayak.medireach.utils.SessionManager;

/**
 * LoginActivity handles user authentication using Firebase Auth.
 * Allows users to login with email and password.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonLogin;
    private TextView textViewRegister;
    private CardView errorCardView;
    private TextView errorMessageTextView;
    private TextView textViewEmailError;
    private TextView textViewPasswordError;
    private ProgressBar progressBarLogin;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private boolean requireReauth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        requireReauth = getIntent().getBooleanExtra("require_reauth", false);

        // Initialize views
        initializeViews();

        // Set up click listeners
        setUpClickListeners();
    }

    /**
     * Initializes all UI views from the layout.
     */
    private void initializeViews() {
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegister = findViewById(R.id.textViewRegister);
        errorCardView = findViewById(R.id.cardViewError);
        errorMessageTextView = findViewById(R.id.textViewErrorMessage);
        textViewEmailError = findViewById(R.id.textViewEmailError);
        textViewPasswordError = findViewById(R.id.textViewPasswordError);
        progressBarLogin = findViewById(R.id.progressBarLogin);

        // Initially hide error card
        errorCardView.setVisibility(android.view.View.GONE);
    }

    /**
     * Sets up click listeners for buttons and text views.
     */
    private void setUpClickListeners() {
        buttonLogin.setOnClickListener(v -> performLogin());
        textViewRegister.setOnClickListener(v -> navigateToRegisterActivity());
    }

    /**
     * Performs login with email and password validation.
     */
    private void performLogin() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(email, password)) {
            return;
        }

        // Hide error card before attempting login
        hideErrorCard();

        // Disable login button to prevent multiple clicks
        setLoading(true);

        // Attempt Firebase authentication
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "Login successful for user: " + email);
                    routeUserAfterLogin(authResult.getUser());
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Login failed: " + exception.getMessage());
                    handleLoginFailure(exception);
                })
                .addOnCompleteListener(task -> {
                    // Re-enable login button after task completion
                    setLoading(false);
                });
    }

    /**
     * Validates email and password inputs.
     *
     * @param email    The email entered by user
     * @param password The password entered by user
     * @return true if inputs are valid, false otherwise
     */
    private boolean validateInputs(String email, String password) {
        textViewEmailError.setVisibility(android.view.View.GONE);
        textViewPasswordError.setVisibility(android.view.View.GONE);

        if (TextUtils.isEmpty(email)) {
            textViewEmailError.setText("Email is required");
            textViewEmailError.setVisibility(android.view.View.VISIBLE);
            editTextEmail.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            textViewEmailError.setText("Please enter a valid email");
            textViewEmailError.setVisibility(android.view.View.VISIBLE);
            editTextEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            textViewPasswordError.setText("Password is required");
            textViewPasswordError.setVisibility(android.view.View.VISIBLE);
            editTextPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            textViewPasswordError.setText("Password should be at least 6 characters");
            textViewPasswordError.setVisibility(android.view.View.VISIBLE);
            editTextPassword.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Handles login failure and displays appropriate error message.
     *
     * @param exception The exception thrown during login
     */
    private void handleLoginFailure(Exception exception) {
        showErrorCard();

        if (exception instanceof FirebaseAuthInvalidUserException) {
            // User account not found
            errorMessageTextView.setText("No account found with this email.");
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            // Invalid password
            errorMessageTextView.setText("Authentication Failed. Please check credentials.");
        } else {
            // Generic error
            errorMessageTextView.setText("Authentication Failed. Please check credentials.");
        }
    }

    /**
     * Shows the error card view.
     */
    private void showErrorCard() {
        errorCardView.setVisibility(android.view.View.VISIBLE);
    }

    /**
     * Hides the error card view.
     */
    private void hideErrorCard() {
        errorCardView.setVisibility(android.view.View.GONE);
    }

    /**
     * Navigates to RoleSelectionActivity on successful login.
     */
    private void routeUserAfterLogin(FirebaseUser user) {
        if (requireReauth) {
            Intent retryDeleteIntent = new Intent(this, ProfileActivity.class);
            retryDeleteIntent.putExtra("retry_delete", true);
            retryDeleteIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(retryDeleteIntent);
            finish();
            return;
        }

        firebaseFirestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String role = snapshot.getString("role");
                    if (role == null) role = "";
                    getSharedPreferences("MediReachPrefs", MODE_PRIVATE)
                            .edit()
                            .putString("user_role", role)
                            .apply();
                    navigateByRole(role);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch role", e);
                    navigateByRole("");
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

    private void setLoading(boolean loading) {
        buttonLogin.setEnabled(!loading);
        progressBarLogin.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    /**
     * Navigates to RegisterActivity.
     */
    private void navigateToRegisterActivity() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }
}

