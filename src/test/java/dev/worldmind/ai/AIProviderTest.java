package dev.worldmind.ai;

import static org.junit.jupiter.api.Assertions.*;
import dev.worldmind.core.EvolutionType;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AIProviderTest {
    private static AIPlaceContext context() {
        return new AIPlaceContext("p1","homestead",.9,"wood",.1,.2,.3,10,.4,.5,.4,.2,false,EvolutionType.RECLAMATION,.5);
    }

    @Test void ollamaUsesApiChatAndParsesMessageContent() {
        AtomicReference<AIHttpRequest> seen = new AtomicReference<>();
        AIHttpTransport t = r -> { seen.set(r); return CompletableFuture.completedFuture(new AIHttpResponse(200,
                "{\"message\":{\"content\":\"{\\\"placeId\\\":\\\"p1\\\",\\\"recommendation\\\":\\\"DECAY\\\",\\\"confidence\\\":0.9,\\\"intensityAdjustment\\\":0.1,\\\"reason\\\":\\\"old\\\",\\\"styleHint\\\":\\\"moss\\\"}\"}}", 12)); };
        var p = new OllamaIntelligenceProvider(new WorldmindAIConfig(true,AIProviderType.OLLAMA,"http://localhost:11434","qwen",8,1,5,true,true,false,false,false), t);
        var out = p.requestTransformation(context()).join();
        assertEquals("http://localhost:11434/api/chat", seen.get().uri().toString());
        assertTrue(seen.get().body().contains("\"stream\":false"));
        assertEquals("DECAY", out.recommendation());
    }

    @Test void forgeyPostsDirectWorldmindEnvelope() {
        AtomicReference<AIHttpRequest> seen = new AtomicReference<>();
        AIHttpTransport t = r -> { seen.set(r); return CompletableFuture.completedFuture(new AIHttpResponse(200,
                "{\"placeId\":\"p1\",\"recommendation\":\"BLENDED\",\"confidence\":0.91,\"intensityAdjustment\":0.0,\"reason\":\"ok\",\"styleHint\":\"\"}", 8)); };
        var p = new ForgeyIntelligenceProvider(new WorldmindAIConfig(true,AIProviderType.FORGEY,"localhost:3000","forgey",8,1,5,true,true,false,false,false), t);
        assertEquals("BLENDED", p.requestTransformation(context()).join().recommendation());
        assertEquals("http://localhost:3000", seen.get().uri().toString());
        assertTrue(seen.get().body().contains("worldmind_transformation_proposal"));
    }

    @Test void compatibleAppendsChatCompletionsPath() {
        AtomicReference<AIHttpRequest> seen = new AtomicReference<>();
        AIHttpTransport t = r -> { seen.set(r); return CompletableFuture.completedFuture(new AIHttpResponse(200,
                "{\"choices\":[{\"message\":{\"content\":\"{\\\"placeId\\\":\\\"p1\\\",\\\"recommendation\\\":\\\"CONSTRUCTIVE\\\",\\\"confidence\\\":0.95,\\\"intensityAdjustment\\\":0.05}\"}}]}", 10)); };
        var p = new CompatibleIntelligenceProvider(new WorldmindAIConfig(true,AIProviderType.COMPATIBLE,"http://localhost:8080","local",8,1,5,true,true,false,false,false), t);
        p.requestTransformation(context()).join();
        assertEquals("http://localhost:8080/v1/chat/completions", seen.get().uri().toString());
    }

    @Test void providerRejectsNon2xxAndOversizedResponse() {
        AIHttpTransport bad = r -> CompletableFuture.completedFuture(new AIHttpResponse(500,"oops",1));
        var p = new ForgeyIntelligenceProvider(new WorldmindAIConfig(true,AIProviderType.FORGEY,"localhost:3000","",8,1,5,true,true,false,false,false), bad);
        assertThrows(Exception.class, () -> p.requestTransformation(context()).join());
    }
}
