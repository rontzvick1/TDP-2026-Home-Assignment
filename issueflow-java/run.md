# 🏃 Running IssueFlow Locally

This document outlines the steps required to build, test, and run the IssueFlow API on your local machine.

## 📦 1. Prerequisites

Before starting, ensure you have the following installed:
* **Java 25** (JDK 25 or higher)
* **Docker** & **Docker Compose** (to run the PostgreSQL database)

*(Note: You do not need to install Maven, as the repository includes the Maven Wrapper `./mvnw`)*

## 🐘 2. Start the Database

IssueFlow requires a PostgreSQL database to run in production mode.

1. Open your terminal in the root of the project.
2. Spin up the database using Docker:
   ```bash
   docker-compose up -d
   ```
3. The database will bind to `localhost:5432` with the credentials configured in `docker-compose.yml`.

## 🏗 3. Build the Application

To compile the application and build the final executable JAR file (skipping tests temporarily to verify the build):

**Windows (PowerShell/CMD):**
```bash
.\mvnw clean package -DskipTests
```

**macOS/Linux:**
```bash
./mvnw clean package -DskipTests
```

## 🚀 4. Run the Application

You can boot the Spring application directly via the Maven plugin:

**Windows:**
```bash
.\mvnw spring-boot:run
```

**macOS/Linux:**
```bash
./mvnw spring-boot:run
```

The application will start on **port 8080**.
You can verify it is running by navigating to the Swagger UI: `http://localhost:8080/swagger-ui.html`

## 🧪 5. Run the Test Suite

IssueFlow contains a comprehensive suite of JUnit 5 and Mockito tests that utilize an in-memory **H2 database** so they run completely isolated from your Docker PostgreSQL instance.

To execute the test suite:

**Windows:**
```bash
.\mvnw test
```

**macOS/Linux:**
```bash
./mvnw test
```
