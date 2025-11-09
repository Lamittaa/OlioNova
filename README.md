# 🫒 OPS Microservices System — Full Run Guide with Docker

This README explains how to build and run the **Olive Press System (OPS)**  
using **Spring Boot**, **PostgreSQL (Docker)**, **Eureka Discovery**, and **Swagger UI**.

---

## ⚙️ System Overview

| Service | Description | Port |
|----------|--------------|------|
| 🧭 discovery-service | Eureka Discovery Server | 8761 |
| 🔐 auth-service | Authentication microservice (JWT, roles, tokens) | 8080 |
| 🗄️ PostgreSQL (Docker) | Database container | 5432 |

---

## 🧱 Step 1 — Build the Auth Service Docker Image

Navigate to your project folder:

```bash
cd auth-service
```

Then build the Docker image:

```bash
docker build -t auth-service .
```

✅ **Explanation:**
- `docker build` creates an image based on your Dockerfile.
- `-t auth-service` gives it the name `auth-service`.

---

## 🗄️ Step 2 — Create and Run PostgreSQL Container

Run the database container:

```bash
docker run --name postgres_auth   -e POSTGRES_USER=postgres   -e POSTGRES_PASSWORD=postgres   -e POSTGRES_DB=auth_db   -p 5432:5432   -d postgres:15
```

✅ **Explanation:**
- Creates a new PostgreSQL container named `postgres_auth`.
- Username = `postgres`
- Password = `postgres`
- Database = `auth_db`
- Maps internal port `5432` to your host machine’s port `5432`.

---

## 🧭 Step 3 — Run Eureka Discovery Service

Navigate to the discovery-service folder and start it with Spring Boot:

```bash
cd discovery-service
mvn spring-boot:run
```

✅ **Explanation:**
- Starts the Eureka Server on port `8761`.
- Keep this terminal window open.
- Open in browser:
  ```
  http://localhost:8761
  ```

---

## 🔐 Step 4 — Run Auth Service (via Spring Boot or Docker)

### Option 1: Run with Spring Boot directly

```bash
cd auth-service
mvn spring-boot:run
```

✅ Runs locally on `localhost:8080`.

### Option 2: Run with Docker

If you already built the image (`auth-service`), run it like this:

```bash
docker run --name auth-service   -p 8080:8080   --link postgres_auth:postgres_auth   -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres_auth:5432/auth_db   -e SPRING_DATASOURCE_USERNAME=postgres   -e SPRING_DATASOURCE_PASSWORD=postgres   auth-service
```

✅ **Explanation:**
- Links the container to the PostgreSQL container.
- Passes database connection settings as environment variables.
- Makes your service accessible at [http://localhost:8080](http://localhost:8080).

---

## 🌐 Step 5 — Access Swagger and Eureka

| Service | URL |
|----------|-----|
| Eureka Dashboard | [http://localhost:8761](http://localhost:8761) |
| Auth Swagger UI | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) |
| Database (pgAdmin or CLI) | `localhost:5432` |

---

## 🧠 Common Commands

| Purpose | Command |
|----------|----------|
| List running containers | `docker ps` |
| Stop a container | `docker stop <container_name>` |
| Start a container | `docker start <container_name>` |
| Remove a container | `docker rm <container_name>` |
| View logs | `docker logs -f <container_name>` |

---

## ✅ Startup Order Summary

| Step | Service | Command | Port |
|------|----------|----------|------|
| 1️⃣ | PostgreSQL | `docker run --name postgres_auth ...` | 5432 |
| 2️⃣ | discovery-service | `cd discovery-service && mvn spring-boot:run` | 8761 |
| 3️⃣ | auth-service | `cd auth-service && mvn spring-boot:run` (or `docker run ...`) | 8080 |

---

## ✅ Verification Checklist

1️⃣ Open [http://localhost:8761](http://localhost:8761) → Eureka should show `auth-service`.  
2️⃣ Open [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) → Swagger UI loads.  

---

## ✨ Developer Info

**Author:** Safa G.  
**Project:** OPS (Olive Press System)  
**Version:** 1.0  
**Date:** November 2025  
**Stack:** Java 21 · Spring Boot 3.3.5 · PostgreSQL 15 · Docker · Eureka · Swagger
