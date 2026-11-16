# Technický audit Cinema Reservation System

Dátum: 2026-11-16. Rozsah: oprava technického základu; bez nových funkcií a bez úprav `docs/`.

## 1. Executive summary

Po dvoch minimálnych opravách backend aj frontend prešli vykonanou verifikáciou. Backend vytvoril spustiteľný JAR; čisté testovanie aj následné balenie vykonali po 84 testov bez zlyhaní, chýb alebo preskočení. Frontend prešiel čistou inštaláciou z lockfilu, lintom a produkčným buildom vrátane TypeScript kontroly.

Spustenie zabaleného backendu v dev profile a Vite servera bolo úspešné. HTTP smoke test cez frontend proxy overil vytvorenie, načítanie, potvrdenie, VIP schválenie, obsadenie a uvoľnenie miest pri storne. Toto podporuje použiteľnosť lokálneho technického základu, nie tvrdenie o úplnej funkčnosti alebo produkčnej pripravenosti.

## 2. Initial repository state

- Vetva: `fix/technical-audit`.
- Počiatočný commit: `40de27e775f2fe89a2f06e996be92df36346ddff` (`Update OP-01_CREATE.md`).
- `git status`: žiadne zmeny sledovaných súborov; nesledované `design-references/` a `tatus`.
- Tieto existujúce položky nekolidovali s opravami a zostali nedotknuté.
- Štruktúra: `backend/` (Maven/Spring Boot), `frontend/` (React/TypeScript/Vite), `docs/` (historické míľniky), koreňové prehľadové dokumenty.
- Recentné commity prevažne upravujú dokumentáciu. Relevantné zmeny kódu: `a43eb1b` odstránil DTO, `5d95413` zaviedol správu kina, `a061558` a `f5e38d2` obsahujú existujúce testy a politiku expirácie. Audit tieto úlohy neimplementoval.
- Inšpekcia pred úpravami: `git branch --show-current`, `git rev-parse HEAD`, `git status`, `git log -8 --oneline`, `git log -8 --oneline -- backend frontend`, zoznam súborov a konfigurácií. Nenašiel sa aplikovateľný `AGENTS.md` v repozitári ani bezprostrednom rodičovi.

### Build prostredie a príkazy

Backend: `backend/pom.xml` požaduje Java release 21, používa Spring Boot 4.1.1; wrapper konfiguruje Maven 3.9.16. Overené na OpenJDK 23.0.1 s kompiláciou `release 21`; samostatný beh na JDK 21 nebol vykonaný. Príkazy z `backend/`: `.\mvnw.cmd clean test`, `.\mvnw.cmd package`.

Frontend: Node v22.13.1, npm 10.9.2. README požaduje Node 22.12+; nainštalovaný Vite 8.3.0 deklaruje `^20.19.0 || >=22.12.0`. Príkazy z `frontend/`: `npm.cmd ci`, `npm.cmd run lint`, `npm.cmd run build`. Build skript je `tsc -b && vite build`. Lint skript používa **Oxlint**, nie ESLint; samostatný ESLint skript nie je nakonfigurovaný. Verzie závislostí ani lockfile neboli zmenené.

Vite beží na `127.0.0.1:5173` a API cesty smeruje na backend `localhost:8080`. Smoke test túto komunikáciu skutočne použil.

## 3. Problems discovered

| Závažnosť | Problém a koreňová príčina | Presná cesta | Stav |
|---|---|---|---|
| CRITICAL | Chýba `CreateReservationResponse`, hoci ho controller a service používajú. Commit `a43eb1bb52392c9fd9b3ee38611efeca1b92c33b` súbor odstránil bez zmeny volajúcich. Zdroj preto nemôže kompilovať. Pôvodný obsah overený pomocou `git show a43eb1b^:backend/src/main/java/com/reservedbytes/cinema_reservation/dto/CreateReservationResponse.java`. | `backend/src/main/java/com/reservedbytes/cinema_reservation/dto/CreateReservationResponse.java` | Opravené obnovením pôvodného recordu. |
| HIGH | Windows wrapper končí na `Cannot index into a null array` ešte pred Mavenom. Bezpodmienečne indexoval `(Get-Item $MAVEN_M2_PATH).Target[0]`, aj keď normálny priečinok nemal Target. | `backend/mvnw.cmd` | Opravené kontrolou existencie hodnoty pred indexovaním; následný clean test aj package prešli. |
| LOW | PowerShell blokuje `npm.ps1` cez ExecutionPolicy. Ide o lokálne prostredie, nie chybu aplikácie. | Mimo repozitára: `C:\Program Files\nodejs\npm.ps1` | Použité `npm.cmd`; politika systému nezmenená. |
| LOW | Sandbox pri prvom behu po oprave wrappera neumožnil vytvoriť Maven repository `C:\.m2\repository`. | Mimo repozitára: `C:\.m2\repository` | Maven verifikácia úspešne zopakovaná mimo sandboxu s povolením. Konfigurácia projektu nezmenená. |

Ďalšie chyby blokujúce kompiláciu, vykonané testy, dev startup alebo overenú API komunikáciu neboli potvrdené. Pôvodný chybný backend sa nedostal ku kompilátoru pre skorší pád wrappera; chýbajúci typ bol potvrdený zdrojovým kódom a Git históriou, nie zaznamenaným pôvodným výstupom javac.

## 4. Changes performed

| Súbor | Zmena a dôvod |
|---|---|
| `backend/src/main/java/com/reservedbytes/cinema_reservation/dto/CreateReservationResponse.java` | Obnovený `public record CreateReservationResponse(Long id) {}` v pôvodnom package. Zachováva HTTP 201 a JSON s jediným poľom `id`, očakávaný službou, controllerom, frontendovým `createReservation` aj existujúcim integračným testom. |
| `backend/mvnw.cmd` | Target uložený do `$mavenM2Target`; pred prístupom na `[0]` sa kontroluje neprázdna hodnota. Zachovaná pôvodná logika pre bežný priečinok aj neprázdny cieľ odkazu. Oprava odstraňuje reprodukovaný pád na Windows. |
| `TECHNICAL_AUDIT_REPORT.md` | Nový požadovaný auditný report. |

Testy sa nemenili: existujúci `createsAndCommitsDraftWithAllSelectedSeats` už kontroluje HTTP 201, presne jedno pole odpovede, kladné `id` a uložený DRAFT so všetkými miestami. Žiadny nový test nebol potrebný na pokrytie obnoveného kontraktu. Priechod wrappera overili reálne Maven behy; samostatný test vetvy so symbolickým odkazom sa nevykonal.

## 5. Verification results

Príkazy sú uvedené relatívne k pracovným adresárom. PASS znamená skutočne vykonanú kontrolu, nie odhad.

| Pracovný adresár / presný príkaz | Výsledok | Počet testov | Poznámka |
|---|---|---:|---|
| koreň: `java -version` | PASS | — | OpenJDK 23.0.1. |
| koreň: `node --version` | PASS | — | v22.13.1. |
| koreň: `npm --version` | FAIL | — | ExecutionPolicy blokuje npm.ps1. |
| frontend: `npm.cmd --version` | PASS | — | 10.9.2. |
| backend: `.\mvnw.cmd clean test` pred opravou | FAIL | 0 | Wrapper: null Target array, Maven sa nespustil. |
| backend: `.\mvnw.cmd clean test` po oprave, sandbox | FAIL | 0 | Lokálny Maven repository nie je dostupný. |
| backend: `.\mvnw.cmd clean test` po oprave, mimo sandboxu | PASS | 84 | 0 failures, 0 errors, 0 skipped; BUILD SUCCESS. |
| backend: `.\mvnw.cmd package` mimo sandboxu | PASS | 84 | Rovnaká sada znova prešla; spustiteľný Boot JAR vytvorený. |
| frontend: `npm.cmd run lint` pred inštaláciou | PASS | — | Existujúce node_modules; Oxlint, bez hlásených problémov. |
| frontend: `npm.cmd run build` pred inštaláciou | PASS | — | TypeScript a Vite prešli. |
| frontend: `npm.cmd ci` | PASS | — | 27 balíkov inštalovaných z lockfilu. |
| frontend: `npm.cmd run lint` po inštalácii | PASS | — | Oxlint bez hlásených problémov. |
| frontend: `npm.cmd run build` po inštalácii | PASS | — | 30 modulov; JS 268.89 kB, gzip 81.42 kB. |
| backend: `java -jar target/cinema-reservation-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev` | PASS | — | Tomcat 8080, in-memory H2, fixtures úspešne načítané. |
| frontend: `node node_modules/vite/bin/vite.js` | PASS | — | Vite server na 127.0.0.1:5173. |
| koreň: PowerShell HTTP smoke skript nižšie | PASS | 2 scenáre | STANDARD aj VIP lifecycle cez proxy; HTML/API/OpenAPI dostupné. |
| koreň: `git diff --check` | PASS | — | Žiadne whitespace chyby; iba upozornenie LF/CRLF pre mvnw.cmd. |
| Samostatný ESLint / frontend unit alebo browser E2E test runner | NOT RUN | — | Projekt nemá príslušné npm skripty; vykonaný existujúci lint a HTTP smoke test. |
| Startup s existujúcou súborovou H2 databázou; samostatný beh na JDK 21 | NOT RUN | — | Nezasahovalo sa do existujúcich dát; overený dev profil a release 21 na JDK 23. |

### Backendové sady (v každom úspešnom Maven behu)

| Sada | Testy | Zlyhania / chyby / preskočené |
|---|---:|---|
| CinemaManagementApiTests | 1 | 0 / 0 / 0 |
| CinemaManagementTests | 5 | 0 / 0 / 0 |
| CinemaReservationApplicationTests | 1 | 0 / 0 / 0 |
| ExpirationServiceTests | 4 | 0 / 0 / 0 |
| PendingExpirationPolicyTests | 8 | 0 / 0 / 0 |
| ReservationIntegrationTests | 65 | 0 / 0 / 0 |
| **Spolu** | **84** | **0 / 0 / 0** |

Sady pokrývajú create kontrakt, validáciu, dostupnosť, potvrdenie, storno, VIP approve/reject, hranice expirácie, rollback a notifikácie, konflikty a súbehy, správu filmov/sál/miest/premietaní a obmedzenia pri existujúcej histórii. Výsledky sú v ignorovanom `backend/target/surefire-reports/`.

Nefatálne upozornenia: zastarané API v `ReservationIntegrationTests`, Mockito inline mock maker a dynamické pripojenie Byte Buddy agenta na JDK 23, JVM class sharing a štandardné upozornenia SpringDoc o zapnutom Swagger/OpenAPI. Nie sú príčinou zlyhania aktuálneho buildu. Závislosti sa kvôli nim nemenili.

### Vykonaný PowerShell HTTP smoke skript

Spustený z koreňa po štarte oboch serverov. Mutácie používajú výhradne čerstvú dev in-memory databázu. Príkazy nižšie sú vykonaný skript; dva servery boli následne zastavené pomocou `Stop-Process -Id 29164,36476`.

```powershell
$ErrorActionPreference = 'Stop'
$auditBase = 'http://127.0.0.1:5173'
$auditHeaders = @{ Accept = 'application/json'; 'X-User-Id' = '1' }
$auditHome = Invoke-WebRequest -UseBasicParsing "$auditBase/" -Headers @{Accept='text/html'}
$auditMovies = Invoke-RestMethod "$auditBase/movies" -Headers $auditHeaders
$auditShows = Invoke-RestMethod "$auditBase/screenings?upcoming=true" -Headers $auditHeaders
$auditShow = $auditShows[0].id
$auditSeats = Invoke-RestMethod "$auditBase/screenings/$auditShow/availability" -Headers $auditHeaders
foreach ($auditType in @('STANDARD','VIP')) {
  $auditSeat = ($auditSeats | Where-Object { $_.seatType -eq $auditType -and $_.availability -eq 'AVAILABLE' } | Select-Object -First 1).seatId
  $auditBody = @{userId=1;screeningId=$auditShow;seatIds=@($auditSeat)} | ConvertTo-Json -Compress
  $auditCreated = Invoke-WebRequest -UseBasicParsing "$auditBase/reservations" -Method Post -ContentType 'application/json' -Headers $auditHeaders -Body $auditBody
  if ($auditCreated.StatusCode -ne 201) { throw 'Expected 201' }
  $auditDraft = $auditCreated.Content | ConvertFrom-Json
  if (@($auditDraft.PSObject.Properties).Count -ne 1 -or $auditDraft.id -le 0) { throw 'Invalid create contract' }
  $auditId = $auditDraft.id
  $auditRead = Invoke-RestMethod "$auditBase/reservations/$auditId" -Headers $auditHeaders
  if ($auditRead.status -ne 'DRAFT') { throw 'Expected DRAFT' }
  $auditConfirmed = Invoke-RestMethod "$auditBase/reservations/$auditId/confirm" -Method Post -Headers $auditHeaders
  $auditExpected = if ($auditType -eq 'VIP') { 'PENDING_APPROVAL' } else { 'CONFIRMED' }
  if ($auditConfirmed.status -ne $auditExpected) { throw "Expected $auditExpected" }
  $auditOccupied = Invoke-RestMethod "$auditBase/screenings/$auditShow/availability" -Headers $auditHeaders
  if (($auditOccupied | Where-Object seatId -eq $auditSeat).availability -ne 'UNAVAILABLE') { throw 'Allocation missing' }
  if ($auditType -eq 'VIP') {
    $auditApproved = Invoke-RestMethod "$auditBase/reservations/$auditId/approval" -Method Post -ContentType 'application/json' -Headers @{Accept='application/json';'X-User-Id'='3'} -Body '{"decision":"APPROVE"}'
    if ($auditApproved.status -ne 'CONFIRMED') { throw 'Approval failed' }
  }
  $auditCancelled = Invoke-RestMethod "$auditBase/reservations/$auditId/cancel" -Method Post -Headers $auditHeaders
  if ($auditCancelled.status -ne 'CANCELLED') { throw 'Cancellation failed' }
  $auditReleased = Invoke-RestMethod "$auditBase/screenings/$auditShow/availability" -Headers $auditHeaders
  if (($auditReleased | Where-Object seatId -eq $auditSeat).availability -ne 'AVAILABLE') { throw 'Seat not released' }
  Write-Output "$auditType lifecycle PASS (reservation $auditId)"
}
$auditDocs = Invoke-WebRequest -UseBasicParsing 'http://localhost:8080/v3/api-docs'
[pscustomobject]@{Home=$auditHome.StatusCode;Movies=$auditMovies.Count;Screenings=$auditShows.Count;Seats=$auditSeats.Count;OpenApi=$auditDocs.StatusCode}
```

Výstup: STANDARD PASS (reservation 1), VIP PASS (reservation 2), Home 200, Movies 3, Screenings 6, Seats 80, OpenApi 200. Expirácia sa overovala automatizovanými testami s riadeným časom; samostatné čakanie na reálny dev timeout sa nerobilo.

## 6. Remaining issues

- Nezostáva známy nevyriešený blokátor vykonaných buildov, testov ani dev smoke scenárov.
- Vizuálne správanie UI, browser E2E a produkčný deployment neboli overené. Vite proxy je vývojová konfigurácia; produkčný hosting musí zabezpečiť API routing.
- Existujúca súborová H2 databáza a migrácia starších záznamov neboli overené. README už popisuje obmedzenie starých záznamov; audit do dát nezasahoval.
- Samostatná kompatibilita runtime JDK 21 a symbolických odkazov v Maven cache nebola vykonaná.
- Mockito agent/deprecated test API sú budúca údržba, aktuálne bez zlyhaní.
- Existujúca demo identita `X-User-Id`, nechránená administrácia a nepersistentné notifikácie zostávajú deklarovanými limitmi projektu. Neboli implementované autentifikácia, persistent outbox ani allocation tabuľky.
- Žiadna regresia sa v vykonaných kontrolách neprejavila; testy nedokazujú absenciu všetkých možných chýb.

## 7. SWI exercise readiness

| Cvičenie | Technická pripravenosť |
|---|---|
| C05 | Základ pripravený na pokračovanie; existujúce expiration unit/integration testy prešli. Obsah ani splnenie zadania sa týmto auditom neposudzuje. |
| C06 | Zostaviteľný a otestovaný základ je použiteľný na ďalšiu review prácu. Kritériá cvičenia treba posúdiť samostatne. |
| C07 | Technický základ umožňuje pokračovať; špecifické výstupy zadania neboli overované. |
| C08 | Výsledky auditu môžu slúžiť ako technická evidencia; nenahrádzajú požadované výstupy cvičenia. |
| C09 | Možno pokračovať nad týmto základom; integračné a odovzdávacie kritériá treba overiť podľa zadania. |

Žiadne cvičenie sa týmto reportom neoznačuje COMPLETE. Dokumentácia zadaní v `docs/` nebola zmenená.

## 8. Recommended next actions

1. Skontrolovať a podľa vlastného rozhodnutia uložiť tri súbory auditu do Gitu; pôvodné nesledované položky spravovať samostatne.
2. Pred pokračovaním si potvrdiť aktuálne požiadavky a stav C05–C09; úspešný build nie je splnenie zadania.
3. Doplniť manuálny browser priechod rezerváciou a administráciou, ak sa má aplikácia predvádzať.
4. Pri požiadavke na JDK 21 zopakovať testy priamo na tomto runtime; pred použitím starých dát overiť kompatibilitu na ich zálohovanej kópii.
5. V neskoršej údržbe riešiť Mockito agent a deprecated test API. Produkčné identity, deployment a doručovanie notifikácií riešiť ako oddelené zadanie.

## 9. Git summary

Konečný commit zostáva `40de27e775f2fe89a2f06e996be92df36346ddff`. Vetva sa nezmenila. Nebol vykonaný commit, push, pull, merge, rebase, stash, reset, clean ani prepnutie vetvy.

Konečný `git status --short`:

```text
 M backend/mvnw.cmd
?? TECHNICAL_AUDIT_REPORT.md
?? backend/src/main/java/com/reservedbytes/cinema_reservation/dto/CreateReservationResponse.java
?? design-references/
?? tatus
```

`git diff --stat` (nezahŕňa nesledované súbory):

```text
 backend/mvnw.cmd | 5 +++--
 1 file changed, 3 insertions(+), 2 deletions(-)
```

Audit pridal report a obnovený DTO, upravil wrapper, neodstránil žiadny súbor. `design-references/` a `tatus` sú pôvodné používateľské položky. Finálny diff bol skontrolovaný; žiadne nesúvisiace funkcie, zmeny business pravidiel, credentials ani secrets neboli pridané. Build výstupy zostali ignorované (`backend/target/`, `frontend/dist/`, `frontend/node_modules/`); nič nebolo pridané do stagingu.
