package com.vinayak.medireach.model;

/**
 * Language model class representing a supported language.
 */
public class Language {

    private String languageName;
    private String languageCode;

    /**
     * Constructor for Language.
     *
     * @param languageName The display name of the language
     * @param languageCode The language code (e.g., "en", "hi")
     */
    public Language(String languageName, String languageCode) {
        this.languageName = languageName;
        this.languageCode = languageCode;
    }

    /**
     * Gets the language name.
     *
     * @return The language name
     */
    public String getLanguageName() {
        return languageName;
    }

    /**
     * Sets the language name.
     *
     * @param languageName The language name to set
     */
    public void setLanguageName(String languageName) {
        this.languageName = languageName;
    }

    /**
     * Gets the language code.
     *
     * @return The language code
     */
    public String getLanguageCode() {
        return languageCode;
    }

    /**
     * Sets the language code.
     *
     * @param languageCode The language code to set
     */
    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }
}

