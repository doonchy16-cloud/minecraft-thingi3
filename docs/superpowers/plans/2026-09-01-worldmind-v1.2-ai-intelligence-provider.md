# Worldmind V1.2 AI Intelligence Provider Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an optional, persistent, asynchronous AI provider framework controlled through `/worldmind ai`, with Ollama/Forgey/compatible HTTP adapters and strictly bounded transformation advice.

**Architecture:** AI is a non-authoritative advisory layer. Provider networking is isolated from Minecraft simulation, requests are asynchronous/cached, responses are strict structured proposals, and deterministic Worldmind gates remain authoritative. Place simulation consumes only cached validated advice and never performs network I/O itself.

**Tech Stack:** Java 25, Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, Brigadier, Gson, `java.net.http.HttpClient`, JUnit 5.11.4.

**Spec:** `docs/superpowers/specs/2026-09-01-worldmind-v1.2-ai-intelligence-provider-design.md`

## Global Constraints
- AI disabled by default and never required for baseline gameplay.
- No direct AI block/entity/command execution.
- Worldseal, absence, confidence, budgets, and validation always outrank AI.
- Never force-load chunks.
- Never accept/store API keys through commands.
- External calls are asynchronous, timeout-bounded, concurrency-bounded, and cached.
- Minecraft 26.2 / Java 25 compatibility is mandatory.

---

### Task 1: Persistent AI configuration
**Files:**
- Create: `src/main/java/dev/worldmind/ai/AIProviderType.java`
- Create: `src/main/java/dev/worldmind/ai/WorldmindAIConfig.java`
- Create: `src/main/java/dev/worldmind/ai/WorldmindAIConfigLoader.java`
- Test: `src/test/java/dev/worldmind/ai/WorldmindAIConfigTest.java`

**Interfaces:**
- Produces `WorldmindAIConfig.defaults()`, `validated()`, endpoint normalization, immutable feature toggles, and loader `get/load/save/update/reload`.

- [ ] Write failing tests for defaults, clamp rules, localhost endpoint normalization, invalid schemes, and feature toggles.
- [ ] Run targeted test and verify RED.
- [ ] Implement provider enum/config/loader with `config/worldmind-ai.json` persistence.
- [ ] Run targeted test and verify GREEN.

### Task 2: Strict proposal/context model and validator
**Files:**
- Create: `src/main/java/dev/worldmind/ai/AIPlaceContext.java`
- Create: `src/main/java/dev/worldmind/ai/AITransformationProposal.java`
- Create: `src/main/java/dev/worldmind/ai/AIValidatedAdvice.java`
- Create: `src/main/java/dev/worldmind/ai/AIProposalValidator.java`
- Test: `src/test/java/dev/worldmind/ai/AIProposalValidatorTest.java`

**Interfaces:**
- Produces strict allowlisted recommendation/intensity advice or rejection.

- [ ] Write failing tests for place mismatch, confidence threshold, Worldseal/absence/confidence authority, NaN rejection, text truncation, and ±0.15 adjustment clamp.
- [ ] Run targeted test and verify RED.
- [ ] Implement models and validator.
- [ ] Run targeted test and verify GREEN.

### Task 3: HTTP transport and provider adapters
**Files:**
- Create: `src/main/java/dev/worldmind/ai/AIHttpRequest.java`
- Create: `src/main/java/dev/worldmind/ai/AIHttpResponse.java`
- Create: `src/main/java/dev/worldmind/ai/AIHttpTransport.java`
- Create: `src/main/java/dev/worldmind/ai/JavaAIHttpTransport.java`
- Create: `src/main/java/dev/worldmind/ai/AIConnectionResult.java`
- Create: `src/main/java/dev/worldmind/ai/WorldmindIntelligenceProvider.java`
- Create: `src/main/java/dev/worldmind/ai/BuiltinIntelligenceProvider.java`
- Create: `src/main/java/dev/worldmind/ai/OllamaIntelligenceProvider.java`
- Create: `src/main/java/dev/worldmind/ai/ForgeyIntelligenceProvider.java`
- Create: `src/main/java/dev/worldmind/ai/CompatibleIntelligenceProvider.java`
- Create: `src/main/java/dev/worldmind/ai/AIProviderFactory.java`
- Test: `src/test/java/dev/worldmind/ai/AIProviderTest.java`

**Interfaces:**
- Consumes validated config/context.
- Produces async connection result/proposal futures with no real network required in unit tests.

- [ ] Write fake-transport tests for Ollama `/api/chat`, Forgey direct contract, compatible chat-completions URL, strict JSON extraction, HTTP failure, and response-size cap.
- [ ] Run targeted test and verify RED.
- [ ] Implement transport/provider adapters.
- [ ] Run targeted test and verify GREEN.

### Task 4: Advisory mixing authority
**Files:**
- Create: `src/main/java/dev/worldmind/ai/AIAdviceSource.java`
- Create: `src/main/java/dev/worldmind/ai/AIAdvisoryMixer.java`
- Test: `src/test/java/dev/worldmind/ai/AIAdvisoryMixerTest.java`

**Interfaces:**
- Produces final `EvolutionDecision` from deterministic decision plus optional validated advice without bypassing safety gates.

- [ ] Write failing tests proving Worldseal/below-threshold/low-confidence STASIS cannot be changed and eligible contextual decisions can be boundedly influenced.
- [ ] Run targeted test and verify RED.
- [ ] Implement source interface and mixer.
- [ ] Run targeted test and verify GREEN.

### Task 5: Asynchronous manager, cache, and runtime observation hook
**Files:**
- Create: `src/main/java/dev/worldmind/ai/WorldmindAIManager.java`
- Test: `src/test/java/dev/worldmind/ai/WorldmindAIManagerTest.java`
- Modify: `src/main/java/dev/worldmind/sim/WorldmindRuntime.java`
- Modify: `src/main/java/dev/worldmind/WorldmindMod.java`

**Interfaces:**
- Manager exposes `initialize`, `shutdown`, `status`, `testConnection`, `considerPlace`, `adviceFor`, `reloadProvider`.

- [ ] Write fake-provider tests for disabled fallback, one in-flight request/place, cache expiry, negative backoff, and concurrency bound.
- [ ] Run targeted test and verify RED.
- [ ] Implement manager lifecycle and runtime `considerPlace` hook at recognized-place observation cadence.
- [ ] Run targeted test and verify GREEN.

### Task 6: PlaceSimulationService consumption of cached advice
**Files:**
- Modify: `src/main/java/dev/worldmind/sim/PlaceSimulationService.java`
- Test: `src/test/java/dev/worldmind/sim/PlaceSimulationAIAdviceTest.java`

**Interfaces:**
- Add constructor overload accepting `AIAdviceSource`; legacy constructor remains deterministic/no-advice.

- [ ] Write failing tests for no-advice identity, valid eligible advice influence, and safety-ineligible advice no-op.
- [ ] Run targeted test and verify RED.
- [ ] Inject advice source and mixer after deterministic decision.
- [ ] Run targeted test and verify GREEN.
- [ ] Wire runtime global cycle to `WorldmindAIManager::adviceFor`; admin `/worldmind advance` retains deterministic constructor to avoid request storms.

### Task 7: `/worldmind ai` persistent command/admin surface
**Files:**
- Create: `src/main/java/dev/worldmind/command/WorldmindAIAdminService.java`
- Modify: `src/main/java/dev/worldmind/command/WorldmindCommands.java`
- Test: `src/test/java/dev/worldmind/command/WorldmindAIAdminServiceTest.java`

**Interfaces:**
- Admin service performs provider/endpoint/model/enable/use/reload mutations independent of Brigadier and returns sanitized status text.

- [ ] Write failing tests for endpoint normalization, Ollama shortcut, Forgey shortcut, provider/model persistence, feature toggles, and status sanitization.
- [ ] Run targeted test and verify RED.
- [ ] Implement admin service and command tree.
- [ ] Implement asynchronous `/worldmind ai test` callback on server thread.
- [ ] Run targeted tests and verify GREEN.

### Task 8: Version/docs/full verification
**Files:**
- Modify: `gradle.properties` -> `mod_version=1.2.0`
- Modify: `src/main/java/dev/worldmind/WorldmindMod.java` startup string
- Modify: `src/main/resources/fabric.mod.json` description
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Create: `docs/V1_2_AI.md`

**Interfaces:** none.

- [ ] Document exact command examples for Ollama and Forgey endpoint setup and security constraints.
- [ ] Run full `gradle --no-daemon test build` on Java 25-compatible environment/CI.
- [ ] Run static guard against forced chunk loading in `sim`, `materialize`, and new `ai` package for Minecraft chunk APIs.
- [ ] Verify production JAR contains `WorldmindAIManager`, providers, config loader, command classes, Worldseal resources, and Fabric metadata version `1.2.0`.
- [ ] Produce SHA-256 for source archive and production JAR.
