# 🏃 Running IssueFlow Locally

This document outlines the steps required to build, test, and run the IssueFlow API on your local machine.

## 📦 1. Prerequisites

Before starting, ensure you have the following installed:
* Java 25 (JDK 25 or higher)
* Docker & Docker Compose (to run the PostgreSQL database)

*(Note: You do not need to install Maven, as the repository includes the Maven Wrapper `./mvnw`)*

## 🐘 2. Start the Database

IssueFlow requires a PostgreSQL database to run in production mode.

1. Open your terminal in the root of the project.
2. Spin up the database using Docker:
   docker compose up -d
3. The database will bind to `localhost:5432` with the credentials configured in `docker-compose.yml`.

## 🏗 3. Build the Application

To compile the application and build the final executable JAR file (skipping tests temporarily to verify the build):

**Windows (PowerShell/CMD):**
.\mvnw clean package -DskipTests

**macOS/Linux:**
./mvnw clean package -DskipTests

## 🚀 4. Run the Application

You can boot the Spring application directly via the Maven plugin:

**Windows:**
.\mvnw spring-boot:run

**macOS/Linux:**
./mvnw spring-boot:run

The application will start on **port 8080**.
You can verify it is running by navigating to the Swagger UI: http://localhost:8080/swagger-ui/index.html

## 🧪 5. Run the Test Suite

IssueFlow contains a comprehensive suite of JUnit 5 and Mockito tests that utilize an in-memory **H2 database** so they run completely isolated from your Docker PostgreSQL instance.

To execute the test suite:

**Windows:**
.\mvnw test

**macOS/Linux:**
./mvnw test

## 🔌 6. Usage & API Verification

You can fully verify and interact with the API using the following standard `curl` commands from a standard Windows CMD or macOS/Linux terminal:

**1. Create an Admin User:**
curl -X POST http://localhost:8080/users -H "Content-Type: application/json" -d "{\"username\":\"ron_admin\",\"email\":\"ron@test.com\",\"fullName\":\"Ron Tzvick\",\"password\":\"my_secret_password\",\"role\":\"ADMIN\"}"

**2. Authenticate and Get Access Token:**
curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d "{\"username\":\"ron_admin\",\"password\":\"my_secret_password\"}"

**3. Create Your First Project:**
*(Replace <YOUR_TOKEN> with the exact accessToken string received from the login step above)*
curl -X POST http://localhost:8080/projects -H "Content-Type: application/json" -H "Authorization: Bearer <YOUR_TOKEN>" -d "{\"name\":\"Project Alpha\",\"description\":\"This is our first project\",\"ownerId\":1}"