# SWI C05–C07 — správa o dokončení

Dátum: 2026-11-16. Projekt: `ricardoboh/cinema_reservation`, Team Deadlock.

## A. Executive summary

Doplnené boli štyri prázdne dokumenty C05 podľa existujúceho kódu: detailný návrh, kontrakty, expiračná politika a sekvenčné diagramy. Návrh obsahuje triedny diagram a väzby na 14 existujúcich testových metód. Historické C05 podklady dostali vysvetlenie nepresného typu udalosti, package testov, rollback mechanizmu a chýbajúceho checker skriptu.

Odchádzajúci C06 review Křečkomatu zachováva F1/F2 aj pôvodnú technickú evidenciu. Doplnený je súhrn, postup reprodukcie, odkaz na zachovaný log 30 PASS / 0 FAIL a konkrétne ACTION REQUIRED položky. Testy Křečkomatu sa teraz nespúšťali.

C07 rozlišuje interné audity od chýbajúceho originálu externého peer review. Historická kópia interného auditu bola označená správne a jej odkazy opravené. Nový C07 dokument obsahuje evidenčné odpovede, navrhnuté reakcie, skutočný stav opráv a checklist externých potvrdení. C08 zostal nezmenený; nesprávna externá atribúcia v ňom je explicitne vysvetlená v C07 a indexe.

Zmenená bola iba dokumentácia. Aplikačné správanie, C01–C04 a C08–C09 deliverables neboli upravené. Nebol vykonaný commit, push, pull, merge, rebase, reset, stash ani zmena vetvy. Počiatočný pracovný strom bol čistý.

## B. Completion matrix

| Cvičenie | Stav | Dokončené a dôvod obmedzenia |
|---|---|---|
| C05 | PARTIAL | Požadované opravy v repozitári sú dokončené a zdrojovo overené: [návrh](docs/04_detailed_design/c05_detailed_design.md), [kontrakty](docs/04_detailed_design/DESIGN_CONTRACTS.md), [politika](docs/04_detailed_design/EXPIRATION_POLICY.md), [sekvencie](docs/04_detailed_design/EXPIRATION_SEQUENCE.md). Celé cvičenie nemožno označiť COMPLETE bez dostupného oficiálneho zadania a potvrdenia jeho odovzdávacích požiadaviek. |
| C06 | PARTIAL | [Review](docs/review/C06_review) je technicky koherentný a log overený; chýbajú potvrdení hodnotitelia, reakcie vlastníkov F1/F2, chýbajúce citované prílohy a dôkaz predania. |
| C07 | PARTIAL | [Proveniencia, reakcie a status](docs/06_reviews_and_status/C07_REVIEW_RESPONSE_AND_STATUS.md) sú pripravené; chýba originálny nezávislý review Cinema Reservation, jeho identita/baseline a potvrdené tímové dispositions/odovzdanie. Oficiálne zadanie chýba. |

PARTIAL sa vzťahuje na celé cvičenia, nie na nedokončené editovanie dokumentov. Žiadne externé potvrdenie ani owner rozhodnutie nebolo vytvorené za iný tím.

## C. Changes performed

Upravené súbory:

1. [docs/04_detailed_design/c05_detailed_design.md](docs/04_detailed_design/c05_detailed_design.md) — rozsah, komponenty, D1–D6, triedny diagram, dátový tok, riziká, traceability.
2. [docs/04_detailed_design/DESIGN_CONTRACTS.md](docs/04_detailed_design/DESIGN_CONTRACTS.md) — pre/postconditions, invarianty, chyby a transakcie.
3. [docs/04_detailed_design/EXPIRATION_POLICY.md](docs/04_detailed_design/EXPIRATION_POLICY.md) — deadline, inkluzívna hranica, overflow, clock a scheduler konfigurácia.
4. [docs/04_detailed_design/EXPIRATION_SEQUENCE.md](docs/04_detailed_design/EXPIRATION_SEQUENCE.md) — scheduled a request reconciliation cesty, commit/rollback.
5. [docs/05_verification_and_quality/DD_DERIVED_TESTS.md](docs/05_verification_and_quality/DD_DERIVED_TESTS.md) — upozornenie na historické výsledky a presné opravy C05 evidence.
6. [docs/05_verification_and_quality/ARCHITECTURE_CHECKER.md](docs/05_verification_and_quality/ARCHITECTURE_CHECKER.md) — označenie historického obsahu a jeho nepresností.
7. [docs/review/C06_review](docs/review/C06_review) — doplnený súhrn, reprodukcia a explicitné externé úlohy; pôvodný assessment zachovaný.
8. [docs/06_reviews_and_status/PEER_REVIEW_C06.md](docs/06_reviews_and_status/PEER_REVIEW_C06.md) — interná proveniencia, historický kontext a relatívne odkazy.
9. [docs/README.md](docs/README.md) — navigácia C05–C07 a odstránenie nepodloženej atribúcie externému tímu.

Vytvorené súbory:

10. [docs/06_reviews_and_status/C07_REVIEW_RESPONSE_AND_STATUS.md](docs/06_reviews_and_status/C07_REVIEW_RESPONSE_AND_STATUS.md).
11. [SWI_C05_C07_COMPLETION_REPORT.md](SWI_C05_C07_COMPLETION_REPORT.md) — táto správa.

Dočasný validačný skript je uložený mimo repozitára v povolenom systémovom Temp adresári; nie je deliverable ani zmena projektu. Pôvodné `C06_tests.txt`, koreňový interný audit a C08 status zostali nezmenené.

## D. Requirements and evidence

Oficiálne C05/C06/C07 zadania sa nenašli medzi sledovanými súbormi ani pri vyhľadávaní relevantných HTML/PDF/DOCX a názvov zadania v pracovnom priestore a nadradenom adresári SWI. Historický review cituje kedysi dodané HTML; samotné citácie nepredstavujú dostupné zadanie. Táto práca mapuje požiadavky aktuálnej používateľskej úlohy a nevymýšľa ďalšie oficiálne kritériá.

Java zdroje v tabuľke sú relatívne k `backend/src/main/java/com/reservedbytes/cinema_reservation/`; testy k `backend/src/test/java/com/reservedbytes/cinema_reservation/`.

| Požiadavka | Dokument | Zdroj / testová evidencia |
|---|---|---|
| C05 zodpovednosti, architektúra, AS-IS/TO-BE | `docs/04_detailed_design/c05_detailed_design.md` | `service/ExpirationService.java`, `ExpirationSweep.java`, `ReservationService.java`; existujúce C03 pohľady len čítané |
| C05 contracts a transakcie | `docs/04_detailed_design/DESIGN_CONTRACTS.md` | `model/Reservation.java`, `repository/ScreeningRepository.java`, `ReservationRepository.java`, `service/NotificationService.java` |
| C05 časová matematika a hranice | `docs/04_detailed_design/EXPIRATION_POLICY.md` | `service/PendingExpirationPolicy.java`, `FinitePendingExpirationPolicy.java`, `CinemaReservationApplication.java`, aplikačné YAML |
| C05 skutočné sekvencie | `docs/04_detailed_design/EXPIRATION_SEQUENCE.md` | Sweep, expiration service, reservation service, `controller/ReservationController.java`, `AvailabilityController.java` |
| C05 traceability | Tabuľka v `c05_detailed_design.md`; `docs/05_verification_and_quality/DD_DERIVED_TESTS.md` | `PendingExpirationPolicyTests.java` (8 metód), `ExpirationServiceTests.java` (4), `ReservationIntegrationTests.java` (2 C05 metódy + súvisiace hranice) |
| C06 projekt, commit, F1/F2 a výsledky | `docs/review/C06_review`; `docs/review/C06_tests.txt` | Křečkomat commit `8050a3990c01b792a94bd01f361fce1409868391`; zachovaný historický log, nie nový externý checkout |
| C07 proveniencia | Úvod `PEER_REVIEW_C06.md`; tabuľka v `C07_REVIEW_RESPONSE_AND_STATUS.md` | Počiatočná úplná zhoda s `PROJECT_MIDTERM_REVIEW.md`; história `2d5ccf9`, `f6e6799`; outgoing review `f3e1d98` |
| C07 reakcie/status/externé dôkazy | `docs/06_reviews_and_status/C07_REVIEW_RESPONSE_AND_STATUS.md` | INT-01–09, DOC-05, PROV-07, EVID-05; rozhodnutia PENDING, dokončené opravy oddelené |
| Rozlíšenie testových období | C07 tabuľka overení a táto správa | C05 historicky 74; Křečkomat historicky 30; technický audit 2026-11-16 hlási 84 po svojej oprave; táto úloha backend testy nespúšťala |

## E. Verification results

- Git branch/SHA/status skontrolované pred úpravami; pracovný strom čistý. Prečítaný index, súvisiaca dokumentácia, zdroje a testy. Prehľadané lokálne zadania a `AGENTS.md`; projektové AGENTS nenájdené.
- Prečítaná relevantná Git história vrátane pôvodu oboch review dokumentov a posledného technického auditu. `git diff --no-index` potvrdil na začiatku identický interný report a súbor nazvaný peer review.
- Zdrojovo overené deadline výpočty, null/invalid vstupy, hranica rovnosti, event payload `ReservationResponse`, účinný čas `expiredAt`, zámky, JPA dirty checking, transakčné anotácie, AFTER_COMMIT logging a request cesty. Overený testový rollback cez `setRollbackOnly()`.
- Lokálny Node validačný skript kontroluje všetkých 11 zmenených/vytvorených dokumentov vrátane extensionless C06 review: neprázdnosť, vyvážené code fences, náhradné Unicode znaky, existenciu 90 relatívnych odkazov a 47 presných referencií na testové metódy. Overil tiež zachovanie pôvodného C06 assessmentu od sekcie Projekt a neprítomnosť zmien mimo rozsahu. Výsledok finálneho behu: PASS, bez chýb.
- Tri Mermaid bloky (class + dva sequence) skontrolované štrukturálne: typ diagramu a vyvážené alt/opt/loop/end; manuálne porovnané s implementáciou. Plný Mermaid parser/rendering a externý Markdown linter nie sú dostupné v lokálnych závislostiach; grafický rendering sa nevykonal. Externé URL sa nesťahovali, historické obrázky sa neoverovali.
- C06 log spočítaný: 30 `[PASS]`, 0 `[FAIL]`; súhrn sedí. Log sám neobsahuje commit/dátum, tie uvádza zachovaný assessment.
- `git diff --check`: PASS, bez whitespace chýb; Git iba upozorňuje na budúcu konverziu LF/CRLF.
- Rozsah diffu overený: žiadne aplikačné zdroje, konfigurácie, C01–C04 ani C08–C09 súbory sa nemenili. Historický text C06 F1/F2 ostal zachovaný.
- Backend/frontend build, runtime a testy sa nespúšťali, pretože sa mení iba dokumentácia. Chýbajúci `backend/check_architecture.py` sa nespúšťal a jeho starý PASS sa nevydáva za nové overenie.

## F. Outstanding actions

### Práca v repozitári

Po dodaní podkladov zapracovať originálne externé findings, ich skutočné ID, dôkazy a potvrdené reakcie; prepojiť receipt/odovzdanie. Overiť úplnosť proti originálnym zadaniam. Prípadný plný Mermaid rendering a nový testový dôkaz možno doplniť s presným baseline; nie sú náhradou externého review. Úpravy C03 alebo C08 sú mimo tejto úlohy.

### Informácie od tímu Deadlock

Potvrdiť skutočných C06 hodnotiteľov, baseline dohodnutú s Křečkomatom, spôsob/dátum predania, prijatie incoming review a tímové rozhodnutia. Doložiť chýbajúce historicky citované build/inventár prílohy, ak majú byť súčasťou dôkazov. Pri tvrdení o remote tagu alebo školskom odovzdaní priložiť overiteľnú referenciu.

### Dôkazy od iného tímu

Křečkomat: owner disposition F1/F2 s odôvodnením a action/no-change. Incoming hodnotitelia Cinema Reservation: originál review, tím/mená, dátum, hodnotený commit, pôvodné findings a potvrdenie odoslania. Označenie Absolute Cinema v starom indexe samo osebe identitu nedokazuje.

### Chýbajúce oficiálne požiadavky

Poskytnúť pôvodné úplné C05, C06 a C07 zadania a odovzdávacie kritériá. Obsah chýbajúcich zadaní sa nerekonštruoval ako autoritatívny z historických opisov.

## G. Git summary

- Vetva na začiatku aj na konci: `docs/swi-c05-c07`.
- Počiatočný a konečný HEAD: `efef609615aed90aae3851d9a7e205345e862cb2`.
- Počiatočný `git status --short`: prázdny.
- Konečný pracovný strom: 9 upravených sledovaných dokumentov a 2 nové dokumenty, uvedené nižšie. Nič nie je commitnuté ani pushnuté.

```text
 M docs/04_detailed_design/DESIGN_CONTRACTS.md
 M docs/04_detailed_design/EXPIRATION_POLICY.md
 M docs/04_detailed_design/EXPIRATION_SEQUENCE.md
 M docs/04_detailed_design/c05_detailed_design.md
 M docs/05_verification_and_quality/ARCHITECTURE_CHECKER.md
 M docs/05_verification_and_quality/DD_DERIVED_TESTS.md
 M docs/06_reviews_and_status/PEER_REVIEW_C06.md
 M docs/README.md
 M docs/review/C06_review
?? SWI_C05_C07_COMPLETION_REPORT.md
?? docs/06_reviews_and_status/C07_REVIEW_RESPONSE_AND_STATUS.md
```

Diff statistics: `git diff --stat` vykazuje **9 files changed, 344 insertions(+), 32 deletions(-)**. Dva nesledované nové dokumenty pridávajú 184 riadkov (táto správa 115, C07 response/status 69). Spolu vrátane nových súborov: **11 súborov, 528 pridaných a 32 odstránených riadkov**. `git diff --stat` samotný nepočíta nové nesledované súbory.
