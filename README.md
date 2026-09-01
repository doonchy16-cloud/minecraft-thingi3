# Worldmind 1.2.0 😈🌎🧠

Worldmind is a Fabric mod for **Minecraft Java 26.2** whose primary goal is simple: **the generated world slowly changes and remembers history**.

V1.2 keeps the V1.1 spatial evolution engine and adds an **optional AI Intelligence Provider layer**. A server can connect Worldmind to local Ollama, Forgey, or an OpenAI-compatible HTTP endpoint. AI is advisory only: deterministic Worldmind remains authoritative over Worldseal, chronology, absence/confidence gates, budgets, and physical materialization.

## Requirements
- Minecraft Java Edition 26.2
- Fabric Loader 0.19.3+
- Fabric API 0.156.0+26.2
- Java 25

## Spatial world evolution
Each known 128x128 region has an 8x8 abstract spatial field. Forests can advance as frontiers, abandoned farmland can reclaim in contiguous patches, repeated travel can wear actual corridors, settlements can push growth edges, and Worldseal domains bend those processes around protected builds.

### Performance contract
Worldmind does not intentionally force-load remote chunks, replay missed Minecraft ticks, tick remote entity armies, or run global per-block scans. Observation is sampled and loaded-only. Materialization is budgeted and Worldseal-safe.

## AI Intelligence Providers
AI is **disabled by default**. First launch creates `config/worldmind-ai.json`.

Supported provider modes:
- `builtin` — deterministic Worldmind only; no external AI
- `ollama` — Ollama `/api/chat`
- `forgey` — direct Worldmind↔Forgey JSON protocol at the configured endpoint
- `compatible` — OpenAI-compatible `/v1/chat/completions`

### Recommended Ollama setup
```text
/worldmind ai ollama
/worldmind ai model qwen3:8b
/worldmind ai enable
/worldmind ai test
/worldmind ai status
```

Default Ollama shortcut endpoint: `http://localhost:11434`.

### Forgey setup
```text
/worldmind ai forgey localhost:3000
/worldmind ai model forgey
/worldmind ai enable
/worldmind ai test
```

Your proposed shorthand works through the explicit endpoint command too:

```text
/worldmind ai provider forgey
/worldmind ai endpoint localhost:3000
/worldmind ai model forgey
/worldmind ai enable
```

`localhost:3000` is normalized to `http://localhost:3000`.

### AI command surface
Commands require moderator/operator permission:

- `/worldmind ai` or `/worldmind ai status`
- `/worldmind ai enable`
- `/worldmind ai disable`
- `/worldmind ai provider <builtin|ollama|forgey|compatible>`
- `/worldmind ai endpoint <url>`
- `/worldmind ai model <model>`
- `/worldmind ai test`
- `/worldmind ai reload`
- `/worldmind ai use <structure|transformation|civilization|history|naming> <on|off>`
- `/worldmind ai ollama`
- `/worldmind ai forgey <endpoint>`

`/worldmind ai test` is asynchronous; it does not intentionally block the Minecraft server thread.

### AI authority rules
External AI receives compact semantic summaries, not chunks. It may return only a strict recommendation schema containing an allowlisted evolution type, confidence, bounded intensity adjustment, short reason, and style hint.

AI cannot directly:
- place/remove blocks
- issue Minecraft commands
- load chunks
- spawn entities/items
- execute code/scripts
- bypass Worldseal
- bypass minimum absence or place-confidence gates

If AI is offline, malformed, timed out, missing a model, or returns invalid advice, Worldmind falls back to its deterministic behavior.

### Credentials/security
Do **not** put API keys in Minecraft commands. Worldmind V1.2 does not provide an `/ai key` command. Endpoint URLs containing embedded userinfo, query strings, or fragments are rejected to reduce accidental secret leakage.

## Core testing/admin commands
- `/worldmind` or `/worldmind status`
- `/worldmind inspect`
- `/worldmind advance <1-365>`
- `/worldmind materialize`
- `/worldmind history`
- `/worldmind debug on`
- `/worldmind debug off`

`/worldmind advance` advances Worldmind abstract chronology and intentionally does not replay hundreds of thousands of vanilla ticks. It also intentionally uses deterministic planning rather than spawning a burst of external AI calls.

## Worldseal
Worldseal remains the earned physical sovereignty block. Exact block safety checks remain authoritative and spatial process masks bend evolution around protected areas.

Default protection radius: **24 blocks**.

See `docs/V1_2_AI.md` for the provider contract and acceptance rules.
