package dev.worldmind.command;

import static org.junit.jupiter.api.Assertions.*;
import dev.worldmind.ai.*;
import org.junit.jupiter.api.Test;

class WorldmindAIAdminServiceTest {
    private final WorldmindAIAdminService admin = new WorldmindAIAdminService();

    @Test void endpointCommandNormalizesLocalhostStyle() {
        var c = admin.endpoint(WorldmindAIConfig.defaults().withProvider(AIProviderType.FORGEY), "localhost:3000");
        assertEquals("http://localhost:3000", c.endpoint());
    }

    @Test void ollamaShortcutSetsProviderAndDefaultEndpoint() {
        var c = admin.ollama(WorldmindAIConfig.defaults());
        assertEquals(AIProviderType.OLLAMA, c.provider());
        assertEquals("http://localhost:11434", c.endpoint());
    }

    @Test void forgeyShortcutSetsDirectEndpoint() {
        var c = admin.forgey(WorldmindAIConfig.defaults(), "localhost:3000");
        assertEquals(AIProviderType.FORGEY, c.provider());
        assertEquals("http://localhost:3000", c.endpoint());
    }

    @Test void featureAndModelMutationsAreImmutable() {
        var base = admin.ollama(WorldmindAIConfig.defaults());
        var changed = admin.model(admin.feature(base, AIFeature.HISTORY, true), "qwen3:8b");
        assertEquals("qwen3:8b", changed.model());
        assertTrue(changed.history());
        assertFalse(base.history());
    }

    @Test void statusContainsOperationalStateButNoResponsePayload() {
        var cfg = admin.forgey(WorldmindAIConfig.defaults(), "localhost:3000").withEnabled(true);
        var status = new WorldmindAIStatus(true, AIProviderType.FORGEY, cfg.endpoint(), "forgey", 1, 2,
                AIConnectionResult.failed(12,"connection_failed","ConnectException"));
        String text = admin.status(cfg, status);
        assertTrue(text.contains("FORGEY"));
        assertTrue(text.contains("http://localhost:3000"));
        assertTrue(text.contains("inFlight=1"));
        assertFalse(text.toLowerCase().contains("prompt"));
    }
}
