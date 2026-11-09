# Cinema frontend

The current scope is [the documentation index](../docs/README.md), not the removed Phase 1 extras. Routes: `/`, `/movies`, `/movies/:id`, `/book/:screeningId`, `/reservations`, `/approver`, `/admin`, `/admin/movies`, `/admin/halls`, `/admin/screenings`.

`npm ci`, `npm run dev`; checks `npm run lint` and `npm run build`. Backend port is 8080. The UI uses the complete reservation response from Create and the exact current JSON keys; there is no selectable theme, search, account preview, stored reservation history or dashboard counter.

The reservation-ID observation form is the UI for the specified owner status/approver operation. Pending status is observed from the backend; the UI does not invent terminal outcomes or locally expire a hold. The last created ID is carried only in app memory to support navigation.

Browser verification: [backend manual](../backend/MANUAL-TESTING.md). The original obsolete redesign verification script has been removed.
