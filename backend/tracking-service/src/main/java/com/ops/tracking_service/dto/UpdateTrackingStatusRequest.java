package com.ops.tracking_service.dto;

import com.ops.tracking_service.model.TrackingStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateTrackingStatusRequest {
    @NotNull
    private TrackingStatus status;

    public TrackingStatus getStatus() { return status; }
    public void setStatus(TrackingStatus status) { this.status = status; }
}
