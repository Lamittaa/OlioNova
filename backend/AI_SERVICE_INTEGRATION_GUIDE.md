# AI Service Integration Guide

## Purpose

This guide explains how to integrate these two modules into another backend:

- `ai-prediction-service`
- `olive-ai-client`

It answers the practical migration question:

Can we copy the AI service and AI client into a new backend as-is?

Short answer:

- `ai-prediction-service`: mostly yes, but it needs configuration, PostgreSQL, writable storage, and a reachable production-batch dependency for prediction requests
- `olive-ai-client`: yes, with Feign setup and correct service resolution
- no broad rewrite of other business services is required unless they will call the AI client or support the production-batch flow

## Current Repository Status

This repository contains:

- `backend/ai-prediction-service`
- `backend/olive-ai-client`

The AI service code in this checkout already includes a Feign dependency on `production-stages-service` during prediction:

- it loads a batch by `batchId`
- it reads `oliveWeightKg`
- it writes prediction results back to the batch service

That means `/api/v1/predict` is no longer a fully isolated endpoint. Even if the AI service boots standalone, prediction calls still depend on a reachable production-batch API unless you adapt that behavior.

Also important:

- this checkout does not currently contain the `ProductionBatchController` / `ProductionBatch` files referenced in earlier rollout notes
- the checked-in `production-stages-service` security config does not yet accept internal API-key authentication
- no frontend directory is present in this checkout

Use this guide as the source of truth for the code that is actually here.

## What Each Module Does

### `ai-prediction-service`

This is the actual AI microservice. It:

- accepts image uploads
- extracts image features
- predicts olive yield
- stores dataset images and prediction history
- trains and activates models
- stores model artifacts on disk
- stores metadata in PostgreSQL
- fetches batch metadata from `production-stages-service` for prediction
- writes prediction summary data back to `production-stages-service`

### `olive-ai-client`

This is a shared Java client library that other backend services can use to call the AI service. It exposes:

- `predict(...)`
- `updateYield(...)`

It is implemented as a Spring OpenFeign client.

## Important Migration Rule

Do the integration work at the same time you copy the modules.

Do not:

1. copy the folders
2. start the backend
3. fix errors later

That usually causes:

- missing datasource errors
- missing API key errors
- Feign bean creation errors
- service discovery resolution failures
- broken prediction calls because the batch service is unreachable
- gateway routing failures

## Current Integration Model in This Repository

The current backend is designed around:

- Eureka service discovery
- Config Server
- API Gateway
- PostgreSQL
- JWT for business APIs
- API keys for AI endpoints and internal service-to-service calls

The AI service can still boot with Eureka and Config Server disabled by environment variables, but prediction requests will still require a reachable production-batch dependency unless you also adapt the Feign client setup or prediction flow.

## Integration Options

There are two common integration patterns.

### Option A: Keep Microservice Style

Use this when the new backend still has:

- API Gateway
- Eureka
- externalized config
- service-to-service communication by service name

In this option, you can keep most of the current approach.

### Option B: Standalone or Reduced Microservice Integration

Use this when the new backend is:

- a single Spring Boot app
- a reduced microservice setup
- not using Eureka
- not using Config Server

In this option:

- the AI service still needs PostgreSQL and storage
- `olive-ai-client` must resolve the AI service by URL instead of discovery name
- the AI service's own `ProductionBatchClient` must also resolve the production service by URL instead of discovery name, or prediction must be refactored to remove that dependency

## Required Modules to Move

At minimum, move:

- `backend/ai-prediction-service`
- `backend/olive-ai-client`

Depending on the architecture, you may also need:

- gateway route configuration
- service registration configuration
- environment variable setup
- persistent storage volume or directory
- a production-batch API that matches the AI service's Feign contract

## `ai-prediction-service` Integration

### 1. Copy the service

Copy:

- `backend/ai-prediction-service`

### 2. Provide PostgreSQL

The service requires PostgreSQL and Flyway.

It uses:

- JPA
- Flyway migrations
- PostgreSQL driver

The schema is created from:

- `src/main/resources/db/migration/V1__init_ai_prediction_schema.sql`
- `src/main/resources/db/migration/V2__add_prediction_batch_metrics.sql`

Core tables include:

- `olive_images`
- `model_versions`
- `prediction_log`

If the new backend uses a separate database per service, give AI its own database.

If the new backend uses one shared database, keep these tables available and avoid name conflicts.

### 3. Provide environment and config values

The service needs these properties:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `AI_STORAGE_ROOT`
- `SECURITY_API_KEY_INTERNAL`
- `SECURITY_API_KEY_ADMIN`
- `SECURITY_JWT_SECRET`

Current config source:

- `src/main/resources/application.yml`

Minimum example:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/aidb
    username: postgres
    password: postgres

security:
  jwt:
    secret: your-jwt-secret
  api-key:
    internal: your-internal-key
    admin: your-admin-key

ai:
  storage:
    root: ./storage/ai-prediction
```

### 4. Provide writable persistent storage

The service stores:

- uploaded images
- generated model files

`AI_STORAGE_ROOT` must point to a writable path that persists across restarts.

Recommended:

- Docker volume
- mounted disk path
- persistent server directory

### 5. Decide how it will boot

#### If using Eureka and Config Server

Keep the existing behavior.

The service currently expects:

- Config Server import
- Eureka registration

#### If running standalone

Disable both:

- `SPRING_CLOUD_CONFIG_ENABLED=false`
- `EUREKA_CLIENT_ENABLED=false`

This pattern already exists in:

- `docker-compose.ai-prediction.yml`

### 6. Keep security rules in mind

Security behavior is defined in:

- `src/main/java/com/zaytoun/aiprediction/config/SecurityConfig.java`

Rules:

- `/api/v1/health` is public
- prediction and dataset endpoints require authentication
- training endpoints require admin privileges
- `X-API-Key` can authenticate internal and admin callers
- JWT auth is also supported

API-key behavior:

- internal key gives internal access
- admin key gives admin and internal access

Backend-to-backend calls should usually use `X-API-Key`.

### 7. Account for the production-batch dependency

This is the most important difference from older standalone guidance.

The current AI prediction flow uses:

- `src/main/java/com/zaytoun/aiprediction/client/ProductionBatchClient.java`
- `src/main/java/com/zaytoun/aiprediction/config/InternalApiKeyFeignConfig.java`
- `src/main/java/com/zaytoun/aiprediction/service/PredictionService.java`

At prediction time, the AI service:

1. fetches `/api/production/batches/{batchId}`
2. reads `oliveWeightKg`
3. computes `predictedOilKg`
4. updates `/api/production/batches/{batchId}/prediction`

Implications:

- `POST /api/v1/predict` will fail if the production service is down or incompatible
- `SECURITY_API_KEY_INTERNAL` must match on both sides if API-key auth is used
- if the new backend does not use Eureka, this Feign client must be converted to URL-based resolution

Recommended standalone adaptation:

```java
@FeignClient(
    name = "productionBatchClient",
    url = "${production.batch.base-url}",
    configuration = InternalApiKeyFeignConfig.class
)
```

And configure:

```yaml
production:
  batch:
    base-url: http://localhost:8083
```

### 8. Expose the service

Main endpoint groups:

- `POST /api/v1/predict`
- `POST /api/v1/dataset/images`
- `PUT /api/v1/dataset/{batchId}/yield`
- `POST /api/v1/train`
- `GET /api/v1/train/{jobId}`
- `GET /api/v1/health`

## `olive-ai-client` Integration

### 1. Add the client as a dependency

Copy:

- `backend/olive-ai-client`

Recommended integration method:

- publish or install it as a Maven module or internal dependency

Less ideal but workable:

- copy its source package into the new backend codebase

### 2. Enable Feign clients

The AI client is a Feign client.

Current client definition:

- `src/main/java/com/zaytoun/aiclient/OliveAiPredictionClient.java`

The consumer service must:

- include OpenFeign dependency
- enable Feign clients
- scan the client package

Typical app annotation:

```java
@SpringBootApplication
@EnableFeignClients(basePackages = "com.zaytoun.aiclient")
public class YourApplication {
}
```

### 3. Resolve the AI service correctly

The current client uses:

```java
@FeignClient(name = "AI-PREDICTION-SERVICE")
```

That means the consumer expects service discovery or load-balanced resolution by that service name.

#### If using Eureka

Keep it as-is, and make sure:

- the AI service registers successfully
- the resolved service ID matches `AI-PREDICTION-SERVICE`

#### If not using Eureka

Adapt the client to use a URL:

```java
@FeignClient(name = "aiPredictionClient", url = "${ai.prediction.base-url}")
```

Then configure:

```yaml
ai:
  prediction:
    base-url: http://localhost:8082
```

Without this change, Feign will fail to resolve the service.

### 4. Pass API keys from backend code

The client methods require:

- `X-API-Key`

Do not hardcode the key in frontend code.

Do:

- keep the key in backend config or environment variables
- inject it into the calling service
- pass it to the Feign client on each call

Example pattern:

```java
@Service
public class MyService {
    private final OliveAiPredictionClient aiClient;

    @Value("${ai.prediction.internal-key}")
    private String aiApiKey;

    public MyService(OliveAiPredictionClient aiClient) {
        this.aiClient = aiClient;
    }

    public void callAi(MultipartFile file) {
        aiClient.predict(aiApiKey, file, "batch-001", null, null);
    }
}
```

## Gateway Integration

If the frontend or another client will reach the AI service through the API Gateway, add the route during migration.

The checked-in gateway config is:

- `backend/config-server/src/main/resources/config/api-gateway.yml`

At the moment, that file does not include an AI route. Add one explicitly.

### If using Gateway + Eureka

Use:

```yaml
- id: ai-prediction-api
  uri: lb://AI-PREDICTION-SERVICE
  predicates:
    - Path=/api/v1/**
```

### If using Gateway without Eureka

Use a direct URI instead, for example:

```yaml
- id: ai-prediction-api
  uri: http://localhost:8082
  predicates:
    - Path=/api/v1/**
```

## Suggested Migration Order

Use this order to avoid unnecessary failures.

1. Copy `ai-prediction-service`
2. Configure PostgreSQL
3. Configure storage path
4. Configure API keys and JWT secret
5. Decide Eureka and Config Server mode
6. Decide how `ProductionBatchClient` will resolve the production service
7. Start and verify `/api/v1/health`
8. Verify the production-batch API required by prediction is reachable
9. Copy or add `olive-ai-client`
10. Enable Feign in the consumer service
11. Configure AI service resolution for the client
12. Add gateway route if needed
13. Test prediction call
14. Test yield update call
15. Test training endpoint with admin key

## Drop-Time Checklist

### `ai-prediction-service`

- copy the module
- add DB connection values
- make sure PostgreSQL exists
- make sure Flyway migrations run
- create and configure persistent storage path
- set internal and admin API keys
- set JWT secret
- disable Eureka and Config Server if not used
- decide how the production-batch Feign client resolves its target
- expose service port
- verify `/api/v1/health`

### `olive-ai-client`

- copy or add the module as a dependency
- add OpenFeign dependency in the consumer service
- enable Feign clients
- scan `com.zaytoun.aiclient`
- configure AI service resolution
- store AI API key in backend config
- pass `X-API-Key` in calls

### Gateway

- add a route for `/api/v1/**` if clients will use the gateway

## Common Failure Points

### Feign client bean not created

Usually caused by:

- missing `@EnableFeignClients`
- wrong package scan
- missing Feign dependency

### AI client cannot find service

Usually caused by:

- no Eureka registration
- service name mismatch
- no `url` configured for standalone mode

### AI service starts but prediction fails

Usually caused by:

- storage path not writable
- no active model loaded
- image-processing or native dependency issue
- production batch service unavailable
- missing batch ID in the production system
- missing or non-positive `oliveWeightKg` on the batch

### Training fails

Usually caused by:

- not enough labeled dataset rows
- database unavailable
- model storage path not writable

### Unauthorized or forbidden responses

Usually caused by:

- missing `X-API-Key`
- wrong key
- training endpoint called without admin-level auth
- JWT secret mismatch between services
- AI-to-production internal key mismatch

## Recommended Approach for a New Backend

If the updated backend is simpler than the current one, the easiest safe path is:

1. move `ai-prediction-service`
2. run it with standalone config for infrastructure concerns
3. disable Eureka and Config Server
4. connect it to PostgreSQL
5. decide whether prediction should call a real production service or a URL-configured local equivalent
6. point `olive-ai-client` to a direct URL
7. only after that, add gateway routing if needed

This proves the AI service infrastructure first, then proves the batch dependency, then reintroduces platform complexity.

## Known Gaps in This Checkout

If you are trying to reproduce the newer batch-based rollout end to end, there are still gaps in this repository snapshot:

- `production-stages-service` in this checkout does not contain the `ProductionBatchController` and related batch entity files referenced in earlier notes
- `production-stages-service` security currently shows JWT resource-server setup only and does not yet expose the internal API-key flow that the AI service expects for machine-to-machine calls
- the checked-in gateway config does not yet route AI endpoints
- the frontend files mentioned in earlier rollout notes are not present in this checkout

Because of that, the AI service and AI client are movable, but the full batch-based prediction workflow is not yet a pure copy-and-run migration from this exact repository state.

## Final Recommendation

You can move both modules, but they are not zero-config drag-and-drop.

Minimum required adaptation:

- database
- storage path
- API keys
- JWT secret
- Feign enablement
- AI service resolution
- production-batch dependency resolution for `POST /api/v1/predict`
- optional gateway routing

If these are handled during the move, the migration should be straightforward.
