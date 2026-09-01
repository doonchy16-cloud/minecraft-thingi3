package dev.worldmind.ai;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WorldmindAIConfigTest {
    @Test void safeDefaultsAreDisabled() {
        var c = WorldmindAIConfig.defaults();
        assertFalse(c.enabled());
        assertEquals(AIProviderType.BUILTIN, c.provider());
        assertEquals(8, c.timeoutSeconds());
        assertEquals(1, c.maxConcurrentRequests());
        assertTrue(c.structure());
        assertTrue(c.transformation());
        assertFalse(c.civilization());
    }

    @Test void validatesBoundsAndNormalizesLocalhost() {
        var c = new WorldmindAIConfig(true, AIProviderType.FORGEY, "localhost:3000", "forgey", 999, 99, 100,
                true, true, false, false, false).validated();
        assertEquals("http://localhost:3000", c.endpoint());
        assertEquals(60, c.timeoutSeconds());
        assertEquals(4, c.maxConcurrentRequests());
        assertEquals(30.0, c.cacheDays(), 0.0001);
    }

    @Test void rejectsUnsafeEndpointSchemeByDisablingExternalUse() {
        var c = new WorldmindAIConfig(true, AIProviderType.FORGEY, "file:///tmp/model", "forgey", 8, 1, 5,
                true, true, false, false, false).validated();
        assertEquals("", c.endpoint());
        assertFalse(c.enabled());
    }

    @Test void rejectsCredentialBearingEndpointForms() {
        assertEquals("", WorldmindAIConfig.normalizeEndpoint("http://user:secret@localhost:3000"));
        assertEquals("", WorldmindAIConfig.normalizeEndpoint("http://localhost:3000?token=secret"));
        assertEquals("", WorldmindAIConfig.normalizeEndpoint("http://localhost:3000/#secret"));
    }

    @Test void featureToggleIsImmutable() {
        var c = WorldmindAIConfig.defaults().withFeature(AIFeature.HISTORY, true);
        assertTrue(c.history());
        assertFalse(WorldmindAIConfig.defaults().history());
    }
}
