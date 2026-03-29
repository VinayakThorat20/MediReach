package com.vinayak.medireach;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vinayak.medireach.adapter.LanguageAdapter;
import com.vinayak.medireach.model.Language;
import com.vinayak.medireach.utils.LocaleHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * LanguageSelectionActivity displays a list of supported languages.
 * Shows only on first launch and allows users to select their preferred language.
 */
public class LanguageSelectionActivity extends AppCompatActivity implements LanguageAdapter.OnLanguageSelectedListener {

    private static final String TAG = "LanguageSelection";
    private static final String PREFERENCES_NAME = "medireach_preferences";
    private static final String FIRST_LAUNCH_KEY = "first_launch";

    private RecyclerView languageRecyclerView;
    private LanguageAdapter languageAdapter;
    private List<Language> languages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isFirstLaunch(this)) {
            navigateToSplashActivity();
            return;
        }

        setContentView(R.layout.activity_language_selection);

        // Apply saved language if available
        LocaleHelper.applyLanguage(this);

        initializeRecyclerView();
        loadLanguages();
    }

    /**
     * Initializes the RecyclerView with GridLayoutManager.
     */
    private void initializeRecyclerView() {
        languageRecyclerView = findViewById(R.id.recyclerViewLanguages);
        languageRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        languages = new ArrayList<>();
        languageAdapter = new LanguageAdapter(languages, this);
        languageRecyclerView.setAdapter(languageAdapter);
    }

    /**
     * Loads the list of supported languages.
     */
    private void loadLanguages() {
        languages.add(new Language("English", "en"));
        languages.add(new Language("Hindi", "hi"));
        languages.add(new Language("Marathi", "mr"));
        languages.add(new Language("Tamil", "ta"));
        languages.add(new Language("Telugu", "te"));
        languages.add(new Language("Bengali", "bn"));
        languages.add(new Language("Gujarati", "gu"));
        languages.add(new Language("Kannada", "kn"));

        languageAdapter.notifyDataSetChanged();
    }

    /**
     * Called when a language is selected from the adapter.
     * Saves the selected language and navigates to SplashActivity.
     *
     * @param language The selected language
     */
    @Override
    public void onLanguageSelected(Language language) {
        Log.d(TAG, "Language selected: " + language.getLanguageName());

        // Set the selected language using LocaleHelper
        LocaleHelper.setLanguage(this, language.getLanguageCode());

        // Mark that first launch is complete
        markFirstLaunchComplete();

        // Navigate to SplashActivity
        navigateToSplashActivity();
    }

    /**
     * Marks the first launch as complete in SharedPreferences.
     */
    private void markFirstLaunchComplete() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(FIRST_LAUNCH_KEY, false);
        editor.putBoolean(OnboardingActivity.KEY_ONBOARDING_DONE, false);
        editor.apply();
    }

    /**
     * Navigates to SplashActivity and finishes this activity.
     */
    private void navigateToSplashActivity() {
        Intent intent = new Intent(LanguageSelectionActivity.this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Checks if this is the first launch of the app.
     *
     * @param context The application context
     * @return true if first launch, false otherwise
     */
    public static boolean isFirstLaunch(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(FIRST_LAUNCH_KEY, true);
    }
}

