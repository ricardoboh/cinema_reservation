# Cinema Reservation System — Baseline v0.2

Spring Boot backend, React/Vite frontend, and embedded H2 database. **Use the
`dev` profile for the runnable demo**: it supplies the users, screenings and seats
expected by the frontend. No database server or separate Maven installation is needed.

Current requirements: [Baseline v0.2](docs/c02_baseline_specification_v0.2.md).
See [the manual testing guide](backend/MANUAL-TESTING.md) for fixtures and flows A–F,
and [the frontend guide](frontend/README.md) for UI behavior.
The original C01 description is preserved below as historical documentation;
Baseline v0.2 describes the current lifecycle, including approval and expiration.

## Prerequisites

- JDK 21 or newer (Java 21 is the project's compilation target); `java` must be on PATH.
- Node.js 22.12 or newer, with npm. The locked Vite version also accepts Node 20.19+ in the 20.x line.
- Git; Windows users can use PowerShell or Git Bash.
- Internet access on the first build/install to download Maven and dependencies.

Check with `java -version`, `node --version`, and `git --version`.
For npm use `npm.cmd --version` in PowerShell or `npm --version` in Git Bash.
Set `JAVA_HOME` to your JDK if the Maven Wrapper cannot find Java.

## Clone

```bash
git clone https://github.com/ricardoboh/cinema_reservation.git
cd cinema_reservation
```

Use two terminals: one for the backend and one for the frontend. Start each in
the cloned repository root. Leave both servers running while using the demo.

## 1. Build and start the backend

### Windows PowerShell

```powershell
cd backend
.\mvnw.cmd clean package
java -jar target/cinema-reservation-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### Git Bash (also Linux/macOS)

```bash
cd backend
bash ./mvnw clean package
java -jar target/cinema-reservation-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

`bash ./mvnw` works even when the wrapper has no executable permission.
The wrapper downloads the Maven version pinned in `backend/.mvn/wrapper/`.
`clean package` runs the automated tests and produces the executable JAR;
wait for `BUILD SUCCESS` before running it.

Alternatively, run directly through Maven from `backend` (choose this **instead
of** the JAR command, so that only one backend uses port 8080):

```powershell
# PowerShell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

```bash
# Git Bash
bash ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Wait for the startup log to show the active `dev` profile and the application
started on port `8080` before opening the frontend.

## 2. Install and start the frontend

In the second terminal, from the repository root:

```powershell
# PowerShell
cd frontend
npm.cmd ci
npm.cmd run dev
```

```bash
# Git Bash
cd frontend
npm ci
npm run dev
```

Use `npm ci` to install the versions recorded in `package-lock.json`.
Open **http://127.0.0.1:5173** in your browser. Vite binds to `127.0.0.1:5173`
and proxies `/screenings` and `/reservations` to `http://localhost:8080`.
No CORS configuration or frontend API environment variable is needed for this setup.
Stop each server with Ctrl+C in its terminal. Restarting the dev backend resets demo data.

## H2 profiles and automatic demo data

| Profile | Database | Schema and data behavior |
|---|---|---|
| `dev` (recommended demo) | `jdbc:h2:mem:cinema-dev;DB_CLOSE_DELAY=-1` | Hibernate creates the schema, then Spring runs `dev-data.sql` automatically. Disposable; resets when the backend process restarts. |
| Default (no profile) | `jdbc:h2:file:./data/cinema-reservation` | Hibernate updates the schema; data persists relative to the backend working directory. No demo fixtures are loaded. |

The dev profile uses `create-drop`, deferred datasource initialization and SQL
initialization enabled explicitly. Do not import SQL manually. It seeds users
1–3, screenings 1–3, four Hall A seats (IDs 1–4), and historical reservations
100/101 for screening 3. Screening 1 starts one day after initialization and is
the frontend demo screening. Screening 2 is inside the creation cutoff;
screening 3 has already started. Seats 1/2 confirm directly; seats 3/4 require
approval. See the manual guide for the full fixture policy.

The default profile can start successfully while the frontend fails to load its
expected screening. Restart with `dev` to use the demo. Automated tests have a
separate in-memory `cinema-test` database configured in test resources.

## Local URLs and seat verification

| Purpose | URL |
|---|---|
| React application | http://127.0.0.1:5173 |
| Backend seat availability | http://localhost:8080/screenings/1/availability |
| Same API through Vite | http://127.0.0.1:5173/screenings/1/availability |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

The backend root `/` is not the React application; use port 5173 for the UI.

On a fresh dev start, open the React application and click **Check Availability**.
Expect four seats in row 1, all available, with approval labels on seats 3/4.
In browser developer tools → Network, the request to
`/screenings/1/availability` should return HTTP 200 with four seat records.

You can also compare the direct API and Vite proxy in PowerShell:

```powershell
Invoke-RestMethod http://localhost:8080/screenings/1/availability | ConvertTo-Json -Depth 6
Invoke-RestMethod http://127.0.0.1:5173/screenings/1/availability | ConvertTo-Json -Depth 6
```

Or in Git Bash:

```bash
curl -f http://localhost:8080/screenings/1/availability
curl -f http://127.0.0.1:5173/screenings/1/availability
```

Both responses should contain the same seats. Availability changes as reservations
are confirmed or held; restart the dev backend for a fresh fixture state.

## Run checks

From `backend`, run automated tests without packaging:

```powershell
# PowerShell
.\mvnw.cmd test
```

```bash
# Git Bash
bash ./mvnw test
```

From `frontend`, after `npm ci`, run lint and the TypeScript/production build:

```powershell
# PowerShell
npm.cmd run lint
npm.cmd run build
```

```bash
# Git Bash
npm run lint
npm run build
```

There is no frontend unit-test script. Use the seat check above for the basic
integration smoke test. The [manual guide](backend/MANUAL-TESTING.md) also documents
optional runtime and browser verification scripts and their prerequisites.
`npm run build` creates `frontend/dist`; it does not start the demo servers.

## Troubleshooting

- **No seats / screening not found:** confirm the backend logs show `dev`, then
  check the direct availability URL. Starting without a profile does not seed data.
- **Connection refused / Vite proxy error:** wait for backend startup, check port
  8080, and keep both terminals running. If the direct API works but the proxy
  fails, check the Vite terminal and its configured target in `frontend/vite.config.ts`.
- **Port already in use:** stop the previous server with Ctrl+C. Vite uses
  `strictPort`, so it fails instead of silently choosing another port. Keep 8080
  and 5173 available for the documented configuration.
- **PowerShell says scripts are disabled for npm:** use `npm.cmd` as shown above;
  no execution-policy change is needed.
- **Java missing / invalid target release 21:** install a JDK 21 or newer and
  check PATH and `JAVA_HOME`; reopen the terminal after updating them.
- **Unsupported Node / missing frontend packages:** check the Node version, then
  run `npm ci` in `frontend`. Do not use `npm install` to work around a lockfile error.
- **Wrapper/dependency download fails:** check internet access and any required
  corporate proxy settings. Keep the wrapper files and `.mvn` directory from Git.
- **JAR locked during `clean package` on Windows:** stop the running backend
  before rebuilding, then start the newly built JAR with `dev` again.
- **Reservation returns 409:** a seat may be held/unavailable, the action may be
  stale, or a screening cutoff may have passed. Refresh availability/status;
  use screening 1 on a fresh dev instance for the basic demo.

---

The following C01 material is preserved for historical context. Its original
state list and walking skeleton describe that milestone, not the full current
Baseline v0.2 implementation.

# C01 – Reservation System

## 1. Git + Repository

**Team name:** Deadlock

### Team members

| Name | Login |
|---|---|
| Dominik Kontrik | KON0500 |
| Petr Michalík | MIC0465 |
| Richard Bohunovský | BOH0162 |
| Radek Skalík | SKA0216 |

**Repository:** https://github.com/ricardoboh/cinema_reservation

---

## 2. Reservation Domain

Our project is a cinema reservation system that allows registered users to reserve seats for specific movie screenings.

| Element | Description |
|---|---|
| **Resource** | A seat for a specific movie screening |
| **Reservation** | A uniquely identified reservation of one or more seats for a specific movie screening, including its state |
| **User** | Registered cinema customer |
| **States** | `DRAFT`, `CONFIRMED`, `CANCELLED` |
| **Create** | A registered user creates a reservation for selected seats |
| **Confirm** | A reservation is confirmed |
| **Cancel** | A registered user cancels a reservation |
| **Check availability** | The system checks whether a seat is available for the selected screening |
| **Common rule** | The same seat for the same movie screening cannot be part of two confirmed reservations |
| **Boundary** | Notification Service – sends a notification to the user after a reservation is confirmed or cancelled |

### Domain-specific business rule

A reservation can be created no later than 15 minutes before the start of the movie screening.

---

## 5. Selected Future Pressure

**Category:** Q – Quality / Scale

**Concrete pressure:**  
A large number of users may try to reserve seats for the same popular movie screening at the same time.

**Why it is relevant to our reservation system:**  
Concurrent reservation requests increase the risk that multiple users try to reserve the same seat at the same time. The system must remain responsive and ensure that a seat cannot be confirmed for more than one reservation.

## 9. CP1 Walking Skeleton

A registered user creates a reservation for a seat at a movie screening.

`POST /reservations`

→ validate user, screening and selected seats  
→ apply reservation creation rules  
→ create reservation in `DRAFT` state  
→ persist the reservation  
→ return `HTTP 201 Created` with the reservation ID  
→ automated integration test verifies that the reservation was stored successfully
