# Microservice Code Generation Platform

This platform allows you to generate ready-to-use microservices based on plain English prompts.

## Prerequisites

-   **Java 21** or higher
-   **Node.js 20** or higher
-   **Maven** (or use the included `./mvnw`)
-   **Docker** (optional, for running generated services)

## High-Level Architecture

-   **Frontend**: Next.js application providing the AI Prompt Engine and Code Viewer.
-   **Backend**: Spring Boot "Orchestrator" that parses prompts and generates code using an internal engine.

---

## How to Run

### 1. Start the Backend (Orchestrator)

The backend must be running for the frontend to communicate with.

```bash
cd backend
./mvnw spring-boot:run
```

-   **API Endpoint**: `http://localhost:8081`
-   **Swagger UI**: `http://localhost:8081/swagger-ui.html` (if enabled)

### 2. Start the Frontend

In a new terminal:

```bash
cd frontend
npm install
npm run dev
```

-   **Web UI**: `http://localhost:3000`

---

## How to Use

1.  Open the Web UI at [http://localhost:3000](http://localhost:3000).
2.  In the **AI PROMPT ENGINE**, enter a prompt such as:
    -   `create an auth microservice with JWT and mybatis`
    -   `create a product service with postgresql and jpa`
3.  Click **Generate Codebase**.
4.  Browse the generated files in the **PROJECT FILES** explorer.
5.  Click **Download ZIP** to get the full source code.

---

## Running the Generated Microservice

Once you download the ZIP and extract it:

1.  Build the project:
    ```bash
    mvn clean package
    ```
2.  Run with Docker:
    ```bash
    docker-compose up --build
    ```
    *Note: Ensure Docker Desktop is running if you are on Windows.*
