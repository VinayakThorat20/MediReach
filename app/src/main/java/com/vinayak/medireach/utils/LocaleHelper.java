package com.vinayak.medireach.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

/**
 * LocaleHelper utility class for managing app language and locale settings.
 * Handles language changes and persistence using SharedPreferences.
 */
public class LocaleHelper {

    private static final String PREFERENCES_NAME = "medireach_preferences";
    private static final String LANGUAGE_KEY = "app_language";
    private static final String DEFAULT_LANGUAGE = "en"; // English as default

    /**
     * Sets the app language and updates the locale.
     * Saves the selected language to SharedPreferences.
     *
     * @param context      The application context
     * @param languageCode The language code (e.g., "en", "hi", "es")
     */
    public static void setLanguage(Context context, String languageCode) {
        // Save language to SharedPreferences
        saveLanguage(context, languageCode);

        // Update app locale
        updateLocale(context, languageCode);
    }

    /**
     * Retrieves the previously saved language from SharedPreferences.
     * Returns the default language if no language has been saved.
     *
     * @param context The application context
     * @return The saved language code, or default language if none exists
     */
    public static String getLanguage(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return preferences.getString(LANGUAGE_KEY, DEFAULT_LANGUAGE);
    }

    /**
     * Saves the language preference to SharedPreferences.
     *
     * @param context      The application context
     * @param languageCode The language code to save
     */
    private static void saveLanguage(Context context, String languageCode) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(LANGUAGE_KEY, languageCode);
        editor.apply();
    }

    /**
     * Updates the app locale to the specified language.
     * Uses API-level specific methods for compatibility.
     *
     * @param context      The application context
     * @param languageCode The language code to set
     */
    private static void updateLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration configuration = new Configuration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33) and above
            configuration.setLocale(locale);
        } else {
            // Below Android 13
            configuration.locale = locale;
        }

        context.getResources().updateConfiguration(configuration, context.getResources().getDisplayMetrics());
    }

    /**
     * Applies the saved language setting to the context.
     * Should be called in onCreate() or when the app starts.
     *
     * @param context The application context
     */
    public static void applyLanguage(Context context) {
        String savedLanguage = getLanguage(context);
        updateLocale(context, savedLanguage);
    }
}

