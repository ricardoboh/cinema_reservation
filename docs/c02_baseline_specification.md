# C02 — Baseline Specification v0.1: Cinema Reservation System

**Team:** Deadlock  
**Domain:** Cinema Seat Reservation System  
**Baseline Version:** v0.1

---

## 1. Shared Domain Rules & Invariants

Společná doménová pravidla a invarianty jsou definována pouze zde. Jednotlivé operace na ně odkazují.

### BR-01 — Resource Scope & Discrete Slot Semantics

- **Resource** je v našem systému konkrétní dvojice `(Screening, Seat)`, tedy fyzické sedadlo použité pro konkrétní filmové představení.
- Každý `Screening` má konkrétní `startTime`.
- Dostupnost sedadla se vždy vyhodnocuje vzhledem ke konkrétnímu `Screening`.

### BR-02 — Exclusive Resource Invariant (No Double Booking)

V žádném platném stavu systému nesmí pro stejný Resource `(Screening, Seat)` existovat více než jedna rezervace ve stavu `CONFIRMED`.

Formálně pro každou dvojici `(screeningId, seatId)` platí:

`count(CONFIRMED reservations) <= 1`

Rezervace ve stavu `DRAFT` ani `CANCELLED` sedadlo neblokují.

### BR-03 — Domain-Specific Creation Horizon

Rezervaci lze vytvořit nejpozději **15 minut před začátkem představení**.

Rezervace je povolena, pokud:

`currentTime <= screening.startTime - 15 minutes`

To znamená, že vytvoření rezervace **přesně 15 minut před začátkem je povoleno**, ale později již ne.

**Zdůvodnění:** Pravidlo omezuje vytváření nových rezervací těsně před začátkem projekce a zjednodušuje odbavení návštěvníků před promítáním.

### BR-04 — Cancellation Policy

- Rezervaci může zrušit pouze její vlastník.
- Zrušit lze rezervaci ve stavu `DRAFT` nebo `CONFIRMED`.
- Rezervaci lze zrušit pouze před začátkem projekce:

`currentTime < screening.startTime`

- Po začátku projekce rezervaci zrušit nelze.
- Opakovaný pokus o zrušení rezervace ve stavu `CANCELLED` je odmítnut jako `409 Conflict`.
- Zrušení nemá za následek fyzické smazání rezervace.

### BR-05 — DRAFT and Resource Allocation

Stav `DRAFT` představuje záměr uživatele vytvořit rezervaci, ale **nepředstavuje závaznou alokaci sedadla**.

- `DRAFT` rezervace neblokuje sedadlo v `Check Availability`.
- Více uživatelů může mít současně `DRAFT` rezervaci obsahující stejné sedadlo.
- Exkluzivní alokace vzniká až úspěšnou operací `Confirm Reservation`.
- Pokud se dvě kolidující `DRAFT` rezervace pokusí o potvrzení, nejvýše jedna smí přejít do `CONFIRMED`.

### BR-06 — Confirm / Cancel Concurrency

Pokud jsou `Confirm Reservation` a `Cancel Reservation` spuštěny souběžně nad stejnou `DRAFT` rezervací, systém musí změny zpracovat tak, aby vznikl právě jeden platný výsledný stav.

Přípustné výsledky:

- pokud je jako první úspěšně provedeno `Confirm`, rezervace přejde do `CONFIRMED`; následný `Cancel` ji může zrušit, pokud stále splňuje BR-04, takže výsledný stav může být `CANCELLED`;
- pokud je jako první úspěšně provedeno `Cancel`, rezervace přejde do `CANCELLED` a souběžný nebo následný `Confirm` musí být odmítnut.

Rezervace nesmí skončit v nekonzistentním nebo částečně aktualizovaném stavu.

---

## 2. Specification of Baseline Operations

### OP-01 — Create Reservation

**Cíl / hodnota pro uživatele:**  
Registrovaný zákazník vytvoří návrh rezervace (`DRAFT`) pro jedno nebo více sedadel na konkrétní filmové představení. Vytvoření návrhu ještě sedadla závazně nealokuje.

**Spouštěcí událost:**  
Registrovaný uživatel odešle požadavek `POST /reservations` se zvolenou projekcí a sedadly.

**Pozorovatelný požadavek / požadavky:**

**REQ-01:** Systém vytvoří jednu novou rezervaci ve stavu `DRAFT`, pokud existuje uživatel, projekce a vybraná sedadla, sedadla patří do sálu dané projekce a je splněno BR-03.

Vytvoření `DRAFT` **nekontroluje kolizi s jinými rezervacemi**, protože `DRAFT` podle BR-05 nepředstavuje závaznou alokaci Resource. Dostupnost se uživateli zobrazuje pomocí `Check Availability` a závazná kontrola kolize probíhá při `Confirm Reservation`.

**Předpoklady:**

- uživatel existuje;
- projekce existuje;
- seznam `seatIds` obsahuje alespoň jedno sedadlo;
- všechna zadaná sedadla existují a patří do sálu dané projekce;
- `currentTime <= screening.startTime - 15 minutes`.

**Stav po úspěšném provedení:**

- existuje právě jedna nová rezervace s unikátním `reservationId`;
- `Reservation.state = DRAFT`;
- rezervace obsahuje zvoleného uživatele, projekci a sedadla;
- sedadla nejsou touto rezervací závazně blokována.

**Změna stavu:**  
`[none] -> DRAFT`

**Odkaz na doménová pravidla / invarianty:**  
BR-01, BR-03, BR-05.

#### Hlavní úspěšný scénář

1. Uživatel odešle `userId`, `screeningId` a neprázdný seznam `seatIds`.
2. Systém ověří existenci uživatele a projekce.
3. Systém ověří existenci sedadel a jejich příslušnost k sálu projekce.
4. Systém ověří časové pravidlo BR-03.
5. Systém vytvoří rezervaci ve stavu `DRAFT`.
6. Systém rezervaci uloží.
7. Systém vrátí `HTTP 201 Created` a identifikátor vytvořené rezervace.

#### Alternativní / chybové výsledky

- neexistující uživatel → `404 Not Found`; rezervace nevznikne;
- neexistující projekce → `404 Not Found`; rezervace nevznikne;
- neexistující sedadlo → `404 Not Found`; rezervace nevznikne;
- sedadlo nepatří do sálu projekce → `400 Bad Request`; rezervace nevznikne;
- prázdný nebo jinak neplatný seznam sedadel → `400 Bad Request`; rezervace nevznikne;
- projekce začíná za méně než 15 minut → `409 Conflict`; rezervace nevznikne.

#### Příklady ověření

- projekce začíná za 2 hodiny + platná sedadla A1 a A2 → `201 Created`, vznikne jedna rezervace `DRAFT`;
- projekce začíná přesně za 15 minut → `201 Created`, vznikne `DRAFT`;
- projekce začíná za 10 minut → `409 Conflict`, rezervace nevznikne;
- neexistující sedadlo → `404 Not Found`, rezervace nevznikne;
- již `CONFIRMED` sedadlo + jinak platný požadavek → `DRAFT` může vzniknout, ale jeho pozdější `Confirm` musí při trvající kolizi selhat.

**Zdůvodnění / zdroj:**  
`DRAFT` zachycuje záměr zákazníka bez okamžité závazné alokace Resource. Oddělení Create a Confirm umožňuje před závazným potvrzením provést další kroky procesu.

**Předpoklad / neznámá / TBD:**  
Není zatím rozhodnuto, zda bude v pozdější verzi zavedena automatická expirace (`TTL`) opuštěných `DRAFT` rezervací. Toto rozhodnutí nemění baseline pravidlo, že `DRAFT` nyní sedadlo neblokuje.

---

### OP-02 — Check Availability

**Cíl / hodnota pro uživatele:**  
Uživatel zjistí, která sedadla jsou pro konkrétní filmové představení dostupná.

**Spouštěcí událost:**  
Uživatel zobrazí dostupnost sedadel pro konkrétní `screeningId`.

**Pozorovatelný požadavek / požadavky:**

**REQ-02:** Pro existující projekci systém označí sedadlo jako `UNAVAILABLE`, pokud je stejné sedadlo pro stejnou projekci součástí rezervace ve stavu `CONFIRMED`. Jinak jej označí jako `AVAILABLE`.

**Předpoklady:**

- projekce existuje.

**Stav po úspěšném provedení:**

- systém vrátí dostupnost sedadel;
- žádná rezervace ani jiný doménový stav se nezmění.

**Změna stavu:**  
Žádná.

**Odkaz na doménová pravidla / invarianty:**  
BR-01, BR-02, BR-05.

#### Hlavní úspěšný scénář

1. Klient požádá o dostupnost sedadel pro `screeningId`.
2. Systém ověří existenci projekce.
3. Systém získá sedadla příslušného sálu.
4. Pro každé sedadlo zjistí, zda pro dvojici `(Screening, Seat)` existuje `CONFIRMED` rezervace.
5. Systém vrátí sedadla označená jako `AVAILABLE` nebo `UNAVAILABLE` a `HTTP 200 OK`.

#### Alternativní / chybové výsledky

- neexistující projekce → `404 Not Found`.

#### Příklady ověření

- projekce bez potvrzených rezervací → všechna sedadla jsou `AVAILABLE`;
- A1 je součástí `CONFIRMED` rezervace → A1 je `UNAVAILABLE`;
- A2 je pouze v `DRAFT` rezervaci → A2 je `AVAILABLE`;
- A3 je pouze v `CANCELLED` rezervaci → A3 je `AVAILABLE`.

**Zdůvodnění / zdroj:**  
Operace umožňuje zákazníkovi zjistit aktuální dostupnost před vytvořením nebo potvrzením rezervace. Stav dostupnosti vychází ze závazných alokací definovaných BR-02 a BR-05.

---

### OP-03 — Confirm Reservation

**Cíl / hodnota pro uživatele:**  
Platná `DRAFT` rezervace se stane závaznou rezervací a vybraná sedadla jsou garantována zákazníkovi.

**Spouštěcí událost:**  
Vlastník rezervace odešle `POST /reservations/{id}/confirm`.

**Pozorovatelný požadavek / požadavky:**

**REQ-03:** Systém potvrdí rezervaci ve stavu `DRAFT` pouze tehdy, pokud žádné její sedadlo není pro stejnou projekci součástí jiné rezervace ve stavu `CONFIRMED`.

**REQ-04:** Při souběžných pokusech o potvrzení kolidujících rezervací smí pro stejný Resource `(Screening, Seat)` přejít do `CONFIRMED` nejvýše jedna rezervace. Ostatní kolidující potvrzení musí být odmítnuta jako `409 Conflict`.

**Předpoklady:**

- rezervace existuje;
- volající uživatel je vlastníkem rezervace;
- `Reservation.state = DRAFT`;
- projekce ještě nezačala: `currentTime < screening.startTime`.

**Stav po úspěšném provedení:**

- `Reservation.state = CONFIRMED`;
- sedadla rezervace jsou pre danou projekci závazně alokována;
- BR-02 zůstává splněno;
- je iniciováno odeslání potvrzovací notifikace přes `Notification Service`.

**Změna stavu:**  
`DRAFT -> CONFIRMED`

**Odkaz na doménová pravidla / invarianty:**  
BR-01, BR-02, BR-05, BR-06.

#### Hlavní úspěšný scénář

1. Uživatel odošle požadavek na potvrzení `reservationId`.
2. Systém ověří existenci rezervace a její vlastnictví.
3. Systém ověří, že rezervace je ve stavu `DRAFT` a projekce ještě nezačala.
4. Systém zkontroluje všechny Resource `(Screening, Seat)` rezervace proti BR-02.
5. Pokud neexistuje kolize, systém atomicky změní stav na `CONFIRMED`.
6. Systém iniciuje potvrzovací notifikaci.
7. Systém vrátí `HTTP 200 OK` s potvrzenou rezervací.

#### Alternativní / chybové výsledky

- rezervace neexistuje → `404 Not Found`;
- volající není vlastníkem rezervace → `403 Forbidden`;
- rezervace není ve stavu `DRAFT` → `409 Conflict`; stav se nezmění;
- projekce již začala → `409 Conflict`; rezervace zůstává `DRAFT`;
- alespoň jedno sedadlo je již potvrzeno jinou rezervací → `409 Conflict`; rezervace zůstává `DRAFT`;
- souběžné kolidující potvrzení → nejvýše jedno uspěje, ostatní skončí `409 Conflict`.

#### Příklady ověření

- `DRAFT` + volná sedadla + projekce nezačala → `200 OK`, stav `CONFIRMED`;
- `CANCELLED` → Confirm je odmítnut `409 Conflict`;
- R1 a R2 jsou `DRAFT` pro stejné sedadlo; R1 se úspěšně potvrdí → následný Confirm R2 skončí `409 Conflict`, R2 zůstává `DRAFT`;
- dva souběžné Confirm requesty pre kolidující rezervace → nejvýše jedna rezervace skončí `CONFIRMED`.

**Zdůvodnění / zdroj:**  
Confirm je okamžik, kdy vzniká závazná alokace Resource. Kontrola kolize právě v tomto kroku chrání invariant BR-02 i v případě více existujících `DRAFT` rezervací.

---

### OP-04 — Cancel Reservation

**Cíl / hodnota pro uživatele:**  
Zákazník může před začátkem projekce zrušit vlastní rezervaci, kterou již nechce využít. Závazně alokovaná sedadla se tím znovu zpřístupní.

**Spouštěcí událost:**  
Vlastník rezervace odešle `POST /reservations/{id}/cancel`.

**Pozorovatelný požadavek / požadavky:**

**REQ-05:** Systém umožní vlastníkovi zrušit rezervaci ve stavu `DRAFT` nebo `CONFIRMED`, pokud projekce ještě nezačala.

**REQ-06:** Úspěšně zrušená rezervace přejde do `CANCELLED`, zůstane uložena a přestane blokovat dostupnost Resource.

**REQ-07 (Souběh Confirm/Cancel):** Pokud jsou nad stejnou `DRAFT` rezervací souběžně spuštěny Confirm a Cancel, systém musí zachovat platný stavový přechod podle BR-06 a nesmí ponechat rezervaci v nekonzistentním stavu.

**Předpoklady:**

- rezervace existuje;
- volající uživatel je vlastníkem rezervace;
- `Reservation.state ∈ {DRAFT, CONFIRMED}`;
- `currentTime < screening.startTime`.

**Stav po úspěšném provedení:**

- `Reservation.state = CANCELLED`;
- záznam rezervace zůstává uložen;
- rezervace již neblokuje sedadla v `Check Availability`;
- je iniciována notifikace o zrušení přes `Notification Service`.

**Změna stavu:**  

`DRAFT -> CANCELLED`

nebo

`CONFIRMED -> CANCELLED`

**Odkaz na doménová pravidla / invarianty:**  
BR-02, BR-04, BR-05, BR-06.

#### Hlavní úspěšný scénář

1. Uživatel odešle požadavek na zrušení `reservationId`.
2. Systém ověří existenci rezervace a její vlastnictví.
3. Systém ověří, že rezervace je ve stavu `DRAFT` nebo `CONFIRMED`.
4. Systém ověří, že projekce ještě nezačala.
5. Systém nastaví stav rezervace na `CANCELLED` a uloží čas zrušení.
6. Systém iniciuje notifikaci o zrušení.
7. Systém vrátí `HTTP 200 OK` s rezervací ve stavu `CANCELLED`.

#### Alternativní / chybové výsledky

- rezervace neexistuje → `404 Not Found`;
- volající není vlastníkem rezervace → `403 Forbidden`;
- rezervace je již `CANCELLED` → `409 Conflict`;
- projekce již začala (`currentTime >= screening.startTime`) → `409 Conflict`; stav se nezmění;
- souběžný Confirm/Cancel → výsledok musí odpovídat BR-06; nesmí vzniknout nekonzistentní stav.

#### Příklady ověření

- `DRAFT` před začátkem → `200 OK`, stav `CANCELLED`;
- `CONFIRMED` před začátkem → `200 OK`, stav `CANCELLED` a sedadla jsou následně `AVAILABLE`;
- `CONFIRMED` přesně v čase začátku projekce → `409 Conflict`, rezervace zůstává `CONFIRMED`;
- již `CANCELLED` rezervace → `409 Conflict`;
- cizí uživatel → `403 Forbidden`;
- souběžný Confirm a Cancel nad stejným `DRAFT` → systém skončí pouze v platném výsledném stavu podle BR-06.

**Zdůvodnění / zdroj:**  
Storno umožňuje zákazníkovi vzdát se rezervace před projekcí a znovu zpřístupnit závazně alokovaná sedadla. Uchování záznamu ve stavu `CANCELLED` zachovává historii rezervace.

---

## 3. Reservation State Model

```text
[none]
   |
   | Create Reservation
   v
 DRAFT
  |  \
  |   \ Cancel
  |    \
  |     v
  |  CANCELLED
  |
  | Confirm
  v
CONFIRMED
  |
  | Cancel
  v
CANCELLED
```

`CANCELLED` je konečný stav. Operace nad neplatným zdrojovým stavem jsou odmítnuty a stav rezervace se nezmění.

---

## 4. Mapování na REST API

| Operace | HTTP metoda | Endpoint | Úspěšný kód |
|---|---|---|---|
| Check Availability | `GET` | `/screenings/{screeningId}/availability` | `200 OK` |
| Create Reservation | `POST` | `/reservations` | `201 Created` |
| Confirm Reservation | `POST` | `/reservations/{id}/confirm` | `200 OK` |
| Cancel Reservation | `POST` | `/reservations/{id}/cancel` | `200 OK` |

---

## 5. Kontrola přijetí požadavků

| Kritérium | Jak je vyřešeno v baseline |
|---|---|
| **Význam** | Resource je jednoznačně definován jako `(Screening, Seat)` a význam stavů `DRAFT`, `CONFIRMED`, `CANCELLED` je explicitní. |
| **Potřeba / zdůvodnění** | Každá operace obsahuje zdůvodnění své hodnoty a role v rezervačním procesu. |
| **Pozorovatelný výsledek** | Požadavky definují pozorovatelné stavy, dostupnost a výsledky operací, nikoli konkrétní databázovou nebo frameworkovou implementaci. |
| **Proveditelnost** | `DRAFT` nealokuje Resource, zatímco `CONFIRMED` jej alokuje; jednotlivé požadavky proto mohou platit současně bez vzájemného rozporu. |
| **Ověřitelnost** | Každá operace obsahuje konkrétní příklady vstupu a očekávaného výsledku. |
| **Stav / čas** | Jsou explicitně definovány povolené stavové přechody i hranice `<= startTime - 15 min` a `< startTime`. |
| **Souběh** | REQ-04 řeší kolidující Confirm operace a REQ-07 řeší souběh Confirm/Cancel nad stejnou rezervací. |
| **Konzistence** | Všechny operace odkazují na jednu společnou sadu BR-01 až BR-06 a používají stejný state model. |
| **Nejistota** | Jediným aktuálním TBD je případné budoucí TTL pro opuštěné `DRAFT` rezervace; baseline na něm nezávisí. |

---

## 6. Přijatá baseline rozhodnutí

Pro odstranění nejednoznačností platí pro baseline v0.1 následující rozhodnutí:

1. `DRAFT` neblokuje sedadlo a Create proto neprovádí závaznou kontrolu kolize.
2. Dostupnost blokují pouze rezervace ve stavu `CONFIRMED`.
3. Přesně 15 minut před začátkem projekce lze rezervaci ještě vytvořit.
4. Závazná kontrola dostupnosti probíhá při Confirm.
5. Dvě kolidující rezervace nesmí být současně `CONFIRMED`.
6. Cancel je povolen pouze před `screening.startTime`.
7. Opakovaný Cancel je odmítnut jako `409 Conflict`.
8. Cancel mění stav na `CANCELLED`; rezervaci fyzicky nemaže.
9. `Notification Service` je systémová hranice vyvolaná po úspěšném Confirm a Cancel.
10. Automatická expirace `DRAFT` rezervací zůstává TBD pro pozdější rozhodnutí.
