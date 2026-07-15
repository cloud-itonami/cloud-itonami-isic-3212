# ADR-0001: ImitationJewelryAdvisor ⊣ Imitation Jewellery Workshop Plant Operations Governor architecture

## Status

Accepted. `cloud-itonami-isic-3212` promoted from `:spec` to
`:implemented` in the `kotoba-lang/industry` registry, following the
verified fresh-scaffold protocol established by prior actors in this
fleet.

## Context

`cloud-itonami-isic-3212` publishes an OSS blueprint for imitation-
jewellery-workshop **plant operations coordination** (production-
batch base-metal-type/plating-thickness/weight/defect-rate data
logging, casting/plating-equipment maintenance scheduling, safety-
concern flagging, and outbound product shipment coordination). Like
every actor in this fleet, the blueprint alone is not an
implementation: this ADR records the governed-actor architecture that
promotes it to real, tested code, following the same langgraph
StateGraph + independent Governor + Phase 0->3 rollout pattern
established across the cloud-itonami fleet.

Identity ({:id "3212" :name "Manufacture of imitation jewellery and
related articles"}) was independently verified against a fresh clone
of `kotoba-lang/industry`'s `resources/kotoba/industry/registry.edn`
before any work began, per this fleet's ID/name-mismatch caution
(prior agents in this fleet have mislabeled their assigned ISIC
class). The entry's own `:repo` field pointed at a stale, never-
created `gftdcojp/cloud-itonami-C3212` placeholder; the real
`cloud-itonami` org target name was independently confirmed 404 via
`gh api repos/cloud-itonami/cloud-itonami-isic-3212` before
scaffolding began.

The closest domain analog is `cloud-itonami-isic-3211` (Manufacture of
jewellery and related articles): both are back-office coordination
actors for a fixed processing WORKSHOP with casting/finishing
equipment and a real safety/consumer-protection dimension, and both
share the same four-op shape
(`:log-production-batch`/`:schedule-maintenance`/
`:flag-safety-concern`/`:coordinate-shipment`) and the same two-entity
verified/registered gate structure (equipment for maintenance
scheduling, batch for shipment coordination). This build mirrors
`cloud-itonami-isic-3211`'s architecture closely but deliberately
diverges on the regulatory-attestation dimension, per this fleet's
explicit design instruction for this vertical: ISIC 3212 (imitation/
costume jewellery, base-metal casting + plating) has NO precious-metal
purity concern, so 3211's hallmark/purity-certification-authority
permanent block does not transplant unchanged. Instead this vertical's
permanent, PERMANENT block guards a genuinely different regulatory
surface -- materials safety, specifically lead/cadmium-content
compliance of base-metal alloys and plating chemistry, a consumer-
protection concern this vertical actually has that 3211's precious-
metal jewellery does not (gold/silver/platinum/palladium are not
lead/cadmium-content compliance risks in the way zinc-alloy/brass
base-metal castings and their plating baths can be). Concretely:
`:log-production-batch`'s production-batch fields become
`:base-metal-type` (closed set spanning zinc-alloy/brass/copper/
nickel-silver/stainless-steel, replacing 3211's precious-metal-type
set) and `:plating-thickness-microns` (a genuinely new field for this
vertical -- imitation jewellery is DEFINED by a thin metal-plating
layer over a base metal, unlike 3211's own `:purity-permille` which
measures a homogeneous precious-metal alloy's own fineness, not a
coating), alongside `:weight-grams` and `:defect-rate-percent` carried
over unchanged in shape; and the permanent certification-authority
block becomes `:issue-lead-compliance-certification?` (replacing
3211's `:issue-hallmark-certification?`) -- this actor is never the
accredited testing laboratory that certifies a batch's materials-
safety/lead-content compliance, the same "no phase, no human override"
posture as 3211's own hallmark block.

This vertical has NO pre-existing `kotoba-lang/imitjewellerymfg`-style
capability library to wrap (verified: no such repo exists, and no
`jewel`/`jewelry`/`jewellery`-named repo exists in `kotoba-lang`
either, via GitHub code/repo search -- the one incidental hit,
`kotoba-lang/com-baker-hughes-jewel`, is an unrelated oilfield-
services JewelSuite integration, not a jewellery-manufacturing
capability library). This build therefore uses self-contained domain
logic -- pure functions in `imitjewellerymfg.registry` (equipment/
batch verification, shipment-quantity recompute, base-metal-type
validation, plating-thickness plausibility validation, weight-grams
plausibility validation, defect-rate plausibility validation) are
re-verified independently by the governor, the same "ground truth, not
self-report" discipline established across prior actors (most
directly `cloud-itonami-isic-3211`'s `jewellerymfg.registry`).

This blueprint's own `:itonami.blueprint/governor` keyword,
`:imitation-jewellery-workshop-plant-operations-governor`, is
grep-verified UNIQUE fleet-wide (`gh search code
"imitation-jewellery-workshop-plant-operations-governor" --owner
cloud-itonami`, zero hits before this repo was created). The
`:lead-compliance-certification-blocked` governor rule keyword is
likewise grep-verified UNIQUE (`gh search code
"lead-compliance-certification-blocked" --owner cloud-itonami`, zero
hits before this repo was created).

## Decision

### Decision 1: Self-contained domain logic (no external imitation-jewellery-manufacturing capability library to wrap)

Unlike actors that delegate to pre-existing domain libraries, this
imitation-jewellery-workshop vertical has NO pre-existing capability
library to wrap. The equipment/batch-verification / shipment-quantity
/ base-metal-type / plating-thickness / weight-grams / defect-rate
validation functions live as pure functions in
`imitjewellerymfg.registry` and are re-verified independently by
`imitjewellerymfg.governor` -- the same "ground truth, not self-report"
discipline established across prior actors (most directly
`cloud-itonami-isic-3211`'s `jewellerymfg.registry`).

### Decision 2: Coordination, not control — scope boundary at the back-office

This actor is **strictly back-office coordination** of imitation-
jewellery-workshop plant operations. It does NOT:
- Control casting or plating equipment directly
- Make workshop-safety, security, or materials-safety/lead-content-compliance decisions (exclusive to the human workshop supervisor / accredited testing laboratory)
- Actuate casting/plating equipment
- Self-issue a materials-safety/lead-content compliance certification

All proposals are `:effect :propose` only. The advisor proposes; the
governor validates; escalation paths funnel to human workshop-
supervisor approval. This is not a replacement for the supervisor's
authority or the testing laboratory's authority — it is a
proposal-screening and documentation layer.

**CRITICAL SAFETY/SECURITY BOUNDARY**: imitation-jewellery
manufacturing is a safety- and security-relevant domain (plating-
chemical (acid/cyanide-bath) materials-safety hazard, lead/cadmium-
content consumer-protection exposure, theft/security exposure
inherent to finished-goods inventory, consumer-protection consequence
downstream). Safety-concern flagging NEVER auto-commits. All safety
concerns escalate immediately to human review.

### Decision 3: Safety-concern escalation — always human sign-off

`:flag-safety-concern` (materials-safety lead/cadmium-content-
compliance concern, plating-chemical hazard, theft/security concern)
ALWAYS escalates, never auto-commits. This is not a "low-stakes
proposal" — it is a circuit-breaker that must reach human authority.

### Decision 4: Two independent verified/registered gates (equipment AND batch), not one

Like `cloud-itonami-isic-3211`, this vertical has TWO entity kinds
each gating a different op: `:schedule-maintenance` independently
verifies the referenced **equipment** unit's own `:verified?`/
`:registered?` fields; `:coordinate-shipment` independently verifies
the referenced **batch**'s own `:verified?`/`:registered?` fields.
Both are the same "workshop/batch record must be independently
verified/registered before any action" HARD invariant applied to the
two distinct record kinds this domain actually has.
`:coordinate-shipment` additionally independently recomputes whether
a batch's own recorded shipped-to-date unit quantity plus the
proposal's own claimed unit quantity would exceed the batch's own
recorded production quantity — never taken on the advisor's
self-report.

### Decision 5: HARD invariants (no override)

Four HARD governor invariants (elaborated into thirteen concrete
checks in `imitjewellerymfg.governor`, mirroring `cloud-itonami-isic-
3211`'s own elaboration of its HARD invariants into concrete checks,
with the materials-safety/lead-content-compliance block replacing the
hallmark/purity-certification block per Decision 2 above) block
proposals and cannot be overridden by human approval:
1. Workshop/batch record (equipment for maintenance, batch for shipment) must be independently verified/registered before any action is taken against it, and a shipment's quantity must independently recompute within the batch's own logged production quantity
2. Proposals must be `:effect :propose` only (never direct equipment control)
3. Direct casting/plating-equipment control, equipment actuation, or self-issued materials-safety/lead-content compliance certification is permanently blocked
4. The op allowlist is closed — `:log-production-batch`/`:schedule-maintenance`/`:flag-safety-concern`/`:coordinate-shipment` only

## Consequences

(+) Imitation-jewellery-workshop plant operations back-office now has
a documented, governed, auditable coordination layer that funnels all
decisions through independent validation before human approval.

(+) The "coordination, not control" boundary is explicit in code: all
`:effect :propose`, all real-world actuation requires human workshop-
supervisor sign-off, and no materials-safety/lead-content compliance
certification can ever be self-issued.

(+) Scope is bounded and verifiable: four HARD invariants (elaborated
into thirteen concrete governor checks) protect against scope creep
into unauthorized equipment operation, equipment actuation, or
materials-safety/lead-content-certification self-issuance. Safety
concerns are a circuit-breaker, not a threshold.

(+) Safety-critical discipline is explicit: safety-concern flagging
cannot be rate-limited, suppressed, or auto-decided by phase gate.
Human review is mandatory.

(-) Still a simulation/proposal layer, not a real workshop-operations
control system. Equipment actuation, line operation, and materials-
safety/lead-content compliance certification issuance remain human-/
institution-controlled via external channels.

(-) No integration with real workshop-management databases (equipment
telemetry, batch tracking, freight dispatch, testing-laboratory
APIs) — this is a standalone coordinator blueprint.

## Verification

- `cloud-itonami-isic-3212`: `clojure -M:test` green (all tests pass;
  see the superproject ADR and `kotoba-lang/industry` registry entry
  for the exact `Ran N tests containing M assertions, 0 failures, 0
  errors` output, verified from an independent fresh clone), `clojure
  -M:lint` clean, `clojure -M:dev:run` demo narrative exercises
  proposal submission, escalation, and every HARD-hold scenario
  directly (not-propose-effect, unknown-op, equipment-not-verified,
  batch-not-verified, shipment-quantity-exceeded, equipment-actuate-
  blocked, lead-compliance-certification-blocked, already-scheduled,
  invalid-base-metal-type, invalid-plating-thickness,
  invalid-weight, invalid-defect-rate).
- All source is `.cljc` (portable ClojureScript / JVM / nbb) — no
  JVM-only interop; the actor graph is invoked exclusively via
  `langgraph.graph/run*` (not `.invoke`, which is not cljs-portable).
- Audit ledger is append-only, all decisions are traced; every settled
  request (commit or hold) leaves exactly one ledger fact.
- `deps.edn` pins `io.github.kotoba-lang/langgraph` and
  `io.github.kotoba-lang/langchain` via `:local/root` directly in the
  top-level `:deps` (not only under a `:dev` alias), so a bare
  `clojure -M:test` resolves offline inside the monorepo checkout.
