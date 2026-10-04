# C02 baseline demonstration

From the repository root (Java 21 or newer):

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd clean package
java -jar target/cinema-reservation-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

Alternatively: `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"`.
Open http://localhost:8080/swagger-ui/index.html. OpenAPI:
http://localhost:8080/v3/api-docs. All four endpoints support Try it out.

The dev profile resets in-memory H2 on restart. Users 1 and 2 exist.
Screening 1 starts one day after initialization; screening 2 starts ten minutes
later (creation cutoff has passed); screening 3 started one hour earlier.
Hall A has seat IDs 1-4, row 1, seat numbers 1-4.
Reservation 100 is DRAFT for screening 3/seat 1; reservation 101 is CONFIRMED
for screening 3/seat 2. Both belong to user 1. New reservation IDs start at 1.
Restart to reset IDs and refresh relative screening times.

Confirm/Cancel require header `X-User-Id: 1`. This explicit baseline caller
identity convention is not authenticated login. User 2 demonstrates 403 for a
non-owner; a missing header returns 400.

Execute in order on a fresh dev instance:

| Request | Expected result |
|---|---|
| GET `/screenings/1/availability` | 200; all four AVAILABLE |
| GET `/screenings/999999/availability` | 404; Screening not found. |
| POST `/reservations`, body `{"userId":1,"screeningId":1,"seatIds":[1,2]}` | 201; `{"id":1}`, DRAFT |
| Repeat same Create | 201; `{"id":2}`, overlapping DRAFT allowed |
| GET `/screenings/1/availability` | 200; still all AVAILABLE |
| POST `/reservations`, body `{"userId":1,"screeningId":2,"seatIds":[1]}` | 409; creation cutoff |
| POST `/reservations/1/confirm`, header user 1 | 200; CONFIRMED |
| GET `/screenings/1/availability` | 200; seats 1,2 UNAVAILABLE |
| POST `/reservations/2/confirm`, header user 1 | 409; conflict; remains DRAFT |
| Repeat same Create for screening 1/seats 1,2 | 201; `{"id":3}`, despite confirmed seats |
| POST `/reservations/1/cancel`, header user 1 | 200; CANCELLED, cancelledAt set |
| GET `/screenings/1/availability` | 200; all AVAILABLE again |
| Repeat Cancel 1 | 409; already CANCELLED |
| POST `/reservations/1/confirm`, header user 1 | 409; only DRAFT can be confirmed |
| POST `/reservations/2/confirm`, header user 2 | 403; owner only |
| POST `/reservations/100/confirm`, header user 1 | 409; screening started |
| POST `/reservations/101/cancel`, header user 1 | 409; screening started |
| POST `/reservations/2/cancel`, header user 1 | 200; DRAFT becomes CANCELLED |

Example terminal requests:

```powershell
curl.exe -i http://localhost:8080/screenings/1/availability
Invoke-WebRequest -UseBasicParsing -Uri http://localhost:8080/reservations -Method Post -ContentType 'application/json' -Body '{"userId":1,"screeningId":1,"seatIds":[1,2]}'
curl.exe -i -X POST -H "X-User-Id: 1" http://localhost:8080/reservations/1/confirm
curl.exe -i -X POST -H "X-User-Id: 1" http://localhost:8080/reservations/1/cancel
```

Exactly 15 minutes before start is allowed. Automated HTTP tests use a fixed
clock to reproduce this boundary. Confirm/Cancel require strictly before start.
Concurrent HTTP tests exercise Confirm/Confirm and Confirm/Cancel races.
For an executable demo against a fresh dev instance, run from backend:
`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify-baseline.ps1`.
It checks Swagger/OpenAPI and all expected HTTP statuses, printing response bodies.
Notification Service logs successful transitions after commit; rejected
operations do not invoke it. No external infrastructure is used.
Source of truth: `docs/c02_baseline_specification.md`. Draft expiration remains
TBD; DRAFT never blocks seats. No frontend changes are needed.
