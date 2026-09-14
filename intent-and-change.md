# Project Frame

## Reservation domain

The system provides reservations of seats for specific movie screenings in a cinema.

A registered user selects a movie screening and reserves one or more available seats in the cinema hall.

## Purpose

The system allows registered cinema customers to reserve seats for a selected movie screening.
Its purpose is to manage seat availability, prevent conflicting reservations and provide customers with information about their reservations.

## Users / Stakeholders

* **Registered User** – creates, confirms and cancels their reservations.
* **Cinema Administrator** – manages movies, screenings, cinema halls and seats.
* **Cinema Operator** – provides the cinema service and uses reservation information.

## Core concepts

* **Reservation** – represents a reservation created by a registered user for one or more seats at a specific screening.
* **Resource** – a seat in a cinema hall reserved for a specific movie screening. It is identified by the hall, row and seat number in combination with the screening.
* **User** – a registered cinema customer who creates and manages reservations.
* **Screening** – a scheduled movie screening with a specific movie, cinema hall and start time.
* **Seat** – a physical seat identified by its hall, row and seat number.

## Core operations

* Create reservation
* Confirm reservation
* Cancel reservation
* Check seat availability

## Persistent state

### Reservation

The system stores:

* reservation ID
* registered user ID
* screening ID
* selected seat(s)
* reservation status
* creation timestamp

### Resource

For the reserved resource, the system stores:

* cinema hall
* row
* seat number
* screening
* screening start time

### Screening

The system stores:

* screening ID
* movie
* cinema hall
* start time

## State-changing operation

A newly created reservation starts in the `DRAFT` state.

A user can confirm it:

`DRAFT → CONFIRMED`

A reservation can also be cancelled:

`DRAFT → CANCELLED`
`CONFIRMED → CANCELLED`

## Common business rule

Two confirmed reservations cannot contain the same seat for the same movie screening.

## Domain-specific business rule

A reservation cannot be created less than 15 minutes before the start of the movie screening.

## External / system boundary

**Notification Service**

The reservation system uses an external notification service to notify the registered user when a reservation is confirmed or cancelled.

## Assumption

We assume that each movie screening has a predefined cinema hall and seating layout and that these do not change after reservations for the screening become available.

## Unknown

We currently do not know how long an unconfirmed (`DRAFT`) reservation should hold selected seats before it automatically expires.
