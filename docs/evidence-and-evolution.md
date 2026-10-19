\# C01 Engineering Spike



\## Question / unknown



Can a new team member build, test and run the application from a clean checkout using only the instructions provided in the repository?



\## What we did



We created the initial Spring Boot project skeleton and documented the required environment and commands in README.



A second team member performed a clean checkout of the repository and followed only the README instructions to build the project, execute the automated tests and start the application.



\## Observed result



The project was successfully built using the Maven Wrapper, all automated tests passed and the Spring Boot application started successfully.





\## Decision / what changes because of the result



We will use Java 21, Spring Boot and Maven Wrapper for the project. Build, test and run instructions will be maintained in README so that the project remains reproducible for all team members.


## C02 Task 14: runnable v0.2

Implemented conditional confirmation, finite exclusive pending holds, independent
Approver decisions, terminal rejection/expiration and pending cancellation using
the existing application and screening lock. Both baseline specifications remain
unchanged. The React/Vite demo now exposes the full delayed lifecycle in a wider,
responsive light layout.

Verified on 2026-10-04: 60 backend tests passed, clean package passed, frontend
lint/build passed, Swagger/OpenAPI loaded, actual API and headless Edge flows A–F
passed including the real 60-second deadline, 403 authority failure and 409 pending
conflict. Browser reported no runtime exceptions; 390px viewport had no horizontal
overflow. No commit or push.

See [Task 14 report](c02-task14-report.md) for file inventory and concrete evidence,
[architecture decisions](architecture-and-decisions.md) for C03 drivers, and
[manual testing](../backend/MANUAL-TESTING.md) for reproduction commands.

## Evidence C02: specifikace → běžící aplikace

- **Přijatá baseline:** [Baseline v0.1](c02_baseline_specification.md) zachycuje původní čtyři operace a stavy `DRAFT`, `CONFIRMED`, `CANCELLED`. Aktuálně přijatá [Baseline v0.2](c02_baseline_specification_v0.2.md) rozšiřuje tento základ o podmíněné schvalování; běžící implementace a [report Task 14](c02-task14-report.md) odpovídají v0.2. Obě specifikace zůstávají samostatně zachované.

- **Předvedené základní operace:** Create ukládá neblokující `DRAFT`; Check Availability zobrazuje obsazení pro `(Screening, Seat)`; Confirm běžná sedadla přímo potvrdí, schvalovaná převede do `PENDING_APPROVAL`; Cancel vlastníka uvolní rezervaci. React/Vite demo i API navíc předvedly samostatné rozhodnutí Approvera `APPROVE`/`REJECT`, čtení stavu a automatickou expiraci. Smíšená rezervace drží všechna sedadla atomicky; schválení převádí hold na `CONFIRMED` bez uvolnění.

- **Skutečně provedené příklady ověření:** Task 14 dne 2026-10-04 zaznamenal `mvnw.cmd test` a `mvnw.cmd clean package`: **60 testů, 0 selhání/chyb/přeskočení**, úspěšný build; lokální Surefire XML potvrzují 59 integračních testů + 1 test kontextu. Frontend `npm.cmd run lint` a `npm.cmd run build` prošly. Zabalený backend s dev profilem a Vite: Swagger HTTP 200, OpenAPI 3.1.0, skript [verify-baseline.ps1](../backend/verify-baseline.ps1) provedl **43 reálných požadavků/kontrol**, scénáře A–F prošly: přímé potvrzení/zrušení, smíšený hold/schválení, zamítnutí, pending zrušení, skutečná 60sekundová expirace a kolize (`409`, konkurent zůstává `DRAFT`). Rozhodnutí vlastníka bez pravomoci vrátila `403`; pozdní rozhodnutí `409`. Headless Edge předvedl stejné toky bez mocků, bez runtime výjimek a bez vodorovného přetečení při 390 px. Testy pokrývají i hranice 15 minut a 59/60/61 sekund, souběhy a zastaralá rozhodnutí. Reprodukce: [manual testing](../backend/MANUAL-TESTING.md); lokální artefakty: `backend/target/c02-v02-runtime-verification.json`, `c02-v02-ui-verification.json` a screenshoty (ignorované gitem). Jde o doložené výsledky Task 14, nikoli nový běh při této dokumentační úpravě.

- **Nalezený nesoulad a způsob vyřešení:** Původní implementace kontrolovala kolizi s potvrzenými sedadly již při Create, přestože `DRAFT` neblokuje Resource. Kontrola byla z Create odstraněna; alokační konflikty se vynucují při Confirm, ve v0.2 proti `CONFIRMED` i živému pending holdu. Regresní test `createsDraftEvenWhenOneSeatIsConfirmed` dokládá povolené vytvoření draftu; kolidující Confirm je odmítnut bez částečné alokace.

- **Shrnutí dopadu změny:** [Task 12 – impact analysis](c02-change-impact-analysis.md) navrhl rozšíření; v0.2 přijala a Task 14 implementoval approval-required Resources, `PENDING_APPROVAL`, nezávislou pravomoc Approvera, terminální `REJECTED`/`EXPIRED` a pending cancellation. Změna zasáhla stavový model, Confirm, dostupnost, Cancel, nové rozhodování, historii/deadline, notifikace po commitu, API/UI, fixtures a ověření. Create nadále používá původní validace a inkluzivní 15minutovou hranici. Použit byl existující screening lock; demo politika sedadel 3/4, Approver 3 a timeout 60 sekund jsou implementační volby.

- **Zbývající předpoklad / neznámá:** Skutečná TBDs v0.2: produkční kritéria/configurace approval-required Resources a přiřazení/rozsah pravomoci Approvera; přesný konečný timeout/výpočet deadline v mezích BR-08; produkční status read API a approval/status payload schémata; budoucí TTL opuštěných draftů. Demo tyto volby předvádí, ale neuzavírá je jako produkční pravidla. Pending blokování, jeho zrušitelnost, terminální výsledky a zákaz schválení na deadline jsou již rozhodnuté.

- **Architektonické drivery přenesené do C03:** Perzistentní pending požadavek se stabilní politikou, pravomocí a historií; oddělení autority od vlastnictví a důvěryhodná identita; zpožděné zpracování a pozorovatelný stav; autoritativní čas a spolehlivá expirace; atomická vícesedadlová alokace/konverze holdu; souběhy rozhodnutí, zrušení, expirace a nové alokace, ochrana před zastaralými požadavky; reprezentace API/auditu a notifikace navázané na commit. Produkční persistence, scheduling/koordinace času, transakční technika a škálování, obnova při porušení invariantu a doručování/retry notifikací zůstávají pro C03. Současný hrubý screening lock a lokální sweep nejsou jejich konečným řešením; viz [architektonická rozhodnutí](architecture-and-decisions.md).

- **Commit / tag aplikace:** Aktuální implementace C02 v0.2: `4d243811d115d0b8caad14dea9f3145db2561992` (`Implement C02 baseline v0.2`, HEAD); na tomto commitu není tag. Tato závěrečná evidence zatím není commitnuta; hash jejího případného finálního commitu se doplní až po jeho vytvoření. V rámci tohoto úkolu nebyl proveden commit ani push.

## C05 DD Verification Evidence

### Scope and source

Výchozí commit: `591c558` (`Revise C03 documentation with updates and findings`, načtený main). Zadání: přiložené `SWI_C05_Detailed_Design_students_toggle_v4 (1).html`, česká část, všech šest kroků a Definition of Done. Návrh: [C05 Detailed Design](architecture-and-decisions.md#c05-detailed-design). Jednotka: pending expiration uvnitř C03 Reservation Management; navazuje na BR-08/REQ-11. Starší C02 výsledky výše jsou historické a nejsou náhradou tohoto běhu.

### Tests derived from DD

Prefix testů: `backend/src/test/java/com/reservedbytes/cinema_reservation/`.

| DD element / decision | What must hold? | Test level | Test case |
|---|---|---|---|
| D2, timeout větev | Při pozdější projekci deadline = request + timeout | unit | `PendingExpirationPolicyTests.usesTimeoutWhenScreeningIsLater` |
| D2, capped větev | Projekce dříve/stejně jako timeout → deadline = start | unit | `capsDeadlineAtScreeningIncludingEquality` |
| D2, bezpečnost výpočtu | Velký platný timeout nezpůsobí overflow Instant | unit | `handlesHugeTimeoutWithoutInstantOverflow` |
| D1, konstrukční kontrakt | null, nula a záporný timeout odmítnuty | unit | `rejectsNonPositiveOrMissingConfiguration` |
| D2, precondition | Request v/po začátku není platný | unit | `rejectsRequestAtOrAfterStart` |
| D3, inkluzivní hranice | Deadline − 1 ns false, deadline a +1 ns true | unit | `expiresExactlyAtDeadlineWithNanosecondPrecision` |
| D3, stavový guard | Všechny ne-pending stavy zůstávají beze změny | unit | `neverExpiresNonPendingStates` |
| Kontrakt vstupů | Null povinná data selžou explicitně | unit | `failsFastOnMissingRequiredInputs` |
| D5, D6, přechod a opakování | EXPIRED má efektivní deadline; opakování nepublikuje | unit, spolupráce s mock publisherem | `ExpirationServiceTests.dueTransitionKeepsEffectiveDeadlineAndPublishesOnce` |
| D3, živý pending | Žádná mutace ani událost před deadline | unit | `livePendingDoesNotMutateOrNotify` |
| D4, lock ordering | Lock před načtením, jeden Clock snapshot; due a live rozlišeny | unit, mock repositories | `batchObtainsLockBeforeLoadingPendingAndUsesClock` |
| D4, chybějící screening | No-op bez načítání pending a publikace | unit | `missingScreeningIsNoOp` |
| D4–D6, persistence/collaboration | Dvojí worker uloží jeden EXPIRED, uvolní celý smíšený hold, jedna after-commit notifikace | component/integration, Spring + H2 | `ReservationIntegrationTests.c05RepeatedWorkerCommitsOneExpirationAndReleasesWholeHold` |
| D5–D6, rollback kontrakt | Rollback obnoví PENDING_APPROVAL/expiredAt null a potlačí listener; další commit funguje | component/integration, Spring + H2 | `c05RollbackRestoresPendingAndSuppressesExpirationNotification` |
| BR-08 v API + SQL | 59/60/61 s; pozdní approval 409 a EXPIRED | component/integration, existující regrese | `approvalDeadlineBoundaryAndReadOnlyAvailability` |
| Uvolnění a stale rozhodnutí | EXPIRED release, nová rezervace není změněna starým rozhodnutím | component/integration, existující regrese | `terminalReleaseIsAtomicAndStaleDecisionsCannotAffectNewAllocation` |
| Capped deadline a cancel | Start omezuje deadline; due cancel nepřepíše EXPIRED | component/integration, existující regrese | `pendingDeadlineIsCappedAtScreeningAndDueCancelCannotOverwriteExpiration` |
| Konkurenční lifecycle | Approval/cancel a approve/reject mají platné výsledky | component/integration, existující regrese | `approvalCancelRaceNeverResurrects`, `approveRejectRaceHasOneDecisionAndOneNotification` |

**Vysvětlení volby úrovně 1:** `expiresExactlyAtDeadlineWithNanosecondPrecision` je unit test. Ověřuje čisté rozhodnutí D3 bez DB, frameworku a reálného čekání. Dokládá návrhové pravidlo now >= deadline; použití Instant umožňuje přesný hraniční případ, který by wall-clock test byl nespolehlivý.

**Vysvětlení volby úrovně 2:** `c05RollbackRestoresPendingAndSuppressesExpirationNotification` je component/integration test. Samotný mock publisher nedokáže ověřit Spring transakční listener ani JPA rollback. Reálná transakce/H2 prokazuje návrhové spojení D5–D6: zrušený zápis stavu nesmí způsobit potvrzenou notifikaci. Test neprokazuje produkční doručování externím providerem.

### Design ↔ implementation mismatch

| DD decision | Implemented? | Evidence | Action |
|---|---|---|---|
| Jedno rozhraní pro časová rozhodnutí | YES | `service/PendingExpirationPolicy.java`, `FinitePendingExpirationPolicy.java`; 8 unit testů | keep |
| Capped finite deadline bez overflow | YES | `deadline`, timeout/cap/huge-timeout testy | update code: původní plus-before-cap nahrazeno duration comparison |
| Jediný pending expiration guard/přechod | YES | `ExpirationService.expireIfDueLocked`; ReservationService jej používá pro recheck | update code: odstraněna duplicita `expireAtIfDue` |
| Lock před načtením a snapshot času | YES | `expireScreening` → `expireDueLocked`; mock ordering test a Spring integrační průchody | keep |
| EXPIRED history = effective deadline | YES | `Reservation.expire`; unit + opožděný worker integration test | keep |
| Event pouze při přechodu a listener po commitu | YES | `publishEvent`, `NotificationService`; repeated worker a rollback integration testy | keep |
| Existující API a baseline v0.2 zůstávají platné | YES | Celá backend regresní sada; baseline soubory beze změn | keep |
| C03 databázové active allocations/outbox | NO, mimo C05 jednotku | C03 J-01–05/K zůstávají otevřené | Samostatné pokračování C03; nerozšiřovat tvrzení o dokončení tohoto slice. |

**Resolution:** zachován návrhový záměr C03 pro ownership expirace. Refaktoring přesunul časová pravidla do čisté policy a přechod na jedno místo. Rozhraní ani diagram nebyly přizpůsobeny chybnému chování: overflow scénář má explicitní bezpečný algoritmus a odpovídající test. Dosavadní coarse locking a after-commit listener zůstávají vědomě současnou implementační variantou. Historická otevřená místa v C03 nejsou přeznačena jako dokončená.

### Remaining design risk

- PESSIMISTIC_WRITE na celé projekci serializuje i nesouvisející sedadla; throughput nebyl měřen.
- `expireIfDueLocked` a `expireDueLocked` jsou interní public operace s transakční/lock precondition. Přímý nový volající ji musí dodržet; jednotkový test pořadí nenahrazuje DB concurrency důkaz.
- Logging listener nemá durable delivery/retry. Proces může po commitu skončit před notifikací. Outbox je nehotová C03 práce.
- Produkční policy timeoutu, identita a persistent restart/multi-instance recovery zůstávají mimo tento demo slice. Databázové testy používají H2, nikoli jiný produkční engine.
- Due-aware SQL má paralelní vyjádření časového guardu; při budoucí změně policy je nutné udržet shodu a zachovat regresní testy availability.
- Při budoucím zavedení C03 DB-enforced allocations musí být přepracována nezávislá expiration reconciliation a rollback mapping (J-07); tento refaktoring to sám neřeší.

### Commit/tag

Lokální implementační a dokumentační commit je označen tagem `c05-detailed-design-v1` na větvi `c05-detailed-design`. Tag identifikuje finální stav tohoto C05 slice po úspěšném závěrečném ověření. Hash lze reprodukovat `git rev-parse c05-detailed-design-v1`. Vzdálený main nebyl změněn.

### C05 Definition of Done review

| Požadavek zadání | Konkrétní doklad |
|---|---|
| Jedna architecture responsibility / unit | Architecture C05 / Selected architecture unit; C03 C2 pending expiration |
| Vazba na requirements, scenarios, business rules | REQ-11, BR-06/08/09, V-07/13/14 v úvodu návrhu |
| 3–7 detailed responsibilities | D1–D6, tabulka se zdůvodněním |
| Ownership a state/data | Design elements and ownership; Reservation vlastní persistovaný snapshot |
| Významný interface a kontrakt | PendingExpirationPolicy, pre/postconditions, failures, interní transakční kontrakt |
| Focused structural UML (cca 4–8 prvků) | classDiagram se 7 prvky, interface realization a závislosti |
| Detailed sequence | Konkrétní worker průchod: lock/load/policy/transition/event/commit |
| Významné metody, vstup/výstup, pre/post, side effects, failures | Important methods / operations, 6 operací |
| Algorithm / decision logic | Pseudokód a tabulka časových/stavových větví |
| Nejméně 5 DD-derived testů | 14 nových testů + relevantní původní regrese, mapování výše |
| Vysvětlení úrovně alespoň 2 testů | Čistá ns policy hranice vs reálný transakční rollback/listener |
| Testy spuštěné | Závěrečný záznam Unit/component tests executed níže |
| Implementovaný slice a DD ↔ code porovnání | Nová policy, odstranění duplicity; tabulka implemented/evidence/action |
| DD detaily ponechané záměrně kódu | Samostatná část návrhu |
| Zbývající design risk a řešení nesouladů | Resolution a Remaining design risk |
| Commit/tag | `c05-detailed-design-v1`, lokální větev `c05-detailed-design` |
| Správná odevzdávací místa | Tato sekce a `docs/architecture-and-decisions.md` / C05 Detailed Design |

### Unit/component tests executed

**Skutečný běh 2026-10-19:** JDK Temurin 21.0.12.1, Maven Wrapper 3.9.16, projektové dependencies beze změn. Proveden čistý backend test build (`clean test`). Výsledek: **BUILD SUCCESS**, **74 testů, 0 failures, 0 errors, 0 skipped**, celkový čas Maven 15.025 s.

| Suite | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| ReservationIntegrationTests | 61 | 0 | 0 | 0 |
| ExpirationServiceTests | 4 | 0 | 0 | 0 |
| CinemaReservationApplicationTests | 1 | 0 | 0 | 0 |
| PendingExpirationPolicyTests | 8 | 0 | 0 | 0 |
| **Celkem** | **74** | **0** | **0** | **0** |

14 nových testů = 8 policy unit + 4 service unit + 2 Spring/H2 component/integration; původních 60 provedení zůstává zachováno. Nejde jen o navržené případy: počty byly zkontrolovány proti Surefire XML v `backend/target/surefire-reports/TEST-*.xml`. Log úspěšného běhu a XML jsou přiložené v předávacím balíčku pod `verification/`; build výstupy nejsou commitované do repozitáře.

**Provozní poznámka k ověřovacímu prostředí:** První spuštění narazilo na nedostupný síťový přístup Maven/JDK a poté na zakázané dynamické připojení Mockito agenta. Po lokálním nastavení JDK/cache/důvěryhodné CA byl agent načten při startu test JVM. Finální běh použil offline již naplněnou cache a volbu `-DargLine=-javaagent:<local-maven-cache>/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar`, plus lokální cesty Maven settings/repository/temp. Jde o ověřovací prostředí; POM, wrapper a aplikační konfigurace nebyly změněny. Počáteční chyby inicializace nejsou vykazovány jako úspěšné testy; tabulka obsahuje pouze následný kompletní úspěšný běh.

**Běžná reprodukce** na JDK 21 z adresáře `backend`: `bash mvnw clean test` (Windows `mvnw.cmd clean test`). Standardní úplný package build lze spustit `bash mvnw clean package`; není zde vydáván za samostatně dokončené ověření. Pokud prostředí zakazuje dynamický agent attach, předejte uvedený Mockito javaagent z vlastní lokální cache.

**Další skutečně provedené kontroly:**

- `git diff --check`: PASS.
- Přesný source checker z C03 L2: `PASS: 2 controller files; no direct persistence dependencies.` Je to omezená kontrola přímých závislostí, nikoli důkaz celého C03 TO-BE.
- Lokální odkazy a párování fenced bloků v obou C05 sekcích: PASS; syntaxe/obsah UML prověřeny textově, grafický render nebyl samostatně testován.
- Všech 14 nových testů má explicitní vazbu na DD v tabulce: PASS.
- Obě C02 baseline a soubor C03 beze změn: PASS.

Frontend ani veřejné API nebyly změněny; nový browser/UI průchod nebyl součástí tohoto backend slice a není vykazován jako provedený.
