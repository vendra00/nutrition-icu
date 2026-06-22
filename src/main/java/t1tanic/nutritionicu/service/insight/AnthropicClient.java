package t1tanic.nutritionicu.service.insight;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import t1tanic.nutritionicu.config.AnthropicProperties;

/** Thin client over the Anthropic Messages API ({@code POST /v1/messages}). */
@Component
@EnableConfigurationProperties(AnthropicProperties.class)
public class AnthropicClient {

    private final AnthropicProperties props;
    private final RestClient restClient;

    public AnthropicClient(AnthropicProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(120));
        this.restClient = RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public boolean isConfigured() {
        return props.configured();
    }

    public String model() {
        return props.model();
    }

    /**
     * Sends a single-turn prompt and returns the concatenated text of the reply. When {@code knowledge}
     * is non-blank it is added as a second system block marked for prompt caching, so the (large, stable)
     * reference text is billed at the cache rate on repeat calls rather than re-sent in full each time.
     *
     * @throws IllegalStateException if the API key is missing or the response is unusable
     */
    public String complete(String instructions, String knowledge, String userPrompt) {
        if (!props.configured()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY is not configured.");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", props.model());
        body.put("max_tokens", props.maxTokens());
        body.put("system", systemBlocks(instructions, knowledge));
        body.put("messages", List.of(Map.of("role", "user", "content", userPrompt)));

        MessagesResponse response = restClient.post()
                .uri("/v1/messages")
                .header("x-api-key", props.apiKey())
                .header("anthropic-version", props.version())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(MessagesResponse.class);

        if (response == null || response.content() == null || response.content().isEmpty()) {
            throw new IllegalStateException("Empty response from the Anthropic API.");
        }
        return response.content().stream()
                .filter(block -> "text".equals(block.type()))
                .map(ContentBlock::text)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Builds the {@code system} field. With no knowledge it is a plain string; with knowledge it is an
     * array of two text blocks, the reference block carrying {@code cache_control} so the prefix is cached.
     */
    private static Object systemBlocks(String instructions, String knowledge) {
        if (knowledge == null || knowledge.isBlank()) {
            return instructions;
        }
        Map<String, Object> instructionBlock = Map.of("type", "text", "text", instructions);
        Map<String, Object> knowledgeBlock = Map.of(
                "type", "text",
                "text", "Reference material to ground your analysis:\n\n" + knowledge,
                "cache_control", Map.of("type", "ephemeral"));
        return List.of(instructionBlock, knowledgeBlock);
    }

    /** Subset of the Messages API response we consume. */
    private record MessagesResponse(List<ContentBlock> content) {
    }

    private record ContentBlock(String type, String text) {
    }
}
