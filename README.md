# cloud-itonami-isic-3212: Manufacture of imitation jewellery and related articles

Open Business Blueprint for **ISIC 3212**: manufacture of imitation jewellery and related articles — an autonomous "actor" (LLM advisor behind an independent Governor, langgraph-clj StateGraph, append-only audit ledger) that coordinates back-office **imitation-jewellery-workshop plant operations**: production-batch data logging (base-metal-type/plating-thickness/weight/defect-rate), casting/plating-equipment maintenance scheduling, safety-concern flagging, and outbound product shipment coordination.

This repository designs a forkable OSS business for imitation-jewellery-workshop
plant operations: run by a qualified operator so a workshop keeps its
own operating records instead of renting a closed SaaS.

## Scope: plant operations coordination, not workshop-line control

ISIC 3212 covers the **imitation-jewellery workshop** that casts
(rings/chains/pendants/bracelets/earrings in zinc-alloy/brass/copper/
nickel-silver/stainless-steel), plates (gold/silver/rhodium flash or
standard plating over the base metal), and finishes the resulting
costume/fashion jewellery and related articles. Unlike ISIC 3211
(precious-metal jewellery), this vertical's central regulatory concern
is NOT hallmarking/purity-assay of a precious metal — it is materials
safety: base-metal alloys and plating chemistry can carry lead/
cadmium content subject to consumer-product restricted-substance
limits, and plating baths (acid/cyanide-based chemistry) are
themselves a workplace hazard. This actor coordinates the back-office
record keeping around that workshop — it never touches the casting/
plating equipment directly, and it is never the accredited testing
laboratory that certifies a batch's materials-safety/lead-content
compliance.

## What this actor does

Proposes **plant operations coordination**, not equipment operation:
- `:log-production-batch` — casting/plating/finishing batch, output-quality data logging (administrative, not an operational decision)
- `:schedule-maintenance` — casting/plating-equipment maintenance scheduling proposal
- `:flag-safety-concern` — surface a materials-safety (lead/cadmium-content compliance, plating-chemical hazard)/theft-security concern (always escalates)
- `:coordinate-shipment` — outbound product shipment coordination proposal

## What this actor does NOT do

**CRITICAL SCOPE BOUNDARY — this is a safety- and security-relevant
domain** (casting/plating-line equipment, plating-chemical (acid/
cyanide-bath) materials hazard, lead/cadmium-content consumer-
protection exposure, theft/security exposure inherent to a finished-
goods inventory, consumer-protection consequence downstream):

- Does NOT control casting or plating equipment directly
- Does NOT make workshop-safety, security, or materials-safety/lead-content-compliance decisions (that's the workshop supervisor's / accredited testing laboratory's exclusive human/institutional authority)
- Does NOT actuate casting/plating equipment (human workshop supervisor decides)
- Does NOT self-issue a materials-safety/lead-content compliance certification (the accredited testing laboratory's exclusive authority — a PERMANENT, unconditional block)
- ONLY proposes/coordinates operations back-office; all actuation and compliance certification requires explicit human/institutional authority
- Safety-concern flagging ALWAYS escalates — never auto-decided, no confidence threshold or phase below escalation

## Architecture

Classic governed-actor pattern (`imitjewellerymfg.operation/build`, a langgraph-clj StateGraph):
1. **`imitjewellerymfg.advisor`** (sealed intelligence node, `ImitationJewelryAdvisor`): proposes decisions only, never commits
2. **`imitjewellerymfg.governor`** (independent, `Imitation Jewellery Workshop Plant Operations Governor`): validates against domain rules, re-derived from `imitjewellerymfg.registry`'s pure functions and `imitjewellerymfg.store`'s SSoT -- never trusts the advisor's own self-report
   - HARD invariants (always `:hold`, no override):
     - Workshop/batch record must be independently verified/registered (`:verified?` AND `:registered?`) before any action is taken against it (equipment before maintenance scheduling, batch before shipment coordination)
     - The request's own `:effect` must be `:propose` (never a direct-write bypass)
     - `:op` must be in the closed four-op allowlist
     - The proposal's own `:effect` must be one of the four propose-shaped effects (no direct casting/plating-line-equipment control)
     - Directly actuating casting/plating equipment (`:actuate-equipment? true`) is a PERMANENT, unconditional block
     - Self-issuing a materials-safety/lead-content compliance certification (`:issue-lead-compliance-certification? true`, any op) is a PERMANENT, unconditional block
     - A shipment may not push a batch's own recorded shipped quantity past its own logged production quantity (independently recomputed)
     - No double-scheduling the same maintenance record
     - No fabricated `:base-metal-type` value on a production-batch patch
     - No physically implausible `:plating-thickness-microns` value on a production-batch patch
     - No physically implausible `:weight-grams` value on a production-batch patch
     - No physically implausible `:defect-rate-percent` value on a production-batch patch
   - ESCALATE (always human sign-off, overridable by a human):
     - `:flag-safety-concern` always escalates, regardless of confidence
     - Low-confidence proposals
3. **`imitjewellerymfg.phase`** (Phase 0->3 rollout): `:schedule-maintenance`/`:flag-safety-concern`/`:coordinate-shipment` are NEVER in any phase's `:auto` set (permanent, matching the governor's own posture); only `:log-production-batch` may auto-commit at phase 3 when clean
4. **`imitjewellerymfg.store`** (append-only audit ledger + SSoT): a single `MemStore` backend behind a `Store` protocol (see ns docstring for why a second Datomic-backed backend is out of scope for this build)

## Development

```bash
# Run tests (top-level deps.edn already pins langgraph+langchain local/root)
clojure -M:test

# Run tests via the workspace :dev override alias (equivalent, kept for sibling-repo parity)
clojure -M:dev:test

# Run the demo
clojure -M:dev:run

# Lint
clojure -M:lint
```

## Status

`:implemented` — `governor.cljc`/`store.cljc`/`advisor.cljc`/`registry.cljc` + `deps.edn` complete the module set; tests green, demo runnable, langgraph-clj integration verified.

## License

AGPL-3.0-or-later
