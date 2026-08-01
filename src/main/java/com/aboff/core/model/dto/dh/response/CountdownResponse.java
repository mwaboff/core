package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.enums.CountdownLoop;
import com.aboff.core.model.enums.CountdownType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response payload describing a campaign countdown.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CountdownResponse {

    private Long id;
    private Long campaignId;
    private String name;
    private CountdownType type;
    private CountdownLoop loopBehavior;
    private Integer startingValue;
    private Integer currentValue;
    private String note;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
}
