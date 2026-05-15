package com.ops.tracking_service.dto;

import com.ops.tracking_service.model.TankCode;

public class TankResponse {
    private TankCode code;
    private String label;
    private boolean current;

    public TankResponse(TankCode code, String label, boolean current) {
        this.code = code;
        this.label = label;
        this.current = current;
    }

    public TankCode getCode() { return code; }
    public String getLabel() { return label; }
    public boolean isCurrent() { return current; }
}
