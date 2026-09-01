# Worldmind V1.2 AI Provider Contract

## Authority
External intelligence is advisory. Worldmind Core remains authoritative. AI cannot directly mutate Minecraft state.

## Persistent settings
`config/worldmind-ai.json` defaults to disabled. Provider changes are saved immediately and rebuild the provider instance while clearing stale transient advice/test state.

## Worldmind proposal response schema
Forgey direct endpoints and model-content responses ultimately resolve to:

```json
{
  "placeId": "<Worldmind place id>",
  "recommendation": "STASIS|RECLAMATION|DECAY|CONSTRUCTIVE|BLENDED",
  "confidence": 0.0,
  "intensityAdjustment": 0.0,
  "reason": "short advisory reason",
  "styleHint": "optional short style hint"
}
```

Validation rules:
- exact `placeId` match
- confidence >= 0.70 and <= 1.0
- finite numbers only
- intensity adjustment clamped to -0.15..+0.15
- Worldsealed places rejected
- place confidence below 0.55 rejected
- absence below configured minimum rejected
- unknown recommendation rejected
- reason max 240 chars; style hint max 120 chars

## Forgey direct request contract
Worldmind POSTs to the exact configured endpoint:

```json
{
  "type": "worldmind_transformation_proposal",
  "protocolVersion": "1.2",
  "model": "forgey",
  "constraints": "<strict advisory constraints>",
  "context": { "...": "compact semantic context" }
}
```

The response is the direct proposal schema above.

Connection tests use:

```json
{
  "type": "worldmind_connection_test",
  "protocolVersion": "1.2",
  "model": "forgey"
}
```

Any HTTP 2xx response counts as endpoint reachability for the Forgey bridge.

## Ollama
Base endpoint example: `http://localhost:11434`.

Worldmind POSTs to `/api/chat` with `stream:false`, `format:"json"`, a strict system constraint, and compact semantic user context. The assistant `message.content` must contain the proposal JSON.

## Compatible provider
If the configured endpoint already ends in `/v1/chat/completions`, it is used as-is. If it ends in `/v1`, `/chat/completions` is appended. Otherwise `/v1/chat/completions` is appended.

## Failure behavior
Network errors, non-2xx responses, timeout, oversized response, malformed proposal, invalid recommendation, low confidence, or failed safety validation produce no authoritative mutation and fall back to deterministic Worldmind.

## Performance
- default external request concurrency: 1
- configurable/clamped concurrency: 1..4
- default timeout: 8 seconds; clamp 1..60
- default advice cache: 5 Worldmind days; clamp 0.25..30
- response cap: 65,536 bytes
- no per-tick requests
- no Minecraft chunk APIs in AI package

## Feature toggles
`transformation` controls whether external transformation advice is requested/consumed. `structure` controls whether detailed palette/architectural intent is disclosed to the provider. Civilization/history/naming flags are persistent capability gates for later provider call sites and are off by default.
