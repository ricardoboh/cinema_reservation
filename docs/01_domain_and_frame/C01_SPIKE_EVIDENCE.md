# C01 Engineering Spike Evidence — Reproducible Build & Project Skeleton

**Team:** Deadlock  
**Domain:** Cinema Seat Reservation System  
**Spike Focus:** Category C — Reproducible Build / Environment Configuration  
**Baseline Stage:** C01 Engineering Spike

---

## 1. Goal & Spike Question

Before implementing the core domain logic, the team performed a technical engineering spike to resolve initial infrastructure and environment risks.

* **Engineering Question:**  
  *Can a new team member execute a clean checkout of the repository and successfully build, test, and run the application using only the setup instructions documented in the repository `README.md`?*

---

## 2. Execution & Methodology

To conduct a valid engineering spike with peer review:

1. **Initial Setup:**  
   The initial Spring Boot project skeleton was configured alongside the project dependency management (`pom.xml`), Maven Wrapper binaries (`mvnw` / `mvnw.cmd`), and basic configuration files (`application.yaml`).
2. **Documented Instructions:**  
   Clear environment prerequisites (JDK 21+) and build execution commands were written into the repository `README.md`.
3. **Peer Execution & Review (Verification Loop):**  
   A second team member performed a completely fresh, isolated checkout of the repository on a clean development environment and attempted to build, execute unit tests, and launch the Spring Boot application using strictly the documented commands.

---

## 3. Observed Results & Evidence

* **Build Execution:**  
  The project built successfully via the Maven Wrapper without requiring a pre-installed local Maven distribution (`mvnw clean package`).
* **Automated Test Execution:**  
  Initial context loading and application smoke tests passed with zero failures or errors.
* **Application Startup:**  
  The Spring Boot backend initialized successfully, binding to the default environment HTTP port and loading in-memory persistence components.

---

## 4. Architectural Decision & Impact

Based on the verified spike results, the team formally accepted the following baseline environment decisions:

* **Technology Stack:**  
  The core project stack is standardized on **Java 21 LTS**, **Spring Boot 3.x**, and the **Maven Wrapper (`mvnw`)**.
* **Reproducibility Requirement:**  
  All environment setup rules, profile configurations, and reproduction commands must be maintained within the repository `README.md` and `MANUAL-TESTING.md` to guarantee that clean checkouts remain 100% executable across all team members' environments.
* **Evolutionary Impact:**  
  This foundation enabled the reproducible execution of all subsequent test suites (expanding to 74 automated tests in C05) and supported the clean integration of the React/Vite development frontend in C02/C05.