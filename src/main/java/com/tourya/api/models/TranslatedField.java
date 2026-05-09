package com.tourya.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Model class to represent multi-language text fields.
 * Supports Spanish (es), English (en), and Portuguese (pt).
 * Spanish is the default and required language.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Multi-language text field supporting Spanish, English, and Portuguese", 
        example = "{\"es\": \"Texto en español\", \"en\": \"Text in English\", \"pt\": \"Texto em português\"}")
public class TranslatedField implements Serializable {

    @NotBlank(message = "Spanish translation is required")
    @JsonProperty("es")
    @Schema(description = "Spanish translation (required)", 
            example = "Texto en español", 
            required = true)
    private String es;

    @JsonProperty("en")
    @Schema(description = "English translation (optional)", 
            example = "Text in English")
    private String en;

    @JsonProperty("pt")
    @Schema(description = "Portuguese translation (optional)", 
            example = "Texto em português")
    private String pt;

    /**
     * Get translation for a specific language.
     * Falls back to Spanish if the requested language is not available.
     *
     * @param lang Language code ("es", "en", or "pt")
     * @return Translation in the requested language or Spanish as fallback
     */
    public String get(String lang) {
        if (lang == null || lang.isEmpty()) {
            return es;
        }

        switch (lang.toLowerCase()) {
            case "en":
                return (en != null && !en.isEmpty()) ? en : es;
            case "pt":
                return (pt != null && !pt.isEmpty()) ? pt : es;
            case "es":
            default:
                return es;
        }
    }

    /**
     * Create a TranslatedField with only Spanish content.
     *
     * @param spanish Spanish text
     * @return TranslatedField with Spanish only
     */
    public static TranslatedField ofSpanish(String spanish) {
        return new TranslatedField(spanish, "", "");
    }

    /**
     * Create a TranslatedField with all three languages.
     *
     * @param spanish Spanish text
     * @param english English text
     * @param portuguese Portuguese text
     * @return TranslatedField with all languages
     */
    public static TranslatedField of(String spanish, String english, String portuguese) {
        return new TranslatedField(spanish, english, portuguese);
    }
}
