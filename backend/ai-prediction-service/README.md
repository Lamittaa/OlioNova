# AI Prediction Service

This microservice predicts olive oil yield percentage from RGB images of olive fruit samples placed on a blue background.

For migration and cross-service setup guidance, see `../AI_SERVICE_INTEGRATION_GUIDE.md`.

## What it includes
- Spring Boot 3.x microservice in Java
- Image segmentation and RGB feature extraction using JavaCV / OpenCV
- Exact implementation of the requested colorimetric formulas with denominator protection
- Dataset ingestion endpoints
- Manual model training job endpoint
- Active model registry persisted to PostgreSQL + file storage
- Dockerfile and docker-compose snippet
- Swagger UI
- Flyway migration
- Testcontainers integration test

## Endpoints
- `POST /api/v1/predict`
- `POST /api/v1/dataset/images`
- `PUT /api/v1/dataset/{batchId}/yield`
- `POST /api/v1/train`
- `GET /api/v1/train/{jobId}`
- `GET /api/v1/health`

## Security
This service uses API keys by default.
- `X-API-Key: <internal key>` for prediction and dataset endpoints
- `X-API-Key: <admin key>` for training endpoints

## Configuration
The service is meant to load config from the project `config-server`. It can also run standalone with environment variables.

Prediction requests in the current codebase also depend on a reachable production-batch API because the service fetches batch weight and writes prediction results back through Feign.

Important variables:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `AI_STORAGE_ROOT`
- `SECURITY_API_KEY_INTERNAL`
- `SECURITY_API_KEY_ADMIN`

## Cold start
There is no public dataset. Until enough labeled samples are collected and a model is trained, `/api/v1/predict` will return a cold-start error telling the operator to keep collecting data.

## Training data collection flow
1. Store images with `POST /api/v1/dataset/images`
2. When lab-measured actual yield is available, send `PUT /api/v1/dataset/{batchId}/yield`
3. Trigger a training job with `POST /api/v1/train`
4. Use the active model for future predictions

## Notes on model types
This implementation accepts `linear`, `xgboost`, and `bpnn` in the training request. To keep deployment deterministic in a pure JVM service and avoid native ML runtime drift, training is currently normalized to the linear regression path inside the service. The request type is still recorded and can be expanded later.

## Build
```bash
mvn clean package
```

## Run locally
```bash
mvn spring-boot:run
```

## Swagger
- `/swagger-ui.html`
- `/v3/api-docs`
