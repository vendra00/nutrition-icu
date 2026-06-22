package t1tanic.nutritionicu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for calling the Anthropic Messages API. The {@code apiKey} comes from the
 * {@code ANTHROPIC_API_KEY} environment variable (see application.properties) and is never committed.
 */
@ConfigurationProperties(prefix = "anthropic")
public record AnthropicProperties(String apiKey, String model, String baseUrl, String version, Integer maxTokens) {

    public AnthropicProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.anthropic.com";
        }
        if (model == null || model.isBlank()) {
            model = "claude-sonnet-4-6";
        }
        if (version == null || version.isBlank()) {
            version = "2023-06-01";
        }
        if (maxTokens == null) {
            maxTokens = 1400;
        }
    }

    public boolean configured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
