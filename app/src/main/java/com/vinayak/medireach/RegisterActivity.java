package com.vinayak.medireach;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.vinayak.medireach.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

/**
 * RegisterActivity handles user registration using Firebase Auth and Firestore.
 * Allows users to create a new account with email and password.
 */
public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private static final String USERS_COLLECTION = "users";
    private static final String EXTRA_SELECTED_ROLE = "selected_role";

    private EditText editTextFullName;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private EditText editTextConfirmPassword;
    private Button buttonRegister;
    private TextView textViewLogin;
    private CardView errorCardView;
    private TextView errorMessageTextView;
    private TextView textViewFullNameError;
    private TextView textViewEmailError;
    private TextView textViewPasswordError;
    private TextView textViewConfirmPasswordError;
    private TextView textViewPasswordStrength;
    private ProgressBar progressBarRegister;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Set up click listeners
        setUpClickListeners();
    }

    /**
     * Initializes all UI views from the layout.
     */
    private void initializeViews() {
        editTextFullName = findViewById(R.id.editTextFullName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLogin = findViewById(R.id.textViewLogin);
        errorCardView = findViewById(R.id.cardViewError);
        errorMessageTextView = findViewById(R.id.textViewErrorMessage);
        textViewFullNameError = findViewById(R.id.textViewFullNameError);
        textViewEmailError = findViewById(R.id.textViewEmailError);
        textViewPasswordError = findViewById(R.id.textViewPasswordError);
        textViewConfirmPasswordError = findViewById(R.id.textViewConfirmPasswordError);
        textViewPasswordStrength = findViewById(R.id.textViewPasswordStrength);
        progressBarRegister = findViewById(R.id.progressBarRegister);

        // Initially hide error card
        errorCardView.setVisibility(android.view.View.GONE);

        editTextPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textViewPasswordStrength.setText("Password strength: " + getPasswordStrength(s == null ? "" : s.toString()));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /**
     * Sets up click listeners for buttons and text views.
     */
    private void setUpClickListeners() {
        buttonRegister.setOnClickListener(v -> performRegistration());
        textViewLogin.setOnClickListener(v -> navigateToLoginActivity());
    }

    /**
     * Performs registration with input validation.
     */
    private void performRegistration() {
        String fullName = editTextFullName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(fullName, email, password, confirmPassword)) {
            return;
        }

        // Hide error card before attempting registration
        hideErrorCard();

        // Disable register button to prevent multiple clicks
        setLoading(true);

        // Create user with Firebase Auth
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "User registration successful for: " + email);
                    saveUserToFirestore(fullName, email, authResult.getUser());
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Registration failed: " + exception.getMessage());
                    handleRegistrationFailure(exception);
                    setLoading(false);
                });
    }

    /**
     * Validates all input fields.
     *
     * @param fullName        The full name entered by user
     * @param email           The email entered by user
     * @param password        The password entered by user
     * @param confirmPassword The confirmation password entered by user
     * @return true if all inputs are valid, false otherwise
     */
    private boolean validateInputs(String fullName, String email, String password, String confirmPassword) {
        textViewFullNameError.setVisibility(android.view.View.GONE);
        textViewEmailError.setVisibility(android.view.View.GONE);
        textViewPasswordError.setVisibility(android.view.View.GONE);
        textViewConfirmPasswordError.setVisibility(android.view.View.GONE);

        if (TextUtils.isEmpty(fullName)) {
            textViewFullNameError.setText("Full name is required");
            textViewFullNameError.setVisibility(android.view.View.VISIBLE);
            editTextFullName.requestFocus();
            return false;
        }

        if (fullName.length() < 2) {
            textViewFullNameError.setText("Full name should be at least 2 characters");
            textViewFullNameError.setVisibility(android.view.View.VISIBLE);
            editTextFullName.requestFocus();
            return false;
        }

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

        if (TextUtils.isEmpty(confirmPassword)) {
            textViewConfirmPasswordError.setText("Please confirm your password");
            textViewConfirmPasswordError.setVisibility(android.view.View.VISIBLE);
            editTextConfirmPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            textViewConfirmPasswordError.setText("Passwords do not match");
            textViewConfirmPasswordError.setVisibility(android.view.View.VISIBLE);
            editTextConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Saves the user data to Firestore after successful authentication.
     *
     * @param fullName The full name of the user
     * @param email    The email of the user
     * @param userId   The unique user ID from Firebase Auth
     */
    private void saveUserToFirestore(String fullName, String email, FirebaseUser user) {
        String userId = user.getUid();
        String selectedRole = SessionManager.normalizeRole(getIntent().getStringExtra(EXTRA_SELECTED_ROLE));
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("email", email);
        userMap.put("fullName", fullName);
        userMap.put("role", selectedRole == null ? "" : selectedRole);
        userMap.put("createdAt", com.google.firebase.Timestamp.now());

        firebaseFirestore.collection(USERS_COLLECTION).document(userId)
                .set(userMap, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User document saved to Firestore for: " + email);
                    SessionManager.saveRole(this, selectedRole);
                    navigateByRole(selectedRole);
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Failed to save user document: " + exception.getMessage());
                    showErrorCard();
                    errorMessageTextView.setText("Registration successful but failed to create profile. Please try again.");
                    setLoading(false);
                });
    }

    /**
     * Handles registration failure and displays appropriate error message.
     *
     * @param exception The exception thrown during registration
     */
    private void handleRegistrationFailure(Exception exception) {
        showErrorCard();

        String errorMessage = exception.getMessage();
        if (errorMessage != null) {
            if (errorMessage.contains("email already in use")) {
                errorMessageTextView.setText("Email already registered. Please login or use a different email.");
            } else if (errorMessage.contains("The email address is badly formatted")) {
                errorMessageTextView.setText("Invalid email format. Please check and try again.");
            } else {
                errorMessageTextView.setText("Registration failed. Please try again.");
            }
        } else {
            errorMessageTextView.setText("Registration failed. Please try again.");
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
     * Navigates to RoleSelectionActivity on successful registration.
     */
    private void navigateByRole(String role) {
        Intent intent;
        String normalizedRole = SessionManager.normalizeRole(role);
        if (TextUtils.isEmpty(normalizedRole)) {
            intent = new Intent(this, RoleSelectionActivity.class);
        } else {
            intent = SessionManager.intentForRole(this, normalizedRole);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Navigates back to LoginActivity.
     */
    private void navigateToLoginActivity() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private String getPasswordStrength(String password) {
        if (password.length() < 6) {
            return "Weak";
        }
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[^A-Za-z0-9].*");
        if (hasLetter && hasDigit && hasSpecial && password.length() >= 8) {
            return "Strong";
        }
        return "Medium";
    }

    private void setLoading(boolean loading) {
        buttonRegister.setEnabled(!loading);
        progressBarRegister.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
    }
}

