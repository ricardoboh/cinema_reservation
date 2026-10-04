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
