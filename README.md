# 🫒 OPS Microservices System — How to Run Everything

This guide explains how to **run all services step-by-step** for the Olive Press Management System  
using **Spring Boot**, **Docker**, **PostgreSQL**, **Eureka**, and **Swagger**.

---

## ⚙️ Step-by-Step Run Instructions

### 🥇 Step 1 — Run PostgreSQL Database (Docker)

**Command:**
```bash
docker run --name postgres_aut1   -e POSTGRES_USER=postgres   -e POSTGRES_PASSWORD=postgres   -e POSTGRES_DB=auth_db   -p 5432:5432   -d postgres:15
```
✅ This starts the database inside Docker on port **5432**.

---

### 🥈 Step 2 — Run Eureka Discovery Service

**Command:**
```bash
cd discovery-service
mvn spring-boot:run
```
✅ This starts the **Eureka server** on port **8761**.

**Check in browser:**  
👉 [http://localhost:8761](http://localhost:8761)

---

### 🥉 Step 3 — Run Auth Service (Spring Boot)

**Command:**
```bash
cd auth-service
mvn spring-boot:run
```
✅ This starts the **Auth API** on port **8080** and automatically registers it in Eureka.

**Check in browser:**  
👉 [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

## 📡 Auth Service API Endpoints

| Method | Endpoint | Description |
|---------|-----------|-------------|
| POST | `/api/auth/login` | Login and get JWT token |
| POST | `/api/auth/logout` | Logout user |
| POST | `/api/auth/refresh` | Refresh access token |
| GET  | `/api/users/profile` | Get current user info |

---

## ✅ Run Order Summary

| Order | Service | Command | Port |
|--------|----------|----------|------|
| 1️⃣ | Database (PostgreSQL Docker) | `docker run ...` | 5432 |
| 2️⃣ | Eureka Discovery Service | `cd discovery-service && mvn spring-boot:run` | 8761 |
| 3️⃣ | Auth Service | `cd auth-service && mvn spring-boot:run` | 8080 |

---

## 🌐 Verify Everything

1️⃣ Open [http://localhost:8761](http://localhost:8761) → Eureka Dashboard should list `auth-service`.  
2️⃣ Open [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) → Swagger UI opens.  
3️⃣ Try login with default credentials (if DataInitializer exists):  
**Username:** `admin` | **Password:** `123456`

---

## ✨ Done!

Your system is now running:
- PostgreSQL (database)  
- Eureka Discovery Service  
- Auth Service (API + Swagger)

Enjoy building your Olive Press Management System 🚀
