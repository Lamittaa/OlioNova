CREATE TABLE IF NOT EXISTS olive_images (
    id BIGSERIAL PRIMARY KEY,
    batch_id VARCHAR(255) NOT NULL,
    image_path TEXT,
    capture_time TIMESTAMP,
    cultivar VARCHAR(255),
    harvest_date DATE,
    r_mean DOUBLE PRECISION,
    g_mean DOUBLE PRECISION,
    b_mean DOUBLE PRECISION,
    color_indexes JSONB,
    segmentation_success BOOLEAN,
    actual_yield_percent DOUBLE PRECISION,
    yield_recorded_at TIMESTAMP,
    predicted_yield_percent DOUBLE PRECISION,
    prediction_confidence DOUBLE PRECISION,
    model_version VARCHAR(255),
    is_training_data BOOLEAN,
    anomaly_flag BOOLEAN
);

CREATE INDEX IF NOT EXISTS idx_olive_images_batch_id ON olive_images(batch_id);
CREATE INDEX IF NOT EXISTS idx_olive_images_training ON olive_images(is_training_data);
CREATE INDEX IF NOT EXISTS idx_olive_images_capture_time ON olive_images(capture_time);

CREATE TABLE IF NOT EXISTS model_versions (
    id BIGSERIAL PRIMARY KEY,
    version VARCHAR(255) UNIQUE,
    model_type VARCHAR(255),
    training_date TIMESTAMP,
    training_samples INTEGER,
    performance_metrics JSONB,
    model_path TEXT,
    active BOOLEAN
);

CREATE TABLE IF NOT EXISTS prediction_log (
    id BIGSERIAL PRIMARY KEY,
    image_id BIGINT REFERENCES olive_images(id),
    predicted_yield DOUBLE PRECISION,
    confidence DOUBLE PRECISION,
    model_version VARCHAR(255),
    prediction_time TIMESTAMP,
    latency_ms INTEGER
);
