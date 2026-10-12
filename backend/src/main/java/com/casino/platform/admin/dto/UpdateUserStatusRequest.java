package com.casino.platform.admin.dto;

import com.casino.platform.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateUserStatusRequest {

    @NotNull(message = "Status is required")
    private UserStatus status;

    public UpdateUserStatusRequest() {}

    public UpdateUserStatusRequest(UserStatus status) {
        this.status = status;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}
