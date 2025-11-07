# 🫒 OPS Microservices System

The **Olive Press Management System** is built using  
**Spring Boot – Maven – Docker – PostgreSQL – Eureka Discovery – Swagger UI**

---

## ⚙️ Project Components

| Component | Description | Port |
|------------|--------------|------|
| 🧭 discovery-service | Eureka Server for service registry | 8761 |
| 🔐 auth-service | Authentication service (login – tokens – users) | 8080 |
| 🗄️ PostgreSQL | Database running in Docker | 5432 |

---

## 🚀 How to Run the Project (Step by Step)

### 1️⃣ Run PostgreSQL (Database)

Open **Docker Desktop** and make sure the container `postgres_aut1` is running ✅  
If not, create and start it using:

```bash
docker run --name postgres_aut1   -e POSTGRES_USER=postgres   -e POSTGRES_PASSWORD=postgres   -e POSTGRES_DB=auth_db   -p 5432:5432   -d postgres:15
```

> The database will be available at port **5432**.

---

### 2️⃣ Run Eureka (Discovery Service)

Navigate to the project folder:

```bash
cd discovery-service
mvn spring-boot:run
```

Then open:

👉 [http://localhost:8761](http://localhost:8761)

You should see the **Eureka Dashboard** (means the server is running).

---

### 3️⃣ Run Auth Service

Go to:

```bash
cd auth-service
mvn spring-boot:run
```

After it starts, open:

👉 [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

You will see **Swagger UI** with all available APIs.

---

## 📡 Main Auth Service APIs

| Method | Endpoint | Description |
|---------|-----------|-------------|
| POST | `/api/auth/login` | Login and return JWT Token |
| POST | `/api/auth/logout` | Logout user |
| POST | `/api/auth/refresh` | Refresh access token |
| GET  | `/api/users/profile` | Get current user profile |
| GET  | `/api/roles` | List all roles |
| POST | `/api/roles` | Create new role |

---

## 🧩 Startup Order

| Step | Service | Port | Purpose |
|------|----------|------|----------|
| 1️⃣ | PostgreSQL (Docker) | 5432 | Database |
| 2️⃣ | discovery-service | 8761 | Eureka registry |
| 3️⃣ | auth-service | 8080 | Swagger + APIs |

---

## 📊 Important Links

| Service | URL |
|----------|-----|
| Eureka Dashboard | [http://localhost:8761](http://localhost:8761) |
| Auth Swagger UI | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) |
| Database | `jdbc:postgresql://localhost:5432/auth_db` |

---

## 🧠 Notes

- Make sure **Docker Desktop** is running before any service.  
- If you get a database connection error, update your `application.yml` in `auth-service/src/main/resources`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_db
    username: postgres
    password: postgres
```

- After starting `auth-service`, it will automatically register inside the **Eureka Dashboard**.

---

## 🧩 (Optional) Add Default Data (DataInitializer)

To insert default admin user and roles automatically, create the file:
```
com.project.auth_service.initializer.DataInitializer.java
```

```java
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final AuthorityRepository authorityRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (roleRepository.count() == 0) {
            Authority read = authorityRepository.save(new Authority("READ_PRIVILEGE"));
            Authority write = authorityRepository.save(new Authority("WRITE_PRIVILEGE"));

            Role adminRole = new Role("ADMIN");
            adminRole.setAuthorities(List.of(read, write));
            roleRepository.save(adminRole);

            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setRole(adminRole);
            userRepository.save(admin);

            System.out.println("✅ Default admin created: admin / 123456");
        }
    }
}
```

---

## ✅ Verification Checklist

1️⃣ Open [http://localhost:8761](http://localhost:8761) → Ensure `auth-service` appears.  
2️⃣ Open [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) → Test the `login` endpoint.  
3️⃣ Use default credentials → **username:** `admin` | **password:** `123456`.

---

## ✨ Developer Info

**Name:** Safa G.  
**Project:** OPS (Olive Press System)  
**Version:** 1.0  
**Date:** November 2025  
**Tech Stack:** Java 21 · Spring Boot 3.3.5 · PostgreSQL 15 · Docker · Eureka · Swagger
