package com.aboff.core.model.dto.dh.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for starting a run of an encounter.
 * <p>
 * {@code campaignId} is entirely optional -- omitting it starts a standalone, campaign-free
 * run. This is the whole "campaign-free" story: nothing else about starting or playing a run
 * requires a campaign at all.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEncounterRunRequest {

    /** Optional campaign to tag this run to. Null starts a standalone run. */
    private Long campaignId;
}
