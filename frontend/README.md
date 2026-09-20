# CP1 reservation demo

Run both processes in separate terminals from the repository root.

Backend (Java 21 required):

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Frontend:

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

Open http://127.0.0.1:5173. Select A1-A4, toggle a selected seat to deselect it,
and click **Reserve**. The button is disabled without seats and during submission.
A successful HTTP 201 displays the backend's reservation ID and clears selection.
Errors preserve selection and show the API's Problem Detail message when available.
Requests time out after 15 seconds; a timeout does not prove creation failed.

`App.tsx` owns selection and submission state and renders the screening, seat
buttons, summary, and feedback. `reservations.ts` sends JSON via native `fetch`:

```json
{ "userId": 1, "screeningId": 1, "seatIds": [1, 2] }
```

Vite proxies `/reservations` to `http://localhost:8080/reservations`, so browser
requests use the frontend origin and no backend CORS change is needed. This is a
development-server proxy; a deployed build needs an equivalent same-origin proxy.

Temporary data: user 1, screening 1 (`CP1 Demo Movie`, Hall A), and A1-A4 mapped to
seat IDs 1-4. No screening or seat lookup endpoint is used. The dev backend seeds
the screening one day after startup and resets its in-memory database on restart.
Restart it if the screening has passed the reservation cutoff. Creation produces
a draft; it does not hold or confirm seats. There is no availability lookup.

Validation:

```powershell
npm.cmd run build
npm.cmd run lint
```

Manual checks: initial Reserve disabled; seat selection/deselection updates the
summary; submit selected seats and see a real ID; stop the backend and submit again
to see an error. Browser network tools show the POST request and response.
