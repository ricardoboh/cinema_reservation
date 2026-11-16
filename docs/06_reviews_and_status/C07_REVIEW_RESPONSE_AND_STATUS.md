# C07 — Review response and project status

Team Deadlock; 2026-11-16. Source baseline `efef609615aed90aae3851d9a7e205345e862cb2`, branch `docs/swi-c05-c07`. This is a source/evidence assessment and documentation correction. It is not an external review, owner sign-off or proof of submission. Official C07 assignment is unavailable; the requested task defines the working scope.

## Review provenance

| Artifact | Classification and evidence | Use |
|---|---|---|
| [Root midterm report](../../PROJECT_MIDTERM_REVIEW.md) | Internal project audit dated 19. 10.; introduced by `2d5ccf9` | Historical observations at `81a2628`, not independent reviewer evidence |
| [PEER_REVIEW_C06.md](PEER_REVIEW_C06.md) | At starting HEAD byte-identical to root report; history includes documentation move `f6e6799` | Historical copy now explicitly labelled internal |
| [Křečkomat assessment](../review/C06_review) | Outgoing review of another project; introduced by `f3e1d98` | F1/F2 belong to Křečkomat, not findings against Cinema Reservation |
| [C06 test log](../review/C06_tests.txt) | Historical Křečkomat verification output: 30 PASS / 0 FAIL | Not Cinema Reservation test output |
| [Technical audit](../../TECHNICAL_AUDIT_REPORT.md) | Internal later technical audit accompanying fix `5dd4993` | More recent build/test evidence; not an external assessment |
| [C08 status](STATUS_REPORT_C08.md) | Historical status text asserts an external review and completed actions | External attribution is unsupported by available original evidence; use this C07 correction for provenance. C08 file is preserved unchanged as required by task scope |

No original incoming independent peer review was found in tracked workspace evidence. No named reviewer, original findings list, acknowledgement or submission receipt can be inferred from a filename, Git author or status narrative. The index's former attribution to Absolute Cinema is unverified. There is no separate `PROJECT_MIDTERM_REVIEW.md` in this folder.

## Responses to evidence-supported observations

Identifiers INT-01–INT-09 are local C07 tracking IDs for the nine discrepancies in section 4.2 of the internal midterm report, not invented external finding IDs. `PENDING` means owner disposition remains unconfirmed. The team response column records a proposed response prepared by this task; it does not claim a historical owner decision. Completed documentation work below was authorized by this task.

| ID / description | Evidence and present assessment | Proposed team response / corrective action | Owner decision / action status |
|---|---|---|---|
| INT-01 Legacy Part A identifiers | Internal §4.2(1); current `ReservationService.confirm`, `getStatus`, `ReservationException` exist | Explain present identifiers in C05; any C03 repair requires separate scope | PENDING; C05 explanation completed, C03 untouched |
| INT-02 Incorrect absence-of-lock claim | `ScreeningRepository.findLockedById` uses `PESSIMISTIC_WRITE` | Document lock-before-load and caller obligations in C05 contracts | PENDING; C05 correction completed |
| INT-03 Notification/DB mismatch | `NotificationService` logs AFTER_COMMIT; YAML uses H2 | Distinguish logging from external delivery and outbox; use AS-IS C05 contracts | PENDING; C05 correction completed; architecture implementation unchanged |
| INT-04 Sweep configuration mismatch | Historical §4.2(4) refers to hourly YAML; current base has no override, dev sets 1000, annotation fallback 1000 | Treat hourly statement as historical; document present effective values | PENDING; current C05 policy corrected; no configuration change |
| INT-05 Historical port 8081 | Base YAML has no server.port; Vite proxy targets localhost:8080 | Treat 8081 as historical/environment-dependent; do not assert current runtime from old notes | PENDING; current status clarified, no live run |
| INT-06 Historical three-state descriptions | `ReservationStatus` has DRAFT, PENDING_APPROVAL, CONFIRMED, CANCELLED, REJECTED, EXPIRED | Use current lifecycle table; preserve v0.1 history | PENDING; C05 lifecycle documented; C01/C02 untouched |
| INT-07 C03 H1/H2 incomplete/misaligned | Historical report §4.2(7); C03 remains outside this task | Supply focused C05 class and sequence diagrams without asserting completion of C03 | PENDING; C05 diagrams completed; C03 claim not closed |
| INT-08 HEAD/tag/submission assertions | Internal report used `81a2628`; this task starts at `efef609`; no remote verification performed | Pin every evidence set to its baseline; request remote tag/submission proof rather than assume it | PENDING; baseline recorded; remote assertions unresolved |
| INT-09 Historical versus current test evidence | Old C05 records report 74/0; later technical audit records fresh verification of its own baseline | Keep both as dated historical evidence; never relabel as this task's execution | PENDING; evidence classification completed; no new backend run |

Other confirmed documentation defects identified during this task:

| ID | Evidence | Prepared response / corrective action | Owner decision / action status |
|---|---|---|---|
| DOC-05 Empty C05 deliverables | Four zero-byte files at starting HEAD under `04_detailed_design` | Restore actual policy, contracts, class/sequence diagrams and exact test traceability | PENDING; documentation completed |
| PROV-07 Unsupported external attribution | Identical internal reports; no incoming original | Label copied report internal; correct navigation; request original evidence | PENDING; classification corrected, external evidence outstanding |
| EVID-05 Inaccurate historical C05 details | `ReservationResponse` publication, test package, rollback via `setRollbackOnly`; absent checker script | Add explicit corrections to historical C05 evidence and authoritative current design | PENDING; annotations completed; missing checker remains unverified |

There are no confirmed external findings against Cinema Reservation to accept or reject. Křečkomat F1/F2 dispositions remain with that project's owners in the outgoing review; they must not be copied here as Deadlock's incoming review findings. No ACCEPTED/REJECTED/DEFERRED decision is fabricated.

## Current project status

Source inspection supports reservation creation, availability, confirmation, cancellation, approval/rejection and finite pending expiration. `ReservationService`, `ExpirationService`, the policy, repositories and controllers implement these flows. React `frontend/src/App.tsx` and `reservations.ts` supply the client; this task did not execute browser or API flows. Other current catalog/admin code is outside the selected design unit and was not functionally verified here.

Pending reservations hold the entire seat set. Expiration preserves history while removing active allocation. Shared screening locks coordinate service-mediated writes. Current identity uses demo `X-User-Id`, not production authentication. Notification is post-commit logging without a durable outbox/retry. There is no global active-allocation uniqueness table. Persistent migration/restart recovery and deployment behavior are not established by this documentation task.

| Verification evidence | Result and limit |
|---|---|
| `docs/review/C06_tests.txt` | Counted 30 PASS / 0 FAIL; Křečkomat historical log, date/baseline supplied by review text |
| `docs/05_verification_and_quality/DD_DERIVED_TESTS.md` | Historical 74 tests / 0 failures/errors; source methods checked, transcript not rerun |
| `TECHNICAL_AUDIT_REPORT.md` | Internal audit dated 2026-11-16 reports 84 backend test executions, 0 failures/errors/skips after its repair, plus build/frontend/dev smoke checks; starting baseline `40de27e`. This is prior audit evidence, not this task's run |
| This task | Documentation/source/test-reference/link checks only, recorded in [completion report](../../SWI_C05_C07_COMPLETION_REPORT.md); no new backend test result |

C05 documentation is restored and linked. C06 outgoing review is technically preserved and clarified, but reviewer identity, owner responses and delivery confirmation are missing. C07 now separates internal observations, proposed responses and completed documentation actions; incoming independent review evidence remains missing. No C01–C04 or C08–C09 deliverables were edited.

## ACTION REQUIRED — evidence and next actions

- [ ] Deadlock supplies official C05/C06/C07 assignments and grading/hand-in instructions; historical citations in Křečkomat review are not available originals.
- [ ] Deadlock confirms actual C06 reviewer names/team, assessment baseline agreement, delivery date/channel and receipt. Attach missing baseline/build artifacts if used as evidence.
- [ ] Křečkomat owners provide F1/F2 decisions, rationale, corrective action or justified no-change and revision links.
- [ ] The incoming review team supplies its original unedited review, named reviewers/team, date, Cinema Reservation commit evaluated, evidence and original finding IDs.
- [ ] Deadlock supplies proof of receiving/submitting that review and confirms dispositions per original finding. Add a response row per genuine finding after receipt.
- [ ] If remote C05 tag/submission completion is claimed, supply a verifiable reference/receipt. Local narrative alone is insufficient.
- [ ] Team confirms proposed responses above; remaining C03/C08 reconciliation belongs to separately authorized work. This C07 document corrects interpretation without changing those deliverables.

Fresh builds or runtime tests may be attached later with exact baseline and environment if required for submission; they cannot replace missing independent review evidence. C06/C07 are not ready for unconditional final sign-off.
