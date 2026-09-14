# cinema_reservation
Reservation system for cinema


## CP1 walking skeleton

POST /reservations

→ validate reservation request
→ check seat availability for the selected screening
→ create reservation in DRAFT state
→ persist reservation
→ return HTTP 201 Created with reservation ID
→ automated test verifies the reservation was persisted
