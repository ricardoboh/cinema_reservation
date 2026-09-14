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

