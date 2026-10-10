package com.akshay.ats.application.dto;

import com.akshay.ats.application.Stage;
import com.akshay.ats.application.StageHistory;

import java.time.LocalDateTime;

public record StageHistoryResponse(
        Long id,
        Stage fromStage,
        Stage toStage,
        String changedByName,
        String note,
        LocalDateTime changedAt
) {
    public static StageHistoryResponse from(StageHistory h) {
        return new StageHistoryResponse(
                h.getId(), h.getFromStage(), h.getToStage(),
                h.getChangedBy().getFullName(), h.getNote(), h.getChangedAt()
        );
    }
}
