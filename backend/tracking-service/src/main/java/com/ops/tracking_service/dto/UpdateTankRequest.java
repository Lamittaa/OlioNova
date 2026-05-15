package com.ops.tracking_service.dto;

import com.ops.tracking_service.model.TankCode;
import jakarta.validation.constraints.NotNull;

public class UpdateTankRequest {
    @NotNull
    private TankCode tankCode;

    public TankCode getTankCode() { return tankCode; }
    public void setTankCode(TankCode tankCode) { this.tankCode = tankCode; }
}
