# Current manual and runtime verification

Source of truth: [baseline v0.2](../docs/02_requirements_and_spec/baseline_v0.2/c02_baseline_v0.2.md) and [operations](../docs/02_requirements_and_spec/operations/OP-01_CREATE.md). All commands here target a fresh disposable dev database.

## Start

Build with JDK 21: `bash ./mvnw package` (PowerShell `.\mvnw.cmd package`). Start the JAR with `--spring.profiles.active=dev`. Default HTTP is 8080; Vite on 5173 proxies the four API prefixes. Swagger: http://localhost:8080/swagger-ui/index.html.

## Reproduce core behavior

1. Select the tomorrow screening from `/movies`. Standard seats 1/2 and VIP 3/4 are a demo fixture; scripts discover actual IDs/types from API.
2. Select a standard seat and Create draft. Observe the full `DRAFT` response and reservation ID; availability is still AVAILABLE. Confirm gives CONFIRMED; Cancel releases it and keeps history.
3. Select standard+VIP, Create, Confirm: entire reservation PENDING_APPROVAL, all selected seats UNAVAILABLE, request/deadline visible.
4. Open `/approver`, enter the generated ID and Load reservation. Approve as demo actor 3 yields CONFIRMED without a release window. Owner 1 attempting the same POST gets 403. A separate request can be rejected to REJECTED and releases all seats.
5. Leave another pending request until its deadline. Background sweep / observation yields EXPIRED with `expiredAt = approvalDeadline`. Late decisions and terminal Cancel return 409.
6. Two drafts may overlap; confirming the second against a live hold/confirmed reservation returns 409 and leaves its whole DRAFT unallocated.

Create input is `{userId,screeningId,seatIds}`. Complete response is documented in [OP-01](../docs/02_requirements_and_spec/operations/OP-01_CREATE.md#7-full-response-contract): `reservationId`, `seats`, `status`, `approverId`, `decisionAt` and other recorded lifecycle metadata. Availability objects have exactly seatId/rowNumber/seatNumber/seatType/status. Default pending timeout is PT60S; a test/demo may explicitly set `--demo.approval-timeout=PT8S` without changing the policy.

## Live API check

From the repository root, with fresh dev backend running:

```bash
python backend/verify_baseline.py --base-url http://localhost:8080
```

PowerShell wrapper from backend: `.\verify-baseline.ps1`. The check creates/cancels demo reservations, observes a real deadline, discovers VIP classification from API, checks exact payloads and writes current runtime JSON to the documented evidence path. It must be run only against the intended disposable demo instance.

## Browser check

With backend and Vite running, start a local Chromium/Edge debugging listener on 9222. From repository root run `node frontend/verify-ui.mjs`. The script uses Node's built-in WebSocket/CDP, current routes and backend-derived screening/seat IDs. It does not require a frontend test dependency. Browser execution results must be reported separately from TypeScript checks or server rendering.

## Minimal administration

Follow [MANAGEMENT_MAPPING](../docs/02_requirements_and_spec/MANAGEMENT_MAPPING.md). Configure STANDARD/VIP before publishing a screening. Published starts and layouts are immutable. Removed extensions (disabled/deactivation/end times/overlaps/account previews) must not be expected in API or UI. Identity headers/IDs are the selected C08 demo convention; there are no real login credentials or external delivery provider in this phase.
