# Deadlock Cinema — redesigned booking demo

React 19 + TypeScript + Vite, with no added runtime dependencies. The shared
layout has responsive navigation, original CSS poster artwork, accessible seat
buttons and dark/light design tokens. Dark is the default. The navigation toggle
saves `deadlock-theme` in localStorage and restores it on refresh.

## Run locally

Use two PowerShell terminals, starting in the repository root:

```powershell
# Terminal 1
cd backend
.\mvnw.cmd package
java -jar target/cinema-reservation-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

```powershell
# Terminal 2
cd frontend
npm.cmd ci
npm.cmd run dev
```

Open **http://127.0.0.1:5173**. Node 22.12+ and JDK 21+ are required.
Restarting the dev backend resets its database. Use the same frontend origin
consistently because localStorage is origin-specific. No login is required.

## Pages

| Route | Purpose |
|---|---|
| `/` | Cinematic homepage and featured catalog |
| `/movies` | Searchable catalog with a working Bookable filter |
| `/movies/demo` | Real fixture screening information |
| `/movies/beyond`, `/movies/last-light`, `/movies/after-hours` | Clearly labeled fictional editorial previews |
| `/book/1` | Real seat availability and reservation workflow |
| `/reservations` | Locally remembered IDs, with live server details and actions |
| `/approver` | Separate demo approval dashboard and ID lookup |
| `/login`, `/register` | Validated visual previews, without authentication |

The small History API router supports links, back/forward and direct navigation.
Vite distinguishes HTML navigation to `/reservations` from JSON API requests.

## Demonstrate booking and approval

1. Choose **Book a seat** on the homepage or open `/book/1`.
2. Select seats 1 and/or 2, create a reservation, then confirm its draft. The
   backend confirms directly. Cancel to release the seats.
3. Select seat 3 or 4 (or mix them with direct seats), create, then confirm.
   The backend holds the entire selection in `PENDING_APPROVAL`.
4. Open **Demo approver** in the footer, preferably in another tab. Find the
   request saved in this browser, or enter its ID. Approve or reject before
   the deadline. Decisions use fixture approver 3; details use fixture owner 1.
5. Alternatively leave it pending. The backend expires it (default 60 seconds)
   and releases the seats. Booking polls every 2 seconds while pending;
   the reservation/approver lists poll every 4 seconds. The countdown never
   changes a reservation state itself.
6. Open **My reservations** to reload known records, confirm drafts or cancel
   active bookings. Page navigation does not cancel a reservation.

Drafts do not allocate seats. Another customer may take them before confirmation;
the backend rejects conflicts. The frontend refreshes after actions and disables
seat selection when availability cannot be verified. No prices or payments are
invented. No backend file, schema, lifecycle rule or fixture was changed.

## API connections and deliberate limitations

Verified controller contracts used by the UI:

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/screenings/1/availability` | Seats, actual availability and approval flags |
| POST | `/reservations` | `{userId:1, screeningId:1, seatIds:[...]}` → draft ID |
| GET | `/reservations/:id` | Server record, with `X-User-Id: 1` |
| POST | `/reservations/:id/confirm` | Confirmation or approval request, owner 1 |
| POST | `/reservations/:id/cancel` | Cancellation, owner 1 |
| POST | `/reservations/:id/approval` | `{decision:"APPROVE"}` or `"REJECT"`, approver 3 |

There are **no** movie/screening catalog, user history, pending queue or auth
endpoints. Catalog metadata is frontend content in `src/data/catalog.ts`;
only CP1 Demo Movie books screening 1. Its title/hall are fixture metadata;
runtime and exact start time are not available through the API. The fixture
starts one day after backend initialization. Screenings 2/3 exist only for
cutoff/started backend demonstrations and are not offered for booking.

Other movies, their genres/runtimes and descriptions are fictional editorial
previews. Posters are original CSS illustrations, requiring no external images
or fonts. Search and filters operate on that frontend catalog only.

`deadlock-reservation-ids-v1` stores only reservation IDs. This is a browser-local
demo convenience, not a real account, durable history or global approval queue.
If storage is unavailable, use the displayed ID for manual lookup. Stale IDs
produce a message; IDs can be reused after a database restart, so clear this key
in browser developer tools when starting a fresh presentation. IDs from other
fixture owners cannot be read. A real service needs catalog endpoints, an
authenticated owner-history endpoint and an authorized pending-queue endpoint.

Account forms validate locally and explicitly report that login/registration
are unavailable. They never send credentials, store passwords or claim account
creation. Route separation is presentation only and provides no security.

## Checks and browser verification

```powershell
cd frontend
npm.cmd run lint
npx.cmd tsc -b
npm.cmd run build
```

For automated browser verification, start both servers plus a local Chromium or
Edge instance with remote debugging port 9222, then **from `frontend`** run:

```powershell
node verify-redesign.mjs
```

It creates real demo reservations: direct confirmation/cancellation, mixed-seat
approval through the approver page, rejection, pending cancellation, conflicts,
real expiration, and late approval. It also checks routes, theme persistence,
mobile overflow, account previews and offline availability recovery. Allow about
90 seconds and ensure seats 1/3 are available first. Screenshots and the report
are written to ignored `backend/target/redesign-*` files. No new browser-test
dependency is required. The old `verify-ui.mjs` targets the historical baseline
below and should not be used against the redesign.

`npm run build` produces static files only. For the integrated demo use Vite dev;
static deployment would require a SPA fallback and API reverse proxy. Plain
`vite preview` is not the documented integrated runtime.

## Historical frontend notes — before the redesign

The following notes are retained for milestone history. They describe the old
single-page UI and do not describe the redesigned routes or controls above.

### C02 v0.2 React/Vite demo

See [the full development and manual guide](../backend/MANUAL-TESTING.md) for
fixtures, API choices, deadline behavior, startup commands and flows A–F.

```powershell
npm.cmd ci
npm.cmd run lint
npm.cmd run build
npm.cmd run dev
```

Open http://127.0.0.1:5173 with the backend dev profile on port 8080. Vite proxies
both API paths. The light, wide layout keeps the screen and seat map central and
puts booking state and the separate DEMO Approver controls alongside it. Narrow
screens use a single column. Seat availability and approval policy come from the
backend; movie/hall prose describes known dev fixtures.

Customer 1 creates/requests/cancels; Approver 3 decides. Seats 1/2 confirm directly;
3/4 require approval; mixed selections hold all seats. The Approver caller selector
also allows Owner 1 to demonstrate a real 403. Two tabs with competing drafts
show a real 409. Pending state polls status and availability every second until a
backend terminal/confirmed outcome is observed. Countdown is display-only and
never determines a transition. A terminal booking permits a fresh draft.

No new dependencies, authentication, catalogue, payment or separate frontend.
No transitions are mocked: Create retrieves its backend record and every mutation
reloads status/availability. Stale availability disables selection until a
successful refresh. Page reload does not cancel the backend booking.

Browser verification: from the repository root, `node frontend/verify-ui.mjs`
with Chromium/Edge listening on local debugging port 9222. Evidence is written to
ignored backend/target files. The script uses existing Node capabilities only.
