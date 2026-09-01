package dev.worldmind.ai;

import static org.junit.jupiter.api.Assertions.*;
import dev.worldmind.core.EvolutionType;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class WorldmindAIManagerTest {
    private static AIPlaceContext context(String id) {
        return new AIPlaceContext(id,"homestead",.9,"wood",0,0,0,10,.4,.5,.4,.2,false,EvolutionType.RECLAMATION,.5);
    }

    @Test void disabledManagerNeverCallsProvider() {
        AtomicInteger calls = new AtomicInteger();
        WorldmindIntelligenceProvider p = fake(calls, new CompletableFuture<>());
        var m = new WorldmindAIManager(WorldmindAIConfig.defaults(), p);
        assertFalse(m.consider(context("p"), 5, 1000));
        assertEquals(0, calls.get());
    }

    @Test void onlyOneInflightRequestPerPlaceAndCachesValidAdvice() {
        AtomicInteger calls = new AtomicInteger();
        CompletableFuture<AITransformationProposal> future = new CompletableFuture<>();
        var cfg = new WorldmindAIConfig(true,AIProviderType.FORGEY,"http://localhost:3000","",8,1,5,true,true,false,false,false);
        var m = new WorldmindAIManager(cfg, fake(calls, future));
        assertTrue(m.consider(context("p"), 5, 1000));
        assertFalse(m.consider(context("p"), 5, 1000));
        assertEquals(1, calls.get());
        future.complete(new AITransformationProposal("p","DECAY",.9,.1,"ok",""));
        assertEquals(EvolutionType.DECAY, m.adviceFor("p").orElseThrow().recommendation());
    }

    @Test void honorsGlobalConcurrencyLimit() {
        AtomicInteger calls = new AtomicInteger();
        CompletableFuture<AITransformationProposal> pending = new CompletableFuture<>();
        var cfg = new WorldmindAIConfig(true,AIProviderType.FORGEY,"http://localhost:3000","",8,1,5,true,true,false,false,false);
        var m = new WorldmindAIManager(cfg, fake(calls, pending));
        assertTrue(m.consider(context("a"),5,1000));
        assertFalse(m.consider(context("b"),5,1000));
        assertEquals(1,calls.get());
    }

    @Test void structureToggleHidesArchitecturalDetailsFromProvider() {
        java.util.concurrent.atomic.AtomicReference<AIPlaceContext> seen = new java.util.concurrent.atomic.AtomicReference<>();
        var cfg = new WorldmindAIConfig(true,AIProviderType.FORGEY,"http://localhost:3000","",8,1,5,false,true,false,false,false);
        WorldmindIntelligenceProvider p = new WorldmindIntelligenceProvider() {
            public AIProviderType type(){return AIProviderType.FORGEY;} public String endpoint(){return "http://localhost";} public String model(){return "";}
            public CompletableFuture<AIConnectionResult> testConnection(){return CompletableFuture.completedFuture(AIConnectionResult.ok(1,"ok"));}
            public CompletableFuture<AITransformationProposal> requestTransformation(AIPlaceContext c){seen.set(c);return CompletableFuture.completedFuture(new AITransformationProposal(c.placeId(),"DECAY",.9,0,"",""));}
        };
        var m = new WorldmindAIManager(cfg,p);
        assertTrue(m.consider(context("p"),5,1000));
        assertEquals("hidden", seen.get().dominantPalette());
        assertEquals(0.0, seen.get().defensiveIntent());
    }

    @Test void cacheExpiresInWorldmindTime() {
        AtomicInteger calls = new AtomicInteger();
        var cfg = new WorldmindAIConfig(true,AIProviderType.FORGEY,"http://localhost:3000","",8,1,.25,true,true,false,false,false);
        var m = new WorldmindAIManager(cfg, immediate(calls,"p"));
        assertTrue(m.consider(context("p"),5,1000));
        assertTrue(m.adviceFor("p").isPresent());
        m.setClockTickForTesting(1000 + 7000);
        assertTrue(m.adviceFor("p").isEmpty());
    }

    private static WorldmindIntelligenceProvider immediate(AtomicInteger calls,String id) {
        return fake(calls, CompletableFuture.completedFuture(new AITransformationProposal(id,"DECAY",.9,.1,"ok","")));
    }
    private static WorldmindIntelligenceProvider fake(AtomicInteger calls, CompletableFuture<AITransformationProposal> future) {
        return new WorldmindIntelligenceProvider() {
            public AIProviderType type(){return AIProviderType.FORGEY;} public String endpoint(){return "http://localhost";} public String model(){return "";}
            public CompletableFuture<AIConnectionResult> testConnection(){return CompletableFuture.completedFuture(AIConnectionResult.ok(1,"ok"));}
            public CompletableFuture<AITransformationProposal> requestTransformation(AIPlaceContext c){calls.incrementAndGet();return future;}
        };
    }
}
