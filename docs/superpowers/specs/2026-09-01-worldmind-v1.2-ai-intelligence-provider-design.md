# Worldmind V1.2 AI Intelligence Provider Design

## Goal
Add an optional, server-authoritative AI intelligence layer to Worldmind that can use Ollama, Forgey, or a compatible HTTP endpoint to provide bounded semantic/transformation advice without becoming authoritative over simulation, protection, chronology, or physical materialization.

## Non-negotiable invariants
- Baseline Worldmind remains fully functional with AI disabled, disconnected, malformed, timed out, or unavailable.
- AI never directly edits blocks, issues Minecraft commands, forces chunks, executes code, or writes authoritative world state.
- Worldseal, absence thresholds, confidence restraint, simulation budgets, and deterministic safety validation always outrank AI output.
- AI requests are asynchronous, event-driven, cached, bounded, and never executed per tick.
- Server-side configuration is authoritative in multiplayer.
- API keys/secrets are not accepted through Minecraft commands and are not stored in world saves or logs.
- Only HTTP/HTTPS endpoints are accepted. Missing schemes are normalized to http:// for localhost-style commands.
- AI output is parsed into a strict allowlisted schema. Unknown fields may be ignored; unknown recommendation values invalidate the proposal.

## Persistent configuration
Create `config/worldmind-ai.json`, independent of `worldmind.json`.

Fields:
- `enabled`: default `false`
- `provider`: `BUILTIN`, `OLLAMA`, `FORGEY`, `COMPATIBLE`; default `BUILTIN`
- `endpoint`: default empty for BUILTIN; Ollama shortcut sets `http://localhost:11434`
- `model`: provider-specific model identifier, default empty
- `timeoutSeconds`: default 8, clamp 1..60
- `maxConcurrentRequests`: default 1, clamp 1..4
- `cacheDays`: default 5.0, clamp 0.25..30
- feature flags: `structure`, `transformation`, `civilization`, `history`, `naming`; structure + transformation default on, the rest default off

All command mutations write the validated config immediately and rebuild the provider instance.

## Command surface
All current `/worldmind` commands retain their behavior. Add moderator/admin-only:

- `/worldmind ai` -> same as status
- `/worldmind ai status`
- `/worldmind ai enable`
- `/worldmind ai disable`
- `/worldmind ai provider <builtin|ollama|forgey|compatible>`
- `/worldmind ai endpoint <url>`
- `/worldmind ai model <model>`
- `/worldmind ai test`
- `/worldmind ai reload`
- `/worldmind ai use <structure|transformation|civilization|history|naming> <on|off>`
- `/worldmind ai ollama` -> provider OLLAMA + endpoint `http://localhost:11434`
- `/worldmind ai forgey <endpoint>` -> provider FORGEY + normalized endpoint

`test` starts asynchronously and immediately returns a testing message; completion is posted back on the server thread with provider, endpoint, latency, and sanitized success/failure. No response body is printed on failure.

## Provider abstraction
`WorldmindIntelligenceProvider` exposes:
- `CompletableFuture<AIConnectionResult> testConnection()`
- `CompletableFuture<AITransformationProposal> requestTransformation(AIPlaceContext context)`
- provider metadata methods

Implementations:
- `BuiltinIntelligenceProvider`: disabled/no-network fallback; never returns external advice.
- `OllamaIntelligenceProvider`: POSTs to `<base>/api/chat`, `stream=false`, asks for strict JSON content, and parses `message.content` as the Worldmind proposal schema.
- `ForgeyIntelligenceProvider`: POSTs the Worldmind request envelope directly to the configured endpoint and expects a direct Worldmind proposal JSON response. This defines the Worldmind↔Forgey contract without assuming an undocumented Forgey API.
- `CompatibleIntelligenceProvider`: POSTs an OpenAI-compatible chat-completions request to the configured endpoint (if endpoint ends in `/v1/chat/completions`, use it as-is; otherwise append `/v1/chat/completions`) and parses the first assistant message content as proposal JSON.

Networking is isolated behind `AIHttpTransport` so request formatting and provider behavior can be tested without real network access.

## Strict proposal schema
AI receives a compact semantic summary, never chunks or arbitrary world files.

`AIPlaceContext` contains:
- place id/kind
- structure confidence
- palette and intent scores
- absence days
- nature/settlement/fragility/threat pressures
- deterministic Worldmind recommendation + intensity

AI may return only:
- `placeId`
- `recommendation`: `STASIS|RECLAMATION|DECAY|CONSTRUCTIVE|BLENDED`
- `confidence`: 0..1
- `intensityAdjustment`: -0.15..+0.15 after validation
- `reason`: sanitized/truncated text, max 240 chars
- `styleHint`: sanitized/truncated text, max 120 chars; advisory only

No block coordinates, commands, scripts, item grants, entity operations, or arbitrary action lists are accepted.

## Validation and authority
`AIProposalValidator` rejects proposals when:
- place id mismatches
- confidence is non-finite or below 0.70
- recommendation is missing/unknown
- adjustment is non-finite
- place is Worldsealed
- place confidence is below deterministic safety threshold
- absence is below configured minimum

Accepted intensity adjustment is clamped to [-0.15,+0.15].

`AIAdvisoryMixer` may influence only a decision that has already passed deterministic eligibility gates. Worldseal/below-threshold/low-confidence STASIS decisions are immutable. For eligible contextual decisions, AI may recommend another allowlisted evolution type and bounded intensity adjustment. Final intensity remains 0..1.

## Runtime lifecycle
`WorldmindAIManager` owns provider/config/in-flight/cache state.

- Initialize on mod startup after core config.
- Clear transient in-flight/cache state when server stops.
- On recognized/updated significant places, `WorldmindRuntime` calls `considerPlace(...)` at the normal 200-tick observation cadence.
- Manager schedules at most one request per place per cache window, obeys concurrency limits, and never blocks the server thread.
- Advice is cached in memory by place id with Worldmind tick expiry; failures are negatively cached briefly to avoid hammering an unavailable endpoint.
- `PlaceSimulationService` consumes cached validated advice through an injected `AIAdviceSource`; no network calls occur inside abstract simulation or `/worldmind advance`.

## Performance and failure behavior
- Default AI is disabled.
- No request on every tick.
- Max concurrent external requests defaults to 1.
- Request timeout defaults to 8 seconds.
- Response body size is capped before parsing.
- Malformed JSON, HTTP errors, timeouts, unavailable endpoints, missing models, and provider exceptions degrade to deterministic behavior.
- AI requests do not load or inspect remote chunks.

## Diagnostics
`/worldmind ai status` reports:
- enabled/disabled
- provider
- endpoint (sanitized)
- model
- enabled feature flags
- in-flight request count
- cached advice count
- last test state/latency/error category

Normal debug logging may report provider lifecycle/error categories but must not log complete prompts/responses or secrets.

## Testing
Add unit tests for:
- AI config validation and endpoint normalization
- provider request formatting/parsing using fake transport
- strict proposal validation
- advisory mixing authority invariants
- cache/concurrency/fallback behavior using fake provider
- command-independent admin config operations
- deterministic PlaceSimulationService behavior with absent, valid, and rejected AI advice

CI remains Minecraft 26.2 + Fabric Loader 0.19.3 + Fabric API 0.156.0+26.2 + Java 25.

## Acceptance criteria
1. Existing V1.1 tests still pass.
2. With AI disabled or provider failing, deterministic decisions are unchanged.
3. Worldsealed/below-threshold/low-confidence places cannot be activated by AI.
4. Valid cached AI advice can influence an otherwise eligible place decision only within allowlisted type/intensity bounds.
5. `/worldmind ai ...` settings persist across config reload.
6. `/worldmind ai test` is asynchronous and does not freeze the server thread.
7. Static forced-chunk-loading guard remains green.
8. Production JAR contains AI provider/manager/config/command classes and reports version 1.2.0.
