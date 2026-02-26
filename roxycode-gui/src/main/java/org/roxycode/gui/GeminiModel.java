package org.roxycode.gui;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Gemini model and its pricing/description metadata.
 */
public record GeminiModel(
    @JsonProperty("api_name") String apiName,
    @JsonProperty("input_price_per_1m") double inputPrice,
    @JsonProperty("cached_price_per_1m") double cachedPrice,
    @JsonProperty("cache_storage_price_per_1m_per_hour") double storagePrice,
    @JsonProperty("output_price_per_1m") double outputPrice,
    @JsonProperty("description") String description
) {
    @Override
    public String toString() {
        return apiName.replace("models/", "");
    }
}
