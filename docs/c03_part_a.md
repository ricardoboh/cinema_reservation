## C03 — Část A: Zmapování současné implementace (AS-IS)

### A1. Sledovaný scénář

| Položka | Hodnota |
| :--- | :--- |
| **Scénář / operace** | `Confirm Reservation` (OP-03)[cite: 16, 18] |
| **Požadavky** | `REQ-03` (kontrola kolize), `REQ-04` (ochrana při souběhu) |
| **Pravidla / invarianty** | `BR-02` (Exclusive Resource invariant - žádný překryv) |
| **Baseline** | `v0.2`[cite: 16, 18] |

### A2. Mapování hlavního průchodu scénáře na kód

| Krok scénáře z C02 | Realizace v kódu | Doklad |
| :--- | :--- | :--- |
| přijmout požadavek na potvrzení | `ReservationController.confirmReservation()` | `ReservationController.java` |
| načíst `Reservation` | `ReservationRepository.findById()` | `ReservationService.java` |
| ověřit, že přechod je povolený | Podmínka `reservation.getState() == DRAFT` a check času | `ReservationService.java`[cite: 18] |
| vyhodnotit konflikt (`BR-02`) | Dotaz `reservationRepository.findConflicting(...)` | `ReservationService.java`[cite: 18] |
| změnit stav | Volání metody `reservation.confirm()` | `Reservation.java` (Domain entity)[cite: 18] |
| uložit výsledek | `ReservationRepository.save()` | `ReservationService.java`[cite: 18] |
| odeslat notifikaci | Synchronní volání `NotificationClient.send(...)` | `ReservationService.java`[cite: 18] |

### A3. Mapování jedné důležité alternativní nebo chybové větve

Zvolená větev: **conflict exists → confirmation is rejected**[cite: 18]

| Co říká v0.2 | Kde se podmínka zjistí | Kde se rozhodne výsledek | Co dostane volající |
| :--- | :--- | :--- | :--- |
| konflikt → odmítnout | V `ReservationService` (ověřením výsledku z DB dotazu na překryv) | V `ReservationService` se vyhodí `ConflictException` | Odpověď `HTTP 409 Conflict`[cite: 18] |

### A4. Hlavní části implementace

| Část implementace | Typ / obsah | Role v tomto scénáři | Doklad |
| :--- | :--- | :--- | :--- |
| **Reservation API** | `ReservationController` | Přijme potvrzovací command od klienta | Třída kontroleru[cite: 18] |
| **Reservation Logic** | `ReservationService`, `Reservation` | Načítá, ověřuje pravidla a rozhoduje o potvrzení | Zdrojové kódy servisy a entity[cite: 18] |
| **Persistence** | `ReservationRepository` | Komunikuje s databází (načtení/uložení stavu) | Spring Data rozhraní[cite: 18] |
| **Notification Integration** | `NotificationClient` | Odesílá zprávu o úspěchu externí službě | REST klient API[cite: 18] |

### A5. Určení stavu, změny stavu a pravidla

**Stav**
| Otázka | Odpověď | Doklad |
| :--- | :--- | :--- |
| Kde je stav `Reservation` trvale uložen? | V relační databázi (např. PostgreSQL) přes JPA repozitář. | `application.yml`, `ReservationRepository.java`[cite: 18] |
| Který kód rozhoduje/provádí přechod použitý ve scénáři? | Rozhoduje `ReservationService`, fyzicky provádí entita `Reservation` metodou `confirm()`. | `ReservationService.java`, `Reservation.java`[cite: 18] |

**Business pravidlo / invariant (`BR-02` - žádný překryv potvrzených rezervací)**
| Otázka | Odpověď | Doklad |
| :--- | :--- | :--- |
| Kde se zjistí podmínka pravidla? | V aplikační vrstvě přes read dotaz, který hledá kolizní sedadla. | `ReservationRepository.findConflicting()`[cite: 18] |
| Kde se podle výsledku rozhodne? | V `ReservationService.confirm()`. | Logika `if/else` před změnou stavu[cite: 18] |
| Kde se provede výsledná změna stavu? | Na instanci `Reservation` v paměti a následně persistencí přes `Repository.save()`. | `ReservationService.java`[cite: 18] |

### A6. Zapište relevantní závislosti

| Závislost | Kde se napojuje na váš kód | Která část zná její technické API | Doklad |
| :--- | :--- | :--- | :--- |
| **Databáze** | Přes ORM / Spring Data JPA framework | `ReservationRepository` / konfigurace | Konfigurace připojení k DB[cite: 18] |
| **Notification Service** | Synchronní HTTP/REST volání | `NotificationClient` (nyní volaný přímo ze Service) | Zdrojový kód klienta[cite: 18] |

### A7. AS-IS strukturální diagram

### A7. AS-IS strukturální diagram

```mermaid
flowchart TD
    subgraph AppCode [Application code]
        API[Reservation API<br>role: accepts confirm command]
        Logic[Reservation logic<br>role: decides confirmation]
        Persist[Persistence<br>role: loads/persists state]
        Notif[Notification integration<br>role: sends notifications]
        
        API -->|"confirm(id)"| Logic
        Logic -->|"load/save Reservation"| Persist
        Logic -->|"send notification"| Notif
    end
    
    DB[(Reservation DB)]
    ExtNotif[Notification Service]
    
    Persist -->|"read/write"| DB
    Notif -->|"HTTP/API"| ExtNotif

### A8. Formulujte jednu otázku pro další C03

| Položka | Obsah |
| :--- | :--- |
| **Otázka** | Kde a jak se musí provést autoritativní rozhodnutí o alokaci a potvrzení, aby pravidlo `BR-02` zůstalo zachováno i při souběžných požadavcích? |
| **Doklad** | `findConflicts()` (čtení) a `save()` (zápis) jsou v AS-IS implementaci v `ReservationService` oddělené operace (tzv. time-of-check to time-of-use vulnerability) bez explicitního zámku. |
| **Proč je důležitá** | Dle požadavku `REQ-04` a driveru na souběh nesmí systém při paralelním pokusu potvrdit stejné sedadlo dvakrát. Stávající AS-IS logika v aplikační vrstvě tento souběh nechrání. |
    
    Persist -->|read/write| DB
    Notif -->|HTTP/API| ExtNotif
