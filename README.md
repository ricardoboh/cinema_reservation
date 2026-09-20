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
