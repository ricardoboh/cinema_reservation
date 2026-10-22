# Cinema Reservation System ? Cinema Management (Phase 1)

Spring Boot/JPA, H2 and React/Vite. Team Deadlock. No authentication, payments,
Spring Security or PostgreSQL are implemented. Administration is an unprotected
local development/demo interface. Existing account screens remain visual previews.
Phase 2 will integrate trusted identities and authorization for management and approval.

## Start (Windows PowerShell)

Requirements: JDK 21+, Node 22.12+, npm. Run from the repository root in two terminals.

Backend:

```powershell
cd backend
.\mvnw.cmd package
java -jar target/cinema-reservation-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

Frontend:

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

Open http://127.0.0.1:5173. Administration: http://127.0.0.1:5173/admin.
Swagger: http://localhost:8080/swagger-ui/index.html.
Git Bash/Linux/macOS: use `bash ./mvnw package` and `npm` in place of Windows commands.
Vite proxies movies, halls, screenings and reservations to port 8080; HTML navigation
for shared movie/reservation paths still serves the React application.

## Management and booking

1. Open /admin/movies. Create a movie with title, genre, description, runtime,
   year and optional HTTP(S) poster URL. Posters use 2:3 proportions and a
   typographic fallback for missing/broken images. Edit or deactivate movies;
   deletion is allowed only before screenings reference them.
2. Open /admin/halls. Save a hall, generate a rectangular layout (1?30 rows,
   1?40 seats per row), then select a paint mode and click seats to mark VIP,
   standard, disabled or enabled. Gold outlines identify VIP seats.
3. Open /admin/screenings. Choose an active movie/hall and a local date/time.
   The API stores UTC instants and derives the end from movie duration.
   Hall overlaps are rejected; adjacent screenings are allowed.
4. Open /movies, select a movie and actual upcoming screening, then select seats.
   Only screenings more than 15 minutes away are listed for booking.
   Physical seats and per-screening availability come from the API.
5. Create a draft, then confirm. Drafts do not hold seats. Standard seats confirm
   directly; any VIP seat sends the entire reservation through the existing
   pending approval workflow with demo approver 3. Use /approver in the same
   browser to approve/reject before the finite deadline. Pending holds expire
   automatically; cancellation, rejection and expiration release allocations.
6. /reservations reads live statuses for IDs remembered by this browser. This is
   a demo list, not an authenticated account history or global approval queue.

The same physical seat can be booked independently across screenings. Confirmation
retains the existing pessimistic screening lock and conflict checks. Creation also
locks the hall to serialize against layout changes. Availability distinguishes
AVAILABLE, UNAVAILABLE (occupied/held), and DISABLED; VIP is persistent classification.

Safety restrictions are conservative: any reservation history, including drafts or
terminal records, blocks replacing that hall's layout and changing/cancelling that
screening. A seat with reservation history cannot change classification or active
state. This preserves historic reservations and approval snapshots. Hall names can
change without changing physical seat identity. Deactivation hides future booking
options; existing reservations retain their lifecycle. Editing movie runtime does
not silently reschedule existing screenings, whose end times are stored snapshots.

## H2 and fixtures

The dev profile remains disposable in-memory H2 (create-drop). SQL seeds demo
users 1/2/3; transactional DemoFixtures uses generated IDs and management services
to create three movies, two 8?10 halls, 20 VIP seats per hall in back rows, and six
screenings across the next three days. Restarting dev resets all changes. Tests use
isolated in-memory H2 and do not load dev fixtures. The default profile remains
file-backed H2 at backend/data/cinema-reservation when launched from backend.

Legacy constructors/string columns remain for existing lifecycle tests and older
records; new administration uses Movie and CinemaHall relationships. Old file-H2
records are not automatically converted into managed movies/halls. Use a fresh dev
profile to demonstrate Phase 1; a production migration is outside this phase.

## REST management API

| Method | Path | Purpose |
|---|---|---|
| GET, POST | /movies | List/create |
| GET, PUT, DELETE | /movies/{id} | Details/edit/safe delete |
| POST | /movies/{id}/deactivate | Deactivate |
| GET, POST | /halls | List/create |
| GET, PUT | /halls/{id} | Details/edit |
| GET | /halls/{id}/seats | Physical layout |
| PUT | /halls/{id}/layout | Replace layout: rows, seatsPerRow |
| PUT | /halls/{id}/seats/{seatId} | seatType STANDARD/VIP, active |
| GET, POST | /screenings | List/create; optional movieId, upcoming filters |
| GET, PUT | /screenings/{id} | Details/edit |
| POST | /screenings/{id}/cancel | Cancel unused screening |
| GET | /screenings/{id}/availability | Existing availability, now physical hall seats |

Movie bodies: title, description, genre, durationMinutes, releaseYear, posterUrl,
active. Hall bodies: name, active. Screening bodies: movieId, hallId, startsAt
(ISO instant). Create returns 201; deletion returns 204. Validation errors return
400, missing records 404 and unsafe/conflicting operations 409 as ProblemDetail.
Existing /reservations APIs and demo X-User-Id convention remain in place.

## Checks

```powershell
cd backend
.\mvnw.cmd test
# In another terminal:
cd frontend
npm.cmd run lint
npm.cmd run build
```

Build includes TypeScript checks. Management tests cover movie/hall edits, layout,
VIP and disabled seats, overlaps, independent per-screening booking, conflicts,
approval and unsafe changes. Existing lifecycle/concurrency/expiration tests remain.

Coursework baseline documents in docs/ remain historical milestone records.
