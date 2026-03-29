package com.vinayak.medireach.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.firebase.auth.FirebaseAuth;
import com.vinayak.medireach.DonorDashboardActivity;
import com.vinayak.medireach.HospitalSetupActivity;
import com.vinayak.medireach.HospitalAdminDashboardActivity;
import com.vinayak.medireach.LoginActivity;
import com.vinayak.medireach.PatientDashboardActivity;
import com.vinayak.medireach.RoleSelectionActivity;

public final class SessionManager {

    public static final String PREFS_NAME = "medireach_preferences";
    public static final String KEY_USER_ROLE = "user_role";
    public static final String KEY_HOSPITAL_SETUP_DONE = "hospital_setup_done";
    public static final String KEY_HOSPITAL_NAME = "hospital_name";
    public static final String ROLE_PATIENT = "patient";
    public static final String ROLE_HOSPITAL_ADMIN = "hospital_admin";
    public static final String ROLE_DONOR = "donor";

    private SessionManager() {
    }

    public static void saveRole(Context context, String role) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        if (TextUtils.isEmpty(role)) {
            editor.remove(KEY_USER_ROLE);
        } else {
            editor.putString(KEY_USER_ROLE, normalizeRole(role));
        }
        editor.apply();
    }

    public static String getSavedRole(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getString(KEY_USER_ROLE, "");
    }

    public static Intent intentForRole(Context context, String role) {
        String normalized = normalizeRole(role);
        if (ROLE_PATIENT.equals(normalized)) {
            return new Intent(context, PatientDashboardActivity.class);
        }
        if (ROLE_HOSPITAL_ADMIN.equals(normalized)) {
            if (!isHospitalSetupDone(context)) {
                return new Intent(context, HospitalSetupActivity.class);
            }
            return new Intent(context, HospitalAdminDashboardActivity.class);
        }
        if (ROLE_DONOR.equals(normalized)) {
            return new Intent(context, DonorDashboardActivity.class);
        }
        return new Intent(context, RoleSelectionActivity.class);
    }

    public static String normalizeRole(String role) {
        if (TextUtils.isEmpty(role)) {
            return "";
        }
        String normalized = role.trim().toLowerCase();
        if ("patient / public".equals(normalized) || "patient".equals(normalized)) {
            return ROLE_PATIENT;
        }
        if ("hospital admin".equals(normalized) || "hospital / admin".equals(normalized)) {
            return ROLE_HOSPITAL_ADMIN;
        }
        if ("donor".equals(normalized)) {
            return ROLE_DONOR;
        }
        return normalized;
    }

    public static void logoutAndOpenLogin(android.app.Activity activity) {
        FirebaseAuth.getInstance().signOut();
        SharedPreferences preferences = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().remove(KEY_USER_ROLE).apply();

        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finishAffinity();
        activity.finish();
    }

    public static boolean isHospitalSetupDone(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_HOSPITAL_SETUP_DONE, false);
    }

    public static void setHospitalSetupDone(Context context, boolean done) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_HOSPITAL_SETUP_DONE, done).apply();
    }

    public static void saveHospitalName(Context context, String name) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        if (TextUtils.isEmpty(name)) {
            editor.remove(KEY_HOSPITAL_NAME);
        } else {
            editor.putString(KEY_HOSPITAL_NAME, name.trim());
        }
        editor.apply();
    }
}

