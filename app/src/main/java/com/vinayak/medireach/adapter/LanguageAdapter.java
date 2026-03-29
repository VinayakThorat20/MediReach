package com.vinayak.medireach.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.vinayak.medireach.R;
import com.vinayak.medireach.model.Language;

import java.util.List;

/**
 * RecyclerView adapter for displaying language options in cards.
 */
public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder> {

    private List<Language> languages;
    private OnLanguageSelectedListener listener;

    /**
     * Interface for language selection callbacks.
     */
    public interface OnLanguageSelectedListener {
        void onLanguageSelected(Language language);
    }

    /**
     * Constructor for LanguageAdapter.
     *
     * @param languages The list of languages to display
     * @param listener  The listener for language selection events
     */
    public LanguageAdapter(List<Language> languages, OnLanguageSelectedListener listener) {
        this.languages = languages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_language, parent, false);
        return new LanguageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LanguageViewHolder holder, int position) {
        Language language = languages.get(position);
        holder.bind(language, listener);
    }

    @Override
    public int getItemCount() {
        return languages.size();
    }

    /**
     * ViewHolder for language items.
     */
    public static class LanguageViewHolder extends RecyclerView.ViewHolder {

        private CardView cardView;
        private TextView languageNameTextView;

        /**
         * Constructor for LanguageViewHolder.
         *
         * @param itemView The view for a single language item
         */
        public LanguageViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardLanguage);
            languageNameTextView = itemView.findViewById(R.id.textViewLanguageName);
        }

        /**
         * Binds the language data to the view.
         *
         * @param language The language to display
         * @param listener The listener for selection events
         */
        public void bind(Language language, OnLanguageSelectedListener listener) {
            languageNameTextView.setText(language.getLanguageName());

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLanguageSelected(language);
                }
            });
        }
    }
}

