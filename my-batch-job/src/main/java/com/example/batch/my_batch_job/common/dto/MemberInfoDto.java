package com.example.batch.my_batch_job.common.dto;

import jakarta.validation.constraints.Max;

public class MemberInfoDto {
    private String id; // (1)

    private String type; // (2)

    private String status; // (3)

    @Max(1000000) // (1)
    private int point; // (4)

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPoint() {
        return point;
    }

    public void setPoint(int point) {
        this.point = point;
    }
}
