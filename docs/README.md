```
cinema_reservation/
├── README.md                           # Hlavní rozcestník, quick start, build & run návod
├── MANUAL-TESTING.md                   # Návod pro ruční verifikaci a přímé REST příkazy
├── .gitignore
│
├── docs/                               # Kompletní projektová dokumentace
│   ├── README.md                       # Rozcestník dokumentace s přehledem milníků C01–C08
│   │
│   ├── 01_domain_and_frame/            # C01: Definice domény a rámec projektu
│   │   ├── PROJECT_FRAME.md            # Základní definice (Resource, User, Invarianty, Boundaries)
│   │   ├── DOMAIN_RULES.md             # Společná doménová pravidla (BR-01 až BR-08)
│   │   └── C01_SPIKE_EVIDENCE.md       # Evidence z C01 Engineering Spike (reprodukovatelnost buildu)
│   │



│   ├── 02_requirements_and_spec/       # C02: Specifikace operací a Změnové řízení
│   │   ├── baseline_v0.1/              # Původní minimální specifikace v0.1
│   │   │   └── c02_baseline_v0.1.md    # Úplná specifikace v0.1 (4 základní operace)
│   │   ├── baseline_v0.2/              # Aktualizovaná specifikace v0.2 (Approval Workflow)
│   │   │   ├── c02_baseline_v0.2.md    # Úplná specifikace v0.2 s novými stavy a pravidly
│   │   │   └── c02_change_impact.md    # Dopadová analýza předcházející v0.2
│   │   ├── operations/                 # Samostatné specifikace jednotlivých operací
│   │   │   ├── OP-01_CREATE.md         # Vytvoření DRAFT rezervace
│   │   │   ├── OP-02_AVAILABILITY.md   # Kontrola dostupnosti sedadel
│   │   │   ├── OP-03_CONFIRM.md        # Potvrzení a přechod do CONFIRMED / PENDING_APPROVAL
│   │   │   ├── OP-04_CANCEL.md         # Zrušení rezervace vlastníkem
│   │   │   └── OP-05_APPROVE.md        # Schválení / zamítnutí rezervace schvalovatelem
│   │   └── task_reports/               # Implementační zprávy
│   │       └── c02-task14-report.md    # Zpráva o dokončení a runtime verifikaci v0.2
│   │
│   ├── 03_architecture/                # C03: Návrh architektury a ADR
│   │   ├── c03_architecture_full.md    # Hlavní dokument C03 (Drivery, statické i dynamické pohledy)
│   │   ├── c03_part_a_mapping.md       # Část A: Mapování AS-IS stavu a odhalení TOCTOU
│   │   ├── decisions/                  # Architektonické záznamy rozhodnutí (ADR)
│   │   │   ├── ADR-01_EXCLUSIVITY.md   # Rozhodnutí o databázovém vynucení alokace sedadel
│   │   │   └── ADR-02_OUTBOX.md        # Transactional Outbox Pattern pro notifikace
│   │   └── views/                      # Samostatné diagramy a pohledy
│   │       ├── CONTEXT_DIAGRAM.md      # G1: Systémový kontext
│   │       ├── STATIC_VIEW.md          # G2: Statická komponentová architektura
│   │       ├── STATE_OWNERSHIP.md      # G3: Vlastnictví stavových přechodů
│   │       └── RUNTIME_MAPPING.md      # G4: Mapování na procesy a H2 databázi
│   │
│   ├── 04_detailed_design/             # C05: Detailní návrh vybrané jednotky
│   │   ├── c05_detailed_design.md      # Kompletní podklad C05 pro jednotku Manage Pending Expiration
│   │   ├── EXPIRATION_POLICY.md        # Rozhraní PendingExpirationPolicy a časová matematika
│   │   ├── EXPIRATION_SEQUENCE.md      # Sekvenční diagram pro worker a pesimistické zamykání
│   │   └── DESIGN_CONTRACTS.md         # Formální kontrakty (Pre/Post-conditions, Failures)
│   │
│   ├── 05_verification_and_quality/    # C04 & C05: Evidence testování a verifikace
│   │   ├── TEST_SUITE_SUMMARY.md       # Souhrn testů (74 spuštěných testů, 0 failures)
│   │   ├── DD_DERIVED_TESTS.md         # Mapování 14 nových C05 testů přímo na návrhová rozhodnutí
│   │   ├── ARCHITECTURE_CHECKER.md     # Výstup z automatického L2 checkeru (izolace kontrolerů)
│   │   └── RUNTIME_VERIFICATION.json   # Logy a zápisy ze skutečného spuštění verifikačních skriptů
│   │
│   └── 06_reviews_and_status/          # C06 & C08: Peer review a řízení projektu
│       ├── PEER_REVIEW_C06.md          # Hodnocení od týmu Absolute Cinema a reakce (F1–F3)
│       └── STATUS_REPORT_C08.md        # C08 Executive Status Report (Slide 01–06 podklady)
│
├── backend/                            # Aplikační backend (Java 21 / Spring Boot)
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── pom.xml
│   ├── verify-baseline.ps1             # Skript pro spuštění verifikačních HTTP požadavků
│   ├── check_architecture.py           # Skript pro L2 kontrolu závislostí v Javě
│   └── src/
│       ├── main/
│       │   ├── java/com/reservedbytes/cinema_reservation/
│       │   │   ├── controller/         # REST Kontrolery (ReservationController, AvailabilityController...)
│       │   │   ├── dto/                # Data Transfer Objecty (ApprovalRequest, ReservationResponse...)
│       │   │   ├── model/              # JPA Entity a doménové stavy (Reservation, ReservationStatus...)
│       │   │   ├── repository/         # JPA Repozitáře (ReservationRepository...)
│       │   │   └── service/            # Servisní vrstva, Policy a Expiration woker
│       │   └── resources/
│       │       ├── application.yaml
│       │       └── dev-data.sql        # Seed data (Demo uživatelé, sály, filmová představení)
│       └── test/                       # Rozsáhlá testovací sada (74 testů)
│
└── frontend/                           # Klientská část (React / Vite / TypeScript)

```
    ├── package.json
    ├── vite.config.ts
    ├── verify-ui.mjs                   # CDP skript pro Headless Edge UI verifikaci
    └── src/
        ├── App.tsx                     # Hlavní UI komponenta (Rezervační mapa, Approver ovládání)
        ├── App.css
        └── reservations.ts             # API klient pro komunikaci s backendem
