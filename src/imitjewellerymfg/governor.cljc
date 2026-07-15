(ns imitjewellerymfg.governor
  "Imitation Jewellery Workshop Plant Operations Governor -- the
  independent compliance layer that earns the ImitationJewelryAdvisor
  the right to commit. The advisor has no notion of whether a piece of
  equipment it wants to schedule maintenance against has actually been
  inspected/registered, whether a batch it wants to coordinate a
  shipment against has actually been QC-verified/registered, whether a
  maintenance proposal secretly tries to ACTUATE (rather than merely
  draft-schedule) casting/plating equipment, whether a proposal
  secretly tries to self-issue a materials-safety/lead-content
  compliance certification (an authority this actor never holds),
  whether a shipment proposal's own claimed quantity would blow
  through the batch's own logged production quantity, or when an act
  stops being a coordination proposal and becomes direct casting/
  plating-line-equipment control, so this MUST be a separate system
  able to *reject* a proposal and fall back to HOLD.

  `:itonami.blueprint/governor` is
  `:imitation-jewellery-workshop-plant-operations-governor` (see
  docs/adr/0001-architecture.md).

  Checks below, ALL HARD violations except the confidence/high-stakes
  gate (SOFT -- asks a human to look, and the human may approve):

    1. Request-level propose-only  -- did the CALLER's own request
                                       actually declare `:effect
                                       :propose`? Any other value is a
                                       mis-wired/compromised caller
                                       trying to bypass proposal-only
                                       mode -- HARD, unconditional,
                                       evaluated BEFORE anything else.
    2. Closed op allowlist         -- is `:op` one of the four ops this
                                       actor is authorized to coordinate?
                                       Anything else -- HARD hold.
    3. Closed effect allowlist     -- is the PROPOSAL's own `:effect`
                                       (what would actually commit) one
                                       of the four propose-shaped
                                       effects? A proposal effect
                                       outside this set (e.g. a
                                       hallucinated `:casting-unit/
                                       actuate` or `:plating-station/
                                       run`) is the 'direct casting/
                                       plating-line-equipment control'
                                       scope violation this actor must
                                       NEVER perform -- HARD, PERMANENT,
                                       unconditional.
    4. Equipment-actuate blocked   -- for `:schedule-maintenance`, does
                                       the proposal's own `:value`
                                       declare `:actuate-equipment?
                                       true`? Directly actuating
                                       casting/plating equipment is
                                       this actor's other permanent
                                       scope boundary (see README `What
                                       this actor does NOT do`) -- HARD,
                                       PERMANENT, unconditional. NO
                                       phase and NO human approval can
                                       ever override this (see
                                       `imitjewellerymfg.phase`: this op
                                       is never a member of any phase's
                                       `:auto` set either -- two
                                       independent layers agree).
    5. Lead/cadmium-content
       compliance-certification-
       authority blocked           -- ANY proposal (any op) whose own
                                       `:value`/`:patch` declares
                                       `:issue-lead-compliance-
                                       certification? true` is
                                       attempting to self-issue a
                                       materials-safety/lead-content
                                       compliance certification -- an
                                       authority exclusively reserved
                                       to the accredited testing
                                       laboratory, never this actor --
                                       HARD, PERMANENT, unconditional.
    6. Equipment not verified/
       registered                  -- for `:schedule-maintenance`,
                                       INDEPENDENTLY verify the
                                       referenced equipment's own
                                       `:verified?` AND `:registered?`
                                       are both true
                                       (`imitjewellerymfg.registry/
                                       equipment-ready?`) -- never trust
                                       the advisor's own rationale
                                       about verification/registration
                                       status. Grounded in this
                                       blueprint's own HARD invariant
                                       ('workshop/batch record must be
                                       independently verified/
                                       registered before any action'):
                                       maintenance must never be
                                       scheduled against equipment
                                       whose own conditions have not
                                       actually been inspected or
                                       whose registration is not
                                       actually on file.
    7. Already scheduled           -- for `:schedule-maintenance`,
                                       refuses to schedule the SAME
                                       maintenance record twice, off a
                                       dedicated `:scheduled?` fact
                                       (never a `:status` value).
    8. Batch not verified/
       registered                  -- for `:coordinate-shipment`,
                                       INDEPENDENTLY verify the
                                       referenced batch's own
                                       `:verified?` AND `:registered?`
                                       are both true
                                       (`imitjewellerymfg.registry/
                                       batch-ready?`) -- never trust
                                       the advisor's own rationale.
                                       Also part of the 'workshop/batch
                                       record' HARD invariant: a
                                       batch's own verified/registered
                                       status is as much a ground-truth
                                       fact as an equipment unit's own.
    9. Shipment quantity exceeded  -- for `:coordinate-shipment`,
                                       INDEPENDENTLY recompute whether
                                       the batch's own recorded
                                       `:shipped-units` plus the
                                       proposal's own claimed `:units`
                                       would exceed the batch's own
                                       recorded `:quantity-units`
                                       (`imitjewellerymfg.registry/
                                       shipment-quantity-exceeded?`) --
                                       ground truth from the batch's
                                       own permanent fields, never a
                                       self-reported quantity claim.
   10. Invalid base-metal-type     -- for `:log-production-batch`, if
                                       the patch declares a
                                       `:base-metal-type` outside the
                                       closed known set
                                       (`imitjewellerymfg.registry/
                                       base-metal-type-valid?`), the
                                       batch record is rejected rather
                                       than let a fabricated
                                       classification through.
   11. Invalid plating-thickness   -- for `:log-production-batch`, if
                                       the patch declares a
                                       `:plating-thickness-microns`
                                       that is not a physically
                                       plausible plating reading
                                       (`imitjewellerymfg.registry/
                                       plating-thickness-microns-
                                       valid?`), the batch record is
                                       rejected rather than let a
                                       fabricated/gauge-error reading
                                       through.
   12. Invalid weight-grams        -- for `:log-production-batch`, if
                                       the patch declares a
                                       `:weight-grams` that is not a
                                       physically plausible batch
                                       finished-piece-weight reading
                                       (`imitjewellerymfg.registry/
                                       weight-grams-valid?`), the batch
                                       record is rejected rather than
                                       let a fabricated/scale-error
                                       reading through.
   13. Invalid defect-rate         -- for `:log-production-batch`, if
                                       the patch declares a
                                       `:defect-rate-percent` that is
                                       not a physically plausible
                                       reading
                                       (`imitjewellerymfg.registry/
                                       defect-rate-valid?`), the batch
                                       record is rejected rather than
                                       let fabricated/sensor-error data
                                       through.
   14. Confidence floor / high-
       stakes gate                  -- LLM confidence below threshold,
                                       OR the proposal's own `:stake` is
                                       in `high-stakes`
                                       (`:coordination/safety-concern`,
                                       ALWAYS set for `:flag-safety-
                                       concern`) -- escalate to a human
                                       workshop supervisor. SOFT: the
                                       human may approve."
  (:require [imitjewellerymfg.registry :as registry]
            [imitjewellerymfg.store :as store]))

(def confidence-floor 0.6)

(def allowed-ops
  "The closed allowlist of coordination proposals this actor may ever
  route -- see README `What this actor does`."
  #{:log-production-batch :schedule-maintenance
    :flag-safety-concern :coordinate-shipment})

(def allowed-proposal-effects
  "The closed allowlist of SSoT-mutation effects a proposal may declare
  -- all four are propose-shaped drafts, NEVER a direct casting/
  plating-equipment-control effect."
  #{:batch/upsert :maintenance/schedule
    :safety-concern/flag :shipment/propose})

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Safety concerns are the one op in this domain that always demands
  human eyes regardless of confidence."
  #{:coordination/safety-concern})

;; ----------------------------- checks -----------------------------

(defn- no-propose-effect-violations
  "HARD, unconditional, evaluated first: the caller's own request MUST
  declare `:effect :propose` -- any other value is a mis-wired or
  compromised caller trying to bypass proposal-only mode."
  [{:keys [effect]}]
  (when (not= effect :propose)
    [{:rule :not-propose-effect
      :detail (str "request :effect must be :propose only (received: " (pr-str effect) ")")}]))

(defn- unknown-op-violations
  "HARD: `:op` must be one of the closed allowlist this actor
  coordinates -- never route an unrecognized operation."
  [{:keys [op]}]
  (when-not (contains? allowed-ops op)
    [{:rule :unknown-op
      :detail (str op " is not on this actor's closed operation allowlist")}]))

(defn- equipment-control-blocked-violations
  "HARD, PERMANENT: the proposal's own `:effect` -- what would actually
  commit -- must be within the closed propose-shaped effect allowlist.
  Anything else (direct casting/plating-line-equipment control, a
  fabricated actuation effect) is this actor's central scope
  boundary."
  [proposal]
  (when-not (contains? allowed-proposal-effects (:effect proposal))
    [{:rule :equipment-control-blocked
      :detail (str "proposal :effect (" (pr-str (:effect proposal))
                   ") may indicate direct casting/plating-line-equipment control, permanently blocked")}]))

(defn- actuate-equipment-blocked-violations
  "HARD, PERMANENT, unconditional: a `:schedule-maintenance` proposal
  whose own `:value` declares `:actuate-equipment? true` is attempting
  to directly actuate casting/plating equipment -- this actor may only
  ever propose/schedule a DRAFT maintenance window, never actuate
  equipment directly. No override, ever."
  [{:keys [op]} proposal]
  (when (and (= op :schedule-maintenance)
             (true? (:actuate-equipment? (:value proposal))))
    [{:rule :actuate-equipment-blocked
      :detail "direct casting/plating-equipment actuation is permanently blocked -- draft scheduling only"}]))

(defn- lead-compliance-certification-blocked-violations
  "HARD, PERMANENT, unconditional: ANY proposal (any op) whose own
  `:value`/`:patch` declares `:issue-lead-compliance-certification?
  true` is attempting to self-issue a materials-safety/lead-content
  compliance certification -- an authority exclusively reserved to the
  accredited testing laboratory, never this actor. No phase and no
  human approval can ever override this."
  [proposal]
  (let [payload (or (:value proposal) (:patch proposal))]
    (when (true? (:issue-lead-compliance-certification? payload))
      [{:rule :lead-compliance-certification-blocked
        :detail "self-issuing a materials-safety/lead-content compliance certification is permanently blocked -- exclusive authority of the accredited testing laboratory"}])))

(defn- equipment-not-verified-violations
  "For `:schedule-maintenance`, INDEPENDENTLY verify the referenced
  equipment exists and is both `:verified?` AND `:registered?` --
  never trust the advisor's own report. This is the HARD invariant
  ('workshop/batch record must be independently verified/registered
  before any action')."
  [{:keys [op]} proposal st]
  (when (= op :schedule-maintenance)
    (let [equipment-id (:equipment-id (:value proposal))
          eq (and equipment-id (store/equipment-unit st equipment-id))]
      (when-not (and eq (registry/equipment-ready? eq))
        [{:rule :equipment-not-verified
          :detail (str equipment-id " is unverified or unregistered, or does not exist -- maintenance may not be scheduled without a verified, registered equipment record")}]))))

(defn- already-scheduled-violations
  "For `:schedule-maintenance`, refuses to schedule the SAME
  maintenance record twice, off a dedicated `:scheduled?` fact (never
  a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :schedule-maintenance)
    (when (store/maintenance-already-scheduled? st subject)
      [{:rule :already-scheduled
        :detail (str subject " is already scheduled")}])))

(defn- batch-not-verified-violations
  "For `:coordinate-shipment`, INDEPENDENTLY verify the referenced
  batch exists and is both `:verified?` AND `:registered?` -- never
  trust the advisor's own report. Also part of the 'workshop/batch
  record must be independently verified/registered before any action'
  HARD invariant."
  [{:keys [op]} proposal st]
  (when (= op :coordinate-shipment)
    (let [batch-id (:batch-id (:value proposal))
          b (and batch-id (store/batch st batch-id))]
      (when-not (and b (registry/batch-ready? b))
        [{:rule :batch-not-verified
          :detail (str batch-id " is unverified or unregistered, or does not exist -- shipment may not be coordinated without a verified, registered batch record")}]))))

(defn- shipment-quantity-exceeded-violations
  "For `:coordinate-shipment`, INDEPENDENTLY recompute whether the
  batch's own recorded shipped-to-date quantity plus the proposal's
  own claimed quantity would exceed the batch's own recorded
  `:quantity-units` -- ground truth from the batch's own permanent
  fields, never a self-reported quantity claim."
  [{:keys [op]} proposal st]
  (when (= op :coordinate-shipment)
    (let [{:keys [batch-id units]} (:value proposal)
          b (and batch-id (store/batch st batch-id))]
      (when (and b (registry/shipment-quantity-exceeded? b units))
        [{:rule :shipment-quantity-exceeded
          :detail (str batch-id "'s recorded production quantity (" (:quantity-units b)
                       " units) would be exceeded by existing shipments (" (:shipped-units b 0.0)
                       " units) plus this request (" units " units)")}]))))

(defn- invalid-base-metal-type-violations
  "For `:log-production-batch`, if the patch declares a
  `:base-metal-type` outside the closed known set, reject rather than
  let a fabricated base-metal-type classification through."
  [{:keys [op]} proposal]
  (when (= op :log-production-batch)
    (let [base-metal-type (:base-metal-type (:value proposal))]
      (when (and (some? base-metal-type) (not (registry/base-metal-type-valid? base-metal-type)))
        [{:rule :invalid-base-metal-type
          :detail (str base-metal-type " is not a known base-metal-type value")}]))))

(defn- invalid-plating-thickness-violations
  "For `:log-production-batch`, if the patch declares a
  `:plating-thickness-microns` that is not a physically plausible
  plating reading, reject rather than let fabricated/gauge-error data
  through."
  [{:keys [op]} proposal]
  (when (= op :log-production-batch)
    (let [thickness (:plating-thickness-microns (:value proposal))]
      (when (and (some? thickness) (not (registry/plating-thickness-microns-valid? thickness)))
        [{:rule :invalid-plating-thickness
          :detail (str thickness "µm is outside the physically plausible plating-thickness range")}]))))

(defn- invalid-weight-violations
  "For `:log-production-batch`, if the patch declares a
  `:weight-grams` that is not a physically plausible batch finished-
  piece-weight reading, reject rather than let fabricated/scale-error
  data through."
  [{:keys [op]} proposal]
  (when (= op :log-production-batch)
    (let [weight (:weight-grams (:value proposal))]
      (when (and (some? weight) (not (registry/weight-grams-valid? weight)))
        [{:rule :invalid-weight
          :detail (str weight "g is outside the physically plausible batch finished-piece-weight range")}]))))

(defn- invalid-defect-rate-violations
  "For `:log-production-batch`, if the patch declares a
  `:defect-rate-percent` that is not a physically plausible reading,
  reject rather than let fabricated/sensor-error data through."
  [{:keys [op]} proposal]
  (when (= op :log-production-batch)
    (let [rr (:defect-rate-percent (:value proposal))]
      (when (and (some? rr) (not (registry/defect-rate-valid? rr)))
        [{:rule :invalid-defect-rate
          :detail (str rr "% is outside the physically plausible defect-rate range")}]))))

(defn check
  "Censors an ImitationJewelryAdvisor proposal against the governor
  rules. Returns {:ok? bool :violations [..] :confidence c :escalate?
  bool :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (no-propose-effect-violations request)
                           (unknown-op-violations request)
                           (equipment-control-blocked-violations proposal)
                           (actuate-equipment-blocked-violations request proposal)
                           (lead-compliance-certification-blocked-violations proposal)
                           (equipment-not-verified-violations request proposal st)
                           (already-scheduled-violations request st)
                           (batch-not-verified-violations request proposal st)
                           (shipment-quantity-exceeded-violations request proposal st)
                           (invalid-base-metal-type-violations request proposal)
                           (invalid-plating-thickness-violations request proposal)
                           (invalid-weight-violations request proposal)
                           (invalid-defect-rate-violations request proposal)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
