package com.ops.tracking_service.service;

import com.ops.tracking_service.client.ProductionBatchClient;
import com.ops.tracking_service.dto.ProductionBatchResponse;
import com.ops.tracking_service.dto.TankResponse;
import com.ops.tracking_service.dto.TrackingResponse;
import com.ops.tracking_service.exception.ResourceNotFoundException;
import com.ops.tracking_service.model.BatchTracking;
import com.ops.tracking_service.model.TankCode;
import com.ops.tracking_service.model.TrackingStatus;
import com.ops.tracking_service.repository.BatchTrackingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class TrackingService {

    private static final int MIN_ESTIMATED_MINUTES = 45;
    private static final int MAX_ESTIMATED_MINUTES = 240;
    private static final int REGISTERED_PROGRESS = 8;
    private static final int DONE_PROGRESS = 100;
    private static final String TRACKING_CODE_PREFIX = "OPS";
    private static final String TRACKING_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int TRACKING_CODE_RANDOM_LENGTH = 8;
    private static final int TRACKING_CODE_ATTEMPTS = 8;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final BatchTrackingRepository trackingRepository;
    private final ProductionBatchClient productionBatchClient;
    private final OrderReadyForPickupEventPublisher orderReadyForPickupEventPublisher;

    public TrackingService(BatchTrackingRepository trackingRepository,
                           ProductionBatchClient productionBatchClient,
                           OrderReadyForPickupEventPublisher orderReadyForPickupEventPublisher) {
        this.trackingRepository = trackingRepository;
        this.productionBatchClient = productionBatchClient;
        this.orderReadyForPickupEventPublisher = orderReadyForPickupEventPublisher;
    }

    public TrackingResponse getByBatchId(String batchId) {
        ProductionBatchResponse batch = productionBatchClient.getBatch(batchId);
        BatchTracking tracking = getOrCreateTracking(batch);
        return toResponse(batch, tracking);
    }

    public TrackingResponse getByOrderId(Long orderId) {
        ProductionBatchResponse batch = productionBatchClient.getBatchByOrderId(orderId);
        BatchTracking tracking = getOrCreateTracking(batch);
        return toResponse(batch, tracking);
    }

    @Transactional(readOnly = true)
    public TrackingResponse getByTrackingCode(String trackingCode) {
        String normalizedCode = normalizeTrackingCode(trackingCode);
        BatchTracking tracking = trackingRepository.findByTrackingCode(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Tracking code not found."));
        ProductionBatchResponse batch = productionBatchClient.getBatch(tracking.getBatchId());
        return toResponse(batch, tracking);
    }

    public TrackingResponse updateStatus(String batchId, TrackingStatus status) {
        ProductionBatchResponse batch = productionBatchClient.getBatch(batchId);
        BatchTracking tracking = getOrCreateTracking(batch);
        LocalDateTime now = LocalDateTime.now();

        tracking.setStatus(status);
        if (status == TrackingStatus.IN_PROGRESS && tracking.getStartedAt() == null) {
            tracking.setStartedAt(now);
        }
        if (status == TrackingStatus.DONE) {
            if (tracking.getStartedAt() == null) {
                tracking.setStartedAt(tracking.getRegisteredAt());
            }
            tracking.setCompletedAt(now);
        }
        if (status != TrackingStatus.DONE) {
            tracking.setCompletedAt(null);
        }

        return toResponse(batch, trackingRepository.save(tracking));
    }

    public TrackingResponse updateTank(String batchId, TankCode tankCode) {
        ProductionBatchResponse batch = productionBatchClient.getBatch(batchId);
        BatchTracking tracking = getOrCreateTracking(batch);
        tracking.setTankCode(tankCode);
        BatchTracking savedTracking = trackingRepository.save(tracking);
        orderReadyForPickupEventPublisher.publishIfReadyForPickup(savedTracking.getBatchId(), tankCode);
        return toResponse(batch, savedTracking);
    }

    public TrackingResponse updateProductionLine(String batchId, String productionLine) {
        ProductionBatchResponse batch = productionBatchClient.getBatch(batchId);
        BatchTracking tracking = getOrCreateTracking(batch);
        tracking.setProductionLine(normalizeProductionLine(productionLine));
        return toResponse(batch, trackingRepository.save(tracking));
    }

    private BatchTracking getOrCreateTracking(ProductionBatchResponse batch) {
        if (batch.getBatchId() == null || batch.getBatchId().isBlank()) {
            throw new ResourceNotFoundException("Production batch is missing a batch id.");
        }

        return trackingRepository.findById(batch.getBatchId())
                .map(this::ensureTrackingCode)
                .orElseGet(() -> {
                    TrackingStatus status = deriveInitialStatus(batch.getStatus());
                    BatchTracking tracking = new BatchTracking(
                            batch.getBatchId(),
                            status,
                            assignTank(batch),
                            estimateTotalMinutes(batch),
                            batch.getCreatedAt() == null ? LocalDateTime.now() : batch.getCreatedAt(),
                            status == TrackingStatus.IN_PROGRESS ? LocalDateTime.now() : null,
                            status == TrackingStatus.DONE ? LocalDateTime.now() : null
                    );
                    tracking.setTrackingCode(generateUniqueTrackingCode());
                    return trackingRepository.save(tracking);
                });
    }

    private TrackingResponse toResponse(ProductionBatchResponse batch, BatchTracking tracking) {
        BatchTracking publicReadyTracking = ensureTrackingCode(tracking);
        syncEstimatedMinutes(batch, tracking);
        int progress = calculateProgress(tracking);
        int remaining = calculateRemainingMinutes(tracking, progress);
        LocalDateTime start = tracking.getStartedAt() == null ? tracking.getRegisteredAt() : tracking.getStartedAt();
        LocalDateTime estimatedDoneAt = start.plusMinutes(tracking.getEstimatedTotalMinutes());

        return new TrackingResponse(
                batch.getBatchId(),
                publicReadyTracking.getTrackingCode(),
                batch.getOrderId(),
                batch.getOrderItemId(),
                batch.getOliveWeightKg(),
                batch.getPredictedOilKg(),
                tracking.getStatus(),
                statusLabel(tracking.getStatus()),
                progress,
                tracking.getEstimatedTotalMinutes(),
                remaining,
                tracking.getTankCode(),
                "Tank " + tracking.getTankCode(),
                tracking.getProductionLine(),
                messageFor(tracking, remaining),
                tanks(tracking.getTankCode()),
                tracking.getRegisteredAt(),
                tracking.getStartedAt(),
                estimatedDoneAt,
                tracking.getCompletedAt(),
                tracking.getUpdatedAt()
        );
    }

    private BatchTracking ensureTrackingCode(BatchTracking tracking) {
        if (tracking.getTrackingCode() != null && !tracking.getTrackingCode().isBlank()) {
            return tracking;
        }

        tracking.setTrackingCode(generateUniqueTrackingCode());
        return trackingRepository.save(tracking);
    }

    private String generateUniqueTrackingCode() {
        for (int attempt = 0; attempt < TRACKING_CODE_ATTEMPTS; attempt++) {
            String code = generateTrackingCode();
            if (!trackingRepository.existsByTrackingCode(code)) {
                return code;
            }
        }

        throw new IllegalStateException("Could not generate a unique tracking code.");
    }

    private String generateTrackingCode() {
        StringBuilder code = new StringBuilder(TRACKING_CODE_PREFIX);
        for (int index = 0; index < TRACKING_CODE_RANDOM_LENGTH; index++) {
            code.append(TRACKING_CODE_ALPHABET.charAt(SECURE_RANDOM.nextInt(TRACKING_CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    private String normalizeTrackingCode(String trackingCode) {
        if (trackingCode == null || trackingCode.isBlank()) {
            throw new ResourceNotFoundException("Tracking code not found.");
        }

        return trackingCode.trim().toUpperCase(Locale.ROOT);
    }

    private TrackingStatus deriveInitialStatus(String productionStatus) {
        String normalized = productionStatus == null ? "" : productionStatus.toUpperCase(Locale.ROOT);
        if (normalized.contains("DONE") || normalized.contains("COMPLETE") || normalized.contains("FINISH")) {
            return TrackingStatus.DONE;
        }
        if (normalized.contains("PROGRESS") || normalized.contains("START") || normalized.contains("PREDICTED")) {
            return TrackingStatus.IN_PROGRESS;
        }
        return TrackingStatus.REGISTERED;
    }

    private TankCode assignTank(ProductionBatchResponse batch) {
        String seed = batch.getBatchId() + ":" + batch.getOrderItemId();
        TankCode[] tanks = TankCode.values();
        return tanks[Math.floorMod(seed.hashCode(), tanks.length)];
    }

    private void syncEstimatedMinutes(ProductionBatchResponse batch, BatchTracking tracking) {
        int estimate = estimateTotalMinutes(batch);
        if (tracking.getEstimatedTotalMinutes() == null || tracking.getEstimatedTotalMinutes() == 90) {
            tracking.setEstimatedTotalMinutes(estimate);
        }
    }

    private int estimateTotalMinutes(ProductionBatchResponse batch) {
        BigDecimal weight = batch.getOliveWeightKg();
        if (weight == null || weight.signum() <= 0) {
            return MIN_ESTIMATED_MINUTES;
        }

        int estimate = BigDecimal.valueOf(40)
                .add(weight.multiply(BigDecimal.valueOf(0.18)))
                .setScale(0, RoundingMode.CEILING)
                .intValue();
        return Math.max(MIN_ESTIMATED_MINUTES, Math.min(estimate, MAX_ESTIMATED_MINUTES));
    }

    private String normalizeProductionLine(String productionLine) {
        if (productionLine == null || productionLine.isBlank()) {
            throw new ResourceNotFoundException("Production line is required.");
        }

        String normalized = productionLine.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("A") && !normalized.equals("B")) {
            throw new ResourceNotFoundException("Production line must be A or B.");
        }
        return normalized;
    }

    private int calculateProgress(BatchTracking tracking) {
        if (tracking.getStatus() == TrackingStatus.DONE) {
            return DONE_PROGRESS;
        }
        if (tracking.getStatus() == TrackingStatus.REGISTERED) {
            return REGISTERED_PROGRESS;
        }

        LocalDateTime start = tracking.getStartedAt() == null ? tracking.getRegisteredAt() : tracking.getStartedAt();
        long elapsed = Math.max(Duration.between(start, LocalDateTime.now()).toMinutes(), 0);
        int linearProgress = REGISTERED_PROGRESS + (int) Math.round((elapsed * 86.0) / tracking.getEstimatedTotalMinutes());
        return Math.max(REGISTERED_PROGRESS, Math.min(linearProgress, 94));
    }

    private int calculateRemainingMinutes(BatchTracking tracking, int progress) {
        if (tracking.getStatus() == TrackingStatus.DONE) {
            return 0;
        }
        if (tracking.getStatus() == TrackingStatus.REGISTERED) {
            return tracking.getEstimatedTotalMinutes();
        }

        int remaining = (int) Math.ceil(tracking.getEstimatedTotalMinutes() * ((100.0 - progress) / 100.0));
        return Math.max(1, remaining);
    }

    private List<TankResponse> tanks(TankCode current) {
        return Arrays.stream(TankCode.values())
                .map(code -> new TankResponse(code, "Tank " + code, code == current))
                .toList();
    }

    private String statusLabel(TrackingStatus status) {
        return switch (status) {
            case REGISTERED -> "Batch registered";
            case IN_PROGRESS -> "In progress";
            case DONE -> "Done";
        };
    }

    private String messageFor(BatchTracking tracking, int remaining) {
        return switch (tracking.getStatus()) {
            case REGISTERED -> "Your batch is registered and waiting to begin.";
            case IN_PROGRESS -> "Your oil is moving through production. About " + remaining + " minutes remaining.";
            case DONE -> "Your oil is ready in " + "Tank " + tracking.getTankCode() + ".";
        };
    }
}
