# Changelog

## 1.2.0
- Added optional server-authoritative AI Intelligence Provider framework; disabled by default.
- Added persistent `config/worldmind-ai.json` settings independent from core Worldmind config.
- Added `/worldmind ai` admin commands for provider, endpoint, model, enable/disable, feature toggles, connection test, reload, Ollama shortcut, and Forgey shortcut.
- Added Ollama `/api/chat`, direct Forgey Worldmind JSON, and OpenAI-compatible chat-completions adapters.
- Added strict allowlisted transformation proposal schema and validation.
- Added asynchronous request manager with per-place de-duplication, hard concurrency limit, Worldmind-time cache expiry, and failure backoff.
- Added race-safe planning: a place defers only while its own AI request is in flight; timeout/failure returns to deterministic planning.
- Added bounded AI advisory mixing that cannot override Worldseal, absence threshold, or low-confidence safety STASIS.
- Added structure-detail privacy toggle; when disabled, external AI does not receive palette or architectural-intent metrics.
- Rejected credential-bearing endpoint URL forms and kept API keys out of command/config design.
- Preserved V1.1 spatial evolution, loaded-only materialization, and no-forced-chunk invariant.

## 1.1.0
- Added persistent 8x8 spatial memory inside each 128x128 Worldmind region.
- Replaced stationary-presence route pressure with actual sampled player movement corridors.
- Mapped forest, vegetation, farm, settlement, build, disturbance, path, and protection observations into specific micro-cells.
- Added spatial process planning: forest frontiers, vegetation/reclamation/farm patches, route corridors, settlement growth/decline edges, fortification edges, place domains, and anomaly sites.
- Physical mutation budgets now scale with accumulated physical age and decision intensity.
- Replaced random point targeting with deterministic patch/frontier/corridor geometry.
- Added immediate clustered tree growth using nearby loaded tree evidence for palette selection.
- Added Worldseal spatial masks while retaining exact block-level protection checks.
- Preserved no-op materialization age: zero changed blocks never reset a region's physical-age clock.
- Added `/worldmind status`, `inspect`, `advance`, `materialize`, `history`, and `debug` commands.
- `/worldmind advance` provides fast Worldmind-only chronology testing without replaying vanilla ticks.
- Increased default bounded physical materialization budget to 384 blocks per regional plan.

## 1.0.0
- Persistent Worldmind place and regional memory.
- Contextual regional evolution, civilization/history foundations, chronology modes, Worldseal protection, and safe loaded-only materialization.
