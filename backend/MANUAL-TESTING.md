# CP1 manual testing

From `backend/`, run:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Open http://localhost:8080/swagger-ui/index.html and expand `POST /reservations`.
Select **Try it out**, paste a request below, and select **Execute**.
The OpenAPI document is at http://localhost:8080/v3/api-docs.
The existing Springdoc dependency supplies both endpoints without extra configuration.

The opt-in `dev` profile uses a disposable in-memory H2 database. It resets all demo
data and reservations on each restart and does not use the normal file-backed database.
Without this profile, sample data is not loaded and normal persistence is unchanged.

## Successful creation

User `1` exists. Screening `1` starts 24 hours after application initialization.
Seats `1`, `2`, `3`, and `4` are in its hall (`Hall A`, row `1`).

```json
{"userId":1,"screeningId":1,"seatIds":[1,2]}
```

Expected: **201 Created**, `{"id":1}` for the first reservation after restart.
Subsequent successful requests return increasing generated IDs. Reservations are
stored as `DRAFT`; repeated draft requests do not hold/block seats.
Restart the application to refresh the sample screening times if necessary.

## Business-rule failure

Screening `2` starts 10 minutes after initialization, so its booking cutoff has
already passed. Use:

```json
{"userId":1,"screeningId":2,"seatIds":[1]}
```

Expected: **409 Conflict**, with problem detail:
`Reservations must be created at least 15 minutes before the screening.`
No reservation is created. This also fails once screening `2` is in the past.
