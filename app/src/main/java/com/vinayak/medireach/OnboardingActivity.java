package com.vinayak.medireach;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.Arrays;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    public static final String PREFS = "medireach_preferences";
    public static final String KEY_ONBOARDING_DONE = "onboarding_done";

    private ViewPager2 viewPager;
    private TextView textViewTitle;
    private TextView textViewSubtitle;
    private Button buttonNext;
    private Button buttonSkip;

    private final List<String> titles = Arrays.asList(
            "Find Hospitals Near You",
            "Real-Time Resource Availability",
            "Connect with Donors"
    );

    private final List<String> subtitles = Arrays.asList(
            "Quickly discover nearby hospitals and emergency contacts.",
            "Check ICU, oxygen, ventilator and blood unit updates instantly.",
            "Reach active donors and improve response during emergencies."
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPagerOnboarding);
        textViewTitle = findViewById(R.id.textViewOnboardingTitle);
        textViewSubtitle = findViewById(R.id.textViewOnboardingSubtitle);
        buttonNext = findViewById(R.id.buttonOnboardingNext);
        buttonSkip = findViewById(R.id.buttonOnboardingSkip);

        viewPager.setAdapter(new OnboardingPagerAdapter());
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateHeader(position);
            }
        });

        buttonSkip.setOnClickListener(v -> finishOnboarding());
        buttonNext.setOnClickListener(v -> {
            int next = viewPager.getCurrentItem() + 1;
            if (next >= 3) {
                finishOnboarding();
            } else {
                viewPager.setCurrentItem(next, true);
            }
        });

        updateHeader(0);
    }

    private void updateHeader(int index) {
        textViewTitle.setText(titles.get(index));
        textViewSubtitle.setText(subtitles.get(index));
        if (index == 2) {
            buttonNext.setText("Start");
        } else {
            buttonNext.setText("Next");
        }
    }

    private void finishOnboarding() {
        SharedPreferences preferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private class OnboardingPagerAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder> {

        @Override
        public PageViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = getLayoutInflater().inflate(R.layout.item_onboarding_page, parent, false);
            return new PageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(PageViewHolder holder, int position) {
            holder.bind(position);
        }

        @Override
        public int getItemCount() {
            return 3;
        }

        class PageViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            private final TextView textViewIllustration;

            PageViewHolder(android.view.View itemView) {
                super(itemView);
                textViewIllustration = itemView.findViewById(R.id.textViewOnboardingIllustration);
            }

            void bind(int position) {
                if (position == 0) {
                    textViewIllustration.setText("HOSPITAL");
                } else if (position == 1) {
                    textViewIllustration.setText("REAL-TIME");
                } else {
                    textViewIllustration.setText("DONOR");
                }
            }
        }
    }
}


