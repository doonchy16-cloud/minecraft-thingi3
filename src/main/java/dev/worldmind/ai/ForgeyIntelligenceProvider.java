package dev.worldmind.ai;

import java.util.concurrent.CompletableFuture;

/** Direct Worldmind <-> Forgey JSON bridge. Endpoint is used exactly as configured. */
public final class ForgeyIntelligenceProvider extends AbstractHttpIntelligenceProvider {
    public ForgeyIntelligenceProvider(WorldmindAIConfig config, AIHttpTransport transport) { super(config, transport); }

    @Override public CompletableFuture<AIConnectionResult> testConnection() {
        String body = "{\"type\":\"worldmind_connection_test\",\"protocolVersion\":\"1.2\",\"model\":" + WorldmindAIJson.quote(config.model()) + "}";
        return send("POST", uri(config.endpoint()), body).thenApply(r -> connectionFrom(r, "Forgey endpoint responded"))
                .exceptionallyCompose(AbstractHttpIntelligenceProvider::failedConnection);
    }

    @Override public CompletableFuture<AITransformationProposal> requestTransformation(AIPlaceContext context) {
        String body = "{\"type\":\"worldmind_transformation_proposal\",\"protocolVersion\":\"1.2\",\"model\":" +
                WorldmindAIJson.quote(config.model()) + ",\"constraints\":" + WorldmindAIJson.quote(SYSTEM_PROMPT) +
                ",\"context\":" + WorldmindAIJson.contextJson(context) + "}";
        return send("POST", uri(config.endpoint()), body).thenApply(r -> WorldmindAIJson.parseProposal(r.body()));
    }
}
