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
